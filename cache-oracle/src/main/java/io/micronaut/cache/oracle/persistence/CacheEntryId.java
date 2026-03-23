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
package io.micronaut.cache.oracle.persistence;

import io.micronaut.data.annotation.Embeddable;
import io.micronaut.data.annotation.MappedProperty;

import java.util.Arrays;
import java.util.Objects;

/**
 * Composite key for a cache entry row.
 *
 * @param cacheName The cache name
 * @param keyHash The serialized key hash
 * @author Davide Cocco
 * @since 6.0.0
 */
@Embeddable
public record CacheEntryId(
    @MappedProperty("CACHE_NAME") String cacheName,
    @MappedProperty("KEY_HASH") byte[] keyHash
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CacheEntryId that)) {
            return false;
        }
        return Objects.equals(cacheName, that.cacheName)
            && Arrays.equals(keyHash, that.keyHash);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(cacheName);
        result = 31 * result + Arrays.hashCode(keyHash);
        return result;
    }
}
