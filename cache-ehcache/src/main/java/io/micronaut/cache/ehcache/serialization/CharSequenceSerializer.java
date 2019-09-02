package io.micronaut.cache.ehcache.serialization;

import org.ehcache.impl.serialization.StringSerializer;
import org.ehcache.spi.serialization.Serializer;
import org.ehcache.spi.serialization.SerializerException;

import java.nio.ByteBuffer;

/**
 * TODO: javadoc
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
