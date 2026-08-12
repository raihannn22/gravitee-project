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

public class ProtobufEncoderTest {

    @Test
    public void shouldEncodeStringField() {
        byte[] encoded = ProtobufEncoder.encodeString(1, "hello");
        assertThat(encoded).isNotNull();
        assertThat(encoded.length).isGreaterThan(5);
        // Tag for field 1 wire type 2: (1 << 3) | 2 = 10 (0x0a)
        assertThat(encoded[0]).isEqualTo((byte) 0x0a);
        // Length: 5
        assertThat(encoded[1]).isEqualTo((byte) 5);
        // Content: "hello"
        assertThat(new String(encoded, 2, 5)).isEqualTo("hello");
    }

    @Test
    public void shouldEncodeVarintField() {
        byte[] encoded = ProtobufEncoder.encodeVarint(1, 42);
        assertThat(encoded).isNotNull();
        // Tag for field 1 wire type 0: (1 << 3) | 0 = 8 (0x08)
        assertThat(encoded[0]).isEqualTo((byte) 0x08);
        // Value: 42 (0x2a)
        assertThat(encoded[1]).isEqualTo((byte) 42);
    }
}
