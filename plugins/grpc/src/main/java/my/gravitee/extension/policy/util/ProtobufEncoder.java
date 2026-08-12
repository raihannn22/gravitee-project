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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ProtobufEncoder {

    public static byte[] encodeVarint(int fieldNum, long value) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long tag = ((long) fieldNum << 3) | 0;
        writeUvarint(baos, tag);
        writeUvarint(baos, value);
        return baos.toByteArray();
    }

    public static byte[] encodeString(int fieldNum, String value) {
        if (value == null) {
            value = "";
        }
        byte[] strBytes = value.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long tag = ((long) fieldNum << 3) | 2;
        writeUvarint(baos, tag);
        writeUvarint(baos, strBytes.length);
        try {
            baos.write(strBytes);
        } catch (IOException ignored) {}
        return baos.toByteArray();
    }

    public static void writeUvarint(ByteArrayOutputStream out, long value) {
        while ((value & ~0x7FL) != 0) {
            out.write((int) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        out.write((int) (value & 0x7F));
    }
}
