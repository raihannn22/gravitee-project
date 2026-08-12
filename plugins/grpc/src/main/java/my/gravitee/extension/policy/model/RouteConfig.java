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
package my.gravitee.extension.policy.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@JsonDeserialize(using = RouteConfig.Deserializer.class)
public class RouteConfig {

    @JsonProperty("grpc_method")
    private String grpcMethod;

    @JsonProperty("fields")
    private Map<String, FieldRoute> fields = new HashMap<>();

    public RouteConfig() {}

    public RouteConfig(String grpcMethod, Map<String, FieldRoute> fields) {
        this.grpcMethod = grpcMethod;
        if (fields != null) {
            this.fields = fields;
        }
    }

    public String getGrpcMethod() {
        return grpcMethod;
    }

    public void setGrpcMethod(String grpcMethod) {
        this.grpcMethod = grpcMethod;
    }

    public Map<String, FieldRoute> getFields() {
        return fields;
    }

    public void setFields(Map<String, FieldRoute> fields) {
        this.fields = fields;
    }

    public static class Deserializer extends JsonDeserializer<RouteConfig> {
        @Override
        public RouteConfig deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            RouteConfig config = new RouteConfig();

            if (node.isTextual()) {
                config.setGrpcMethod(node.asText());
                return config;
            }

            if (node.isObject()) {
                if (node.has("grpc_method")) {
                    config.setGrpcMethod(node.get("grpc_method").asText());
                }
                if (node.has("fields") && node.get("fields").isObject()) {
                    JsonNode fieldsNode = node.get("fields");
                    Map<String, FieldRoute> fieldMap = new HashMap<>();
                    Iterator<Map.Entry<String, JsonNode>> fieldsIter = fieldsNode.fields();
                    while (fieldsIter.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fieldsIter.next();
                        FieldRoute fr = p.getCodec().treeToValue(entry.getValue(), FieldRoute.class);
                        fieldMap.put(entry.getKey(), fr);
                    }
                    config.setFields(fieldMap);
                }
            }

            return config;
        }
    }
}
