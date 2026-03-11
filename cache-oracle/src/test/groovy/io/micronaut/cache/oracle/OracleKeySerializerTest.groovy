/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cache.oracle

import io.micronaut.cache.interceptor.ParametersKey
import io.micronaut.cache.oracle.serialization.OracleKeySerializer
import io.micronaut.core.convert.DefaultMutableConversionService
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class OracleKeySerializerTest extends Specification {

    private final OracleKeySerializer serializer = new OracleKeySerializer(new DefaultMutableConversionService())

    void canonicalizesNestedArguments() {
        given:
        def nestedMap = [
                b      : [3, 2, 1],
                a      : [x: 1, y: [2, 3]],
                message: 'hello'
        ]
        ParametersKey key = new ParametersKey('user-1', nestedMap, ['z', 'y'])

        when:
        def first = serializer.serialize(key)
        def second = serializer.serialize(key)

        then:
        // Deterministic payload is required so semantically equal keys hash to exactly the same byte sequence.
        first == second
        new String(first.keyPayload, StandardCharsets.UTF_8) == '["user-1",{"a":{"x":1,"y":[2,3]},"b":[3,2,1],"message":"hello"},["z","y"]]'
        first.keyHash.length == 32
    }

    void ignoresMapInsertionOrder() {
        given:
        LinkedHashMap<String, Object> firstMap = new LinkedHashMap<>()
        firstMap.put('b', 2)
        firstMap.put('a', [k2: 'v2', k1: 'v1'])

        LinkedHashMap<String, Object> secondMap = new LinkedHashMap<>()
        secondMap.put('a', [k1: 'v1', k2: 'v2'])
        secondMap.put('b', 2)

        when:
        def first = serializer.serialize(new ParametersKey(firstMap))
        def second = serializer.serialize(new ParametersKey(secondMap))

        then:
        // Map ordering differences must not change cache key identity.
        first == second
    }
}
