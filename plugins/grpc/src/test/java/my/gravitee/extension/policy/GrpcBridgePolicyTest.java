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

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import my.gravitee.extension.policy.model.RouteConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class GrpcBridgePolicyTest {

    private GrpcBridgePolicyConfiguration configuration;
    private GrpcBridgePolicy policy;

    @BeforeEach
    public void setUp() {
        configuration = new GrpcBridgePolicyConfiguration();
        configuration.setMappingsJson("{\n" +
                "  \"POST /api/auth/login\": \"/auth.AuthService/Login\"\n" +
                "}");
        policy = new GrpcBridgePolicy(configuration);
    }

    @Test
    public void shouldLoadInlineMappings() {
        assertThat(policy.getMappings()).containsKey("POST /api/auth/login");
        RouteConfig routeConfig = policy.getMappings().get("POST /api/auth/login");
        assertThat(routeConfig.getGrpcMethod()).isEqualTo("/auth.AuthService/Login");
    }

    @Test
    public void shouldFailWhenRouteNotFound() {
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        PolicyChain policyChain = mock(PolicyChain.class);

        when(request.method()).thenReturn(HttpMethod.POST);
        when(request.path()).thenReturn("/unknown/route");

        ReadWriteStream<Buffer> stream = policy.onRequestContent(request, response, policyChain);
        stream.end();

        verify(policyChain, times(1)).failWith(any(PolicyResult.class));
    }
}
