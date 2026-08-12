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
package my.gravitee.extension.policy.util;

import my.gravitee.extension.policy.model.FieldRoute;
import my.gravitee.extension.policy.model.RouteConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class RouteMatcherTest {

    @Test
    public void shouldMatchExactRoute() {
        Map<String, RouteConfig> mappings = new HashMap<>();
        RouteConfig loginConfig = new RouteConfig("/auth.AuthService/Login", new HashMap<>());
        mappings.put("POST /api/auth/login", loginConfig);

        RouteMatcher.MatchResult result = RouteMatcher.match(mappings, "POST", "/api/auth/login");
        assertThat(result).isNotNull();
        assertThat(result.getRouteConfig().getGrpcMethod()).isEqualTo("/auth.AuthService/Login");
    }

    @Test
    public void shouldMatchPathTemplateRoute() {
        Map<String, RouteConfig> mappings = new HashMap<>();
        Map<String, FieldRoute> fields = new HashMap<>();
        fields.put("name", new FieldRoute(1, "string"));
        RouteConfig helloConfig = new RouteConfig("/HelloService/SayHello", fields);
        mappings.put("GET /api/hello/say-hello/{name}", helloConfig);

        RouteMatcher.MatchResult result = RouteMatcher.match(mappings, "GET", "/api/hello/say-hello/John");
        assertThat(result).isNotNull();
        assertThat(result.getRouteConfig().getGrpcMethod()).isEqualTo("/HelloService/SayHello");
        assertThat(result.getPathParams()).containsEntry("name", "John");
    }
}
