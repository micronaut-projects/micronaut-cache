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
import io.micronaut.context.ApplicationContext
import io.micronaut.core.type.Argument
import io.micronaut.serde.oracle.jdbc.json.OracleJdbcJsonBinaryObjectMapper
import spock.lang.AutoCleanup
import spock.lang.Specification

class OracleKeySerializerTest extends Specification {

    @AutoCleanup
    private final ApplicationContext context = ApplicationContext.run()
    private OracleKeySerializer serializer
    private OracleJdbcJsonBinaryObjectMapper jsonMapper

    void setup() {
        serializer = context.getBean(OracleKeySerializer)
        jsonMapper = context.getBean(OracleJdbcJsonBinaryObjectMapper)
    }

    void serializesNestedArguments() {
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
        first == second
        jsonMapper.readValue(first.keyPayload, Argument.listOf(Object)) == [
                'user-1',
                [a: [x: 1, y: [2, 3]], b: [3, 2, 1], message: 'hello'],
                ['z', 'y']
        ]
        first.keyHash.length == 32
    }

    void mapPayloadsWithDifferentKeyOrdersResultInSameKey() {
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
        first == second
    }

    void singleMapPayloadsWithDifferentKeyOrdersResultInSameKey() {
        given:
        LinkedHashMap<String, Object> firstMap = new LinkedHashMap<>()
        firstMap.put('b', 2)
        firstMap.put('a', [k2: 'v2', k1: 'v1'])

        LinkedHashMap<String, Object> secondMap = new LinkedHashMap<>()
        secondMap.put('a', [k1: 'v1', k2: 'v2'])
        secondMap.put('b', 2)

        when:
        def first = serializer.serialize(firstMap)
        def second = serializer.serialize(secondMap)

        then:
        first == second
    }

    void parametersKeySupportsNullArguments() {
        when:
        def key = serializer.serialize(new ParametersKey(null, 'x'))

        then:
        jsonMapper.readValue(key.keyPayload, Argument.listOf(Object)) == [null, 'x']
    }
}
