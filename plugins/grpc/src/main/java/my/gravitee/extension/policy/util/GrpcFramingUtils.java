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

import java.nio.ByteBuffer;

public class GrpcFramingUtils {

    public static byte[] framePayload(byte[] payload) {
        if (payload == null) {
            payload = new byte[0];
        }
        byte[] frame = new byte[5 + payload.length];
        frame[0] = 0; // 0 = uncompressed
        ByteBuffer.wrap(frame, 1, 4).putInt(payload.length);
        System.arraycopy(payload, 0, frame, 5, payload.length);
        return frame;
    }

    public static byte[] unframePayload(byte[] grpcFrame) {
        if (grpcFrame == null || grpcFrame.length < 5) {
            return new byte[0];
        }
        int payloadLength = ByteBuffer.wrap(grpcFrame, 1, 4).getInt();
        if (grpcFrame.length - 5 < payloadLength) {
            return new byte[0];
        }
        byte[] payload = new byte[payloadLength];
        System.arraycopy(grpcFrame, 5, payload, 0, payloadLength);
        return payload;
    }
}
