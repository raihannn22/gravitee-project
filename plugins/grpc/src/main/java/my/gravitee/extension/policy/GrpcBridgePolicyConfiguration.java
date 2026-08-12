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

import io.gravitee.policy.api.PolicyConfiguration;

@SuppressWarnings("unused")
public class GrpcBridgePolicyConfiguration implements PolicyConfiguration {

    private String mappingFilePath = "/opt/gravitee/plugins/mapping.json";
    private String mappingsJson;
    private String grpcBackendAddr = "http://localhost:50051";
    private int timeoutMs = 10000;

    public String getMappingFilePath() {
        return mappingFilePath;
    }

    public void setMappingFilePath(String mappingFilePath) {
        this.mappingFilePath = mappingFilePath;
    }

    public String getMappingsJson() {
        return mappingsJson;
    }

    public void setMappingsJson(String mappingsJson) {
        this.mappingsJson = mappingsJson;
    }

    public String getGrpcBackendAddr() {
        return grpcBackendAddr;
    }

    public void setGrpcBackendAddr(String grpcBackendAddr) {
        this.grpcBackendAddr = grpcBackendAddr;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
