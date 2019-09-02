/*
 * Copyright 2017-2019 original authors
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
package io.micronaut.cache.ehcache.serialization;

import org.ehcache.impl.serialization.StringSerializer;
import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;

import java.nio.ByteBuffer;

/**
 * A {@link org.ehcache.spi.serialization.Serializer} implementation for {@link java.lang.CharSequence} that delegates
 * to {@link org.ehcache.impl.serialization.StringSerializer}.
 *
 * @author Álvaro Sánchez-Mariscal
 * @since 1.0.0
 */
public class CharSequenceSerializer implements Serializer<CharSequence> {

    private final StringSerializer stringSerializer;

    public CharSequenceSerializer() {
        this.stringSerializer = new StringSerializer();
    }

    @Override
    public ByteBuffer serialize(CharSequence object) throws SerializerException {
        return stringSerializer.serialize(object.toString());
    }

    @Override
    public CharSequence read(ByteBuffer binary) throws ClassNotFoundException, SerializerException {
        return stringSerializer.read(binary);
    }

    @Override
    public boolean equals(CharSequence object, ByteBuffer binary) throws ClassNotFoundException, SerializerException {
        return stringSerializer.equals(object.toString(), binary);
    }
}
