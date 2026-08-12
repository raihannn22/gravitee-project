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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.BufferedReadWriteStream;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.SimpleReadWriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import io.gravitee.policy.api.annotations.OnRequestContent;
import io.gravitee.policy.api.annotations.OnResponse;
import com.google.protobuf.util.JsonFormat;
import my.gravitee.extension.policy.proto.HelloRequest;

@SuppressWarnings("unused")
public class FooHeaderCheckPolicy {

    /**
     * The associated configuration to this FooHeaderCheck Policy
     */
    private final FooHeaderCheckPolicyConfiguration configuration;

    /**
     * Create a new FooHeaderCheck Policy instance based on its associated configuration
     *
     * @param configuration the associated configuration to the new FooHeaderCheck Policy instance
     */
    public FooHeaderCheckPolicy(FooHeaderCheckPolicyConfiguration configuration) {
        this.configuration = configuration;
    }

    @OnRequestContent
    public ReadWriteStream<Buffer> onRequestContent(Request request, PolicyChain policyChain) {
        return new BufferedReadWriteStream() {
            private final Buffer buffer = Buffer.buffer();

            @Override
            public SimpleReadWriteStream<Buffer> write(Buffer content) {
                buffer.appendBuffer(content);
                return this;
            }

            @Override
            public void end() {
                try {
                    String jsonStr = buffer.toString();
                    System.out.println("JSON: " + jsonStr);

                    // Parse JSON to HelloRequest Protobuf message
                    HelloRequest.Builder builder = HelloRequest.newBuilder();
                    JsonFormat.parser().ignoringUnknownFields().merge(jsonStr, builder);
                    HelloRequest protobufMessage = builder.build();
                    byte[] protobufBytes = protobufMessage.toByteArray();

                    byte[] finalBytes;
                    String contentType;

                    if (configuration.isUseGrpcFraming()) {
                        // Standard gRPC framing: 1-byte compression flag + 4-byte message length
                        int messageLength = protobufBytes.length;
                        finalBytes = new byte[5 + messageLength];
                        finalBytes[0] = 0; // uncompressed
                        finalBytes[1] = (byte) ((messageLength >> 24) & 0xFF);
                        finalBytes[2] = (byte) ((messageLength >> 16) & 0xFF);
                        finalBytes[3] = (byte) ((messageLength >> 8) & 0xFF);
                        finalBytes[4] = (byte) (messageLength & 0xFF);
                        System.arraycopy(protobufBytes, 0, finalBytes, 5, messageLength);
                        contentType = "application/grpc";
                    } else {
                        finalBytes = protobufBytes;
                        contentType = "application/x-protobuf";
                    }

                    Buffer newBuffer = Buffer.buffer(finalBytes);

                    // Update HTTP Request Headers
                    request.headers().set("Content-Type", contentType);
                    request.headers().set("Content-Length", Integer.toString(newBuffer.length()));

                    // Write the transformed buffer downstream
                    super.write(newBuffer);
                    super.end();

                } catch (Exception e) {
                    policyChain.failWith(PolicyResult.failure(
                            HttpStatusCode.BAD_REQUEST_400,
                            "Failed to convert JSON request payload to Protobuf: " + e.getMessage()
                    ));
                }
            }
        };
    }

    @OnResponse
    public void onResponse(Request request, Response response, PolicyChain policyChain) {
        if (isASuccessfulResponse(response)) {
            policyChain.doNext(request, response);
        } else {
            policyChain.failWith(
                PolicyResult.failure(HttpStatusCode.INTERNAL_SERVER_ERROR_500, "Not a successful response :-("));
        }
    }

    private static boolean isASuccessfulResponse(Response response) {
        switch (response.status() / 100) {
            case 1:
            case 2:
            case 3:
                return true;
            default:
                return false;
        }
    }
}

