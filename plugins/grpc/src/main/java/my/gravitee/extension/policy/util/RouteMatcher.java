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

import my.gravitee.extension.policy.model.RouteConfig;

import java.util.HashMap;
import java.util.Map;

public class RouteMatcher {

    public static class MatchResult {
        private final RouteConfig routeConfig;
        private final Map<String, String> pathParams;

        public MatchResult(RouteConfig routeConfig, Map<String, String> pathParams) {
            this.routeConfig = routeConfig;
            this.pathParams = pathParams != null ? pathParams : new HashMap<>();
        }

        public RouteConfig getRouteConfig() {
            return routeConfig;
        }

        public Map<String, String> getPathParams() {
            return pathParams;
        }
    }

    public static MatchResult match(Map<String, RouteConfig> mappings, String method, String path) {
        if (mappings == null || mappings.isEmpty()) {
            return null;
        }

        // Clean query params from path if present
        if (path.contains("?")) {
            path = path.substring(0, path.indexOf("?"));
        }

        // 1. Exact match with method
        String exactKey = method + " " + path;
        if (mappings.containsKey(exactKey)) {
            return new MatchResult(mappings.get(exactKey), new HashMap<>());
        }

        // 2. Exact match without method
        if (mappings.containsKey(path)) {
            return new MatchResult(mappings.get(path), new HashMap<>());
        }

        String[] incomingSegments = trimSlash(path).split("/");

        for (Map.Entry<String, RouteConfig> entry : mappings.entrySet()) {
            String patternKey = entry.getKey();
            RouteConfig config = entry.getValue();

            String patternMethod = "";
            String patternPath = patternKey;

            if (patternKey.contains(" ")) {
                String[] parts = patternKey.split(" ", 2);
                patternMethod = parts[0];
                patternPath = parts[1];
            }

            if (patternPath.contains("?")) {
                patternPath = patternPath.substring(0, patternPath.indexOf("?"));
            }

            if (!patternMethod.isEmpty() && !patternMethod.equalsIgnoreCase(method)) {
                continue;
            }

            String[] patternSegments = trimSlash(patternPath).split("/");
            if (patternSegments.length != incomingSegments.length) {
                continue;
            }

            boolean isMatch = true;
            Map<String, String> pathParams = new HashMap<>();

            for (int i = 0; i < patternSegments.length; i++) {
                String pSeg = patternSegments[i];
                String iSeg = incomingSegments[i];

                if (pSeg.startsWith("{") && pSeg.endsWith("}") && pSeg.length() > 2) {
                    String paramName = pSeg.substring(1, pSeg.length() - 1);
                    pathParams.put(paramName, iSeg);
                } else if (!pSeg.equals(iSeg)) {
                    isMatch = false;
                    break;
                }
            }

            if (isMatch) {
                return new MatchResult(config, pathParams);
            }
        }

        return null;
    }

    private static String trimSlash(String str) {
        if (str == null) return "";
        while (str.startsWith("/")) {
            str = str.substring(1);
        }
        while (str.endsWith("/")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }
}
