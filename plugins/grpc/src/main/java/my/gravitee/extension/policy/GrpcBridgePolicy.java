/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package my.gravitee.extension.policy;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.api.stream.BufferedReadWriteStream;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.SimpleReadWriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import io.gravitee.policy.api.annotations.OnRequestContent;

import my.gravitee.extension.policy.model.FieldRoute;
import my.gravitee.extension.policy.model.RouteConfig;
import my.gravitee.extension.policy.util.GrpcFramingUtils;
import my.gravitee.extension.policy.util.ProtobufEncoder;
import my.gravitee.extension.policy.util.RouteMatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GrpcBridgePolicy {

    private static final Logger log = LoggerFactory.getLogger(GrpcBridgePolicy.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final GrpcBridgePolicyConfiguration configuration;
    private final Map<String, RouteConfig> mappings = new HashMap<>();

    public GrpcBridgePolicy(GrpcBridgePolicyConfiguration configuration) {
        this.configuration = configuration;
        loadMappings();
    }

    private void loadMappings() {
        try {
            if (configuration.getMappingsJson() != null && !configuration.getMappingsJson().trim().isEmpty()) {
                parseAndSetMappings(configuration.getMappingsJson());
                log.info("[gRPC Bridge] Loaded route mappings from inline configuration JSON");
                return;
            }

            String path = configuration.getMappingFilePath();
            String envPath = System.getenv("GRPC_MAPPING_FILE_PATH");
            if (envPath != null && !envPath.trim().isEmpty()) {
                path = envPath;
            }

            File file = new File(path);
            if (file.exists()) {
                Map<String, RouteConfig> rawMappings = objectMapper.readValue(
                        file,
                        objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, RouteConfig.class)
                );
                setNormalizedMappings(rawMappings);
                log.info("[gRPC Bridge] Loaded {} route mappings from {}", mappings.size(), path);
            } else {
                log.warn("[gRPC Bridge] Mapping file not found at path: {}. Using empty mappings.", path);
            }
        } catch (Exception e) {
            log.error("[gRPC Bridge] Error loading route mappings: {}", e.getMessage(), e);
        }
    }

    private void parseAndSetMappings(String jsonString) throws Exception {
        Map<String, RouteConfig> rawMappings = objectMapper.readValue(
                jsonString,
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, RouteConfig.class)
        );
        setNormalizedMappings(rawMappings);
    }

    private void setNormalizedMappings(Map<String, RouteConfig> rawMappings) {
        mappings.clear();
        if (rawMappings != null) {
            for (Map.Entry<String, RouteConfig> entry : rawMappings.entrySet()) {
                String key = entry.getKey();
                if (key.contains("?")) {
                    key = key.substring(0, key.indexOf("?"));
                }
                mappings.put(key, entry.getValue());
            }
        }
    }

    public Map<String, RouteConfig> getMappings() {
        return mappings;
    }

    @OnRequestContent
    public ReadWriteStream<Buffer> onRequestContent(Request request, Response response, PolicyChain policyChain) {
        return new BufferedReadWriteStream() {
            private final Buffer buffer = Buffer.buffer();

            @Override
            public SimpleReadWriteStream<Buffer> write(Buffer content) {
                if (content != null) {
                    buffer.appendBuffer(content);
                }
                return this;
            }

            @Override
            public void end() {
                try {
                    processTranscode(request, response, policyChain, buffer);
                } catch (Exception e) {
                    log.error("[gRPC Bridge] Error during transcoding: {}", e.getMessage(), e);
                    policyChain.failWith(PolicyResult.failure(HttpStatusCode.INTERNAL_SERVER_ERROR_500, "Internal server error: " + e.getMessage()));
                }
            }
        };
    }

    private void processTranscode(Request request, Response response, PolicyChain policyChain, Buffer inputBuffer) {
        String method = request.method().name();
        String path = request.path();

        RouteMatcher.MatchResult matchResult = RouteMatcher.match(mappings, method, path);
        if (matchResult == null) {
            log.warn("[gRPC Bridge] No route mapping found for key: {} {}", method, path);
            policyChain.failWith(PolicyResult.failure(HttpStatusCode.NOT_FOUND_404, "Route not found"));
            return;
        }

        RouteConfig routeConfig = matchResult.getRouteConfig();
        String grpcMethod = routeConfig.getGrpcMethod();

        byte[] bodyBytes;
        if ("GET".equalsIgnoreCase(method)) {
            ByteArrayOutputStream protoBuf = new ByteArrayOutputStream();
            Map<String, String> pathParams = matchResult.getPathParams();

            if (routeConfig.getFields() != null) {
                for (Map.Entry<String, FieldRoute> fieldEntry : routeConfig.getFields().entrySet()) {
                    String fieldName = fieldEntry.getKey();
                    FieldRoute fieldRoute = fieldEntry.getValue();

                    String val = pathParams.get(fieldName);
                    if (val == null && request.parameters() != null) {
                        List<String> qVals = request.parameters().get(fieldName);
                        if (qVals != null && !qVals.isEmpty()) {
                            val = qVals.get(0);
                        }
                    }

                    if (val == null || val.isEmpty()) {
                        continue;
                    }

                    try {
                        switch (fieldRoute.getType()) {
                            case "string":
                                protoBuf.write(ProtobufEncoder.encodeString(fieldRoute.getFieldNumber(), val));
                                break;
                            case "int32":
                            case "int64":
                            case "uint32":
                            case "uint64":
                                long intVal = Long.parseLong(val);
                                protoBuf.write(ProtobufEncoder.encodeVarint(fieldRoute.getFieldNumber(), intVal));
                                break;
                            case "bool":
                                long boolVal = ("true".equalsIgnoreCase(val) || "1".equals(val)) ? 1 : 0;
                                protoBuf.write(ProtobufEncoder.encodeVarint(fieldRoute.getFieldNumber(), boolVal));
                                break;
                        }
                    } catch (Exception e) {
                        log.warn("[gRPC Bridge] Failed to encode field {}: {}", fieldName, e.getMessage());
                    }
                }
            }
            bodyBytes = protoBuf.toByteArray();
        } else {
            bodyBytes = inputBuffer.getBytes();
            if (bodyBytes.length > 0) {
                String strBody = new String(bodyBytes, StandardCharsets.UTF_8).trim();
                try {
                    byte[] decoded = Base64.getDecoder().decode(strBody);
                    if (decoded.length > 0) {
                        bodyBytes = decoded;
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Frame the payload into 5-byte gRPC header
        byte[] grpcFrame = GrpcFramingUtils.framePayload(bodyBytes);

        // Target URL
        String targetBase = configuration.getGrpcBackendAddr();
        String envBackend = System.getenv("GRPC_BACKEND_ADDR");
        if (envBackend != null && !envBackend.trim().isEmpty()) {
            targetBase = envBackend;
        }
        if (!targetBase.contains("://")) {
            targetBase = "http://" + targetBase;
        }

        if (targetBase.endsWith("/")) {
            targetBase = targetBase.substring(0, targetBase.length() - 1);
        }
        String targetUrl = targetBase + (grpcMethod.startsWith("/") ? grpcMethod : "/" + grpcMethod);

        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(Duration.ofMillis(configuration.getTimeoutMs()))
                    .build();

            HttpRequest grpcReq = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .header("Content-Type", "application/grpc")
                    .header("TE", "trailers")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(grpcFrame))
                    .timeout(Duration.ofMillis(configuration.getTimeoutMs()))
                    .build();

            HttpResponse<byte[]> grpcResp = client.send(grpcReq, HttpResponse.BodyHandlers.ofByteArray());

            if (grpcResp.statusCode() != 200) {
                log.warn("[gRPC Bridge] Backend responded with HTTP status: {}", grpcResp.statusCode());
                policyChain.failWith(PolicyResult.failure(HttpStatusCode.BAD_GATEWAY_502, "Backend error"));
                return;
            }

            String grpcStatus = grpcResp.headers().firstValue("Grpc-Status").orElse(null);
            if (grpcStatus != null && !"0".equals(grpcStatus)) {
                String grpcMessage = grpcResp.headers().firstValue("Grpc-Message").orElse("gRPC Error: status " + grpcStatus);
                log.warn("[gRPC Bridge] gRPC returned error status: {} ({})", grpcStatus, grpcMessage);
                policyChain.failWith(PolicyResult.failure(HttpStatusCode.UNAUTHORIZED_401, grpcMessage, "application/x-protobuf"));
                return;
            }

            byte[] respBytes = grpcResp.body();
            byte[] unframedPayload = GrpcFramingUtils.unframePayload(respBytes);

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-protobuf");
            String payloadStr = new String(unframedPayload, StandardCharsets.ISO_8859_1);
            policyChain.failWith(PolicyResult.failure(HttpStatusCode.OK_200, payloadStr, "application/x-protobuf"));

        } catch (Exception e) {
            log.error("[gRPC Bridge] Failed to execute gRPC backend request: {}", e.getMessage(), e);
            policyChain.failWith(PolicyResult.failure(HttpStatusCode.SERVICE_UNAVAILABLE_503, "Service unavailable: " + e.getMessage()));
        }
    }
}
