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

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class GrpcFramingUtilsTest {

    @Test
    public void shouldFrameAndUnframePayload() {
        byte[] payload = "test-payload".getBytes();
        byte[] framed = GrpcFramingUtils.framePayload(payload);

        assertThat(framed).isNotNull();
        assertThat(framed.length).isEqualTo(5 + payload.length);
        assertThat(framed[0]).isEqualTo((byte) 0);

        byte[] unframed = GrpcFramingUtils.unframePayload(framed);
        assertThat(unframed).isEqualTo(payload);
    }

    @Test
    public void shouldHandleEmptyOrInvalidFrame() {
        byte[] unframedNull = GrpcFramingUtils.unframePayload(null);
        assertThat(unframedNull).isEmpty();

        byte[] unframedShort = GrpcFramingUtils.unframePayload(new byte[]{0, 1, 2});
        assertThat(unframedShort).isEmpty();
    }
}
