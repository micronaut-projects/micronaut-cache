/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.oracle.serialization;

import java.util.Arrays;

/**
 * Canonicalized cache key payload and hash.
 */
public final class OracleCacheKey {

    private final byte[] keyHash;
    private final byte[] keyPayload;

    public OracleCacheKey(byte[] keyHash, byte[] keyPayload) {
        this.keyHash = keyHash;
        this.keyPayload = keyPayload;
    }

    public byte[] getKeyHash() {
        return keyHash;
    }

    public byte[] getKeyPayload() {
        return keyPayload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OracleCacheKey that)) {
            return false;
        }
        return Arrays.equals(keyHash, that.keyHash) && Arrays.equals(keyPayload, that.keyPayload);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(keyHash);
        result = 31 * result + Arrays.hashCode(keyPayload);
        return result;
    }
}
