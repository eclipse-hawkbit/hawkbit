/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.event;

import java.nio.ByteBuffer;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.event.remote.AbstractRemoteEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.MimeType;

/**
 * A custom message converter for Spring Cloud Stream using Protostuff serialization.
 * The converter is registered for the {@code application/binary+protostuff} content type.
 * It embeds the {@link EventType} metadata inside the message payload to preserve the type
 * during deserialization — even when message headers may be lost (e.g. with RabbitMQ batching).
 *
 * <p>
 * Message Structure:
 * <ul>
 *   <li>The first {@link #HEADER_LENGTH_PREFIX_SIZE} bytes are an integer indicating the length N (in bytes) of the serialized {@link EventType}.</li>
 *   <li>Next N bytes contain the serialized {@link EventType} data itself.</li>
 *   <li>The remaining bytes represent the serialized {@link AbstractRemoteEvent} payload.</li>
 * </ul>
 *
 * This format allows decoding messages without relying on external headers, ensuring robustness
 * in systems where header information may be merged or dropped.
 */
@Slf4j
public class EventProtoStuffMessageConverter extends AbstractMessageConverter {

    public static final MimeType APPLICATION_BINARY_PROTOSTUFF = new MimeType("application", "binary+protostuff");
    private static final int HEADER_LENGTH_PREFIX_SIZE = 4;

    public EventProtoStuffMessageConverter() {
        super(APPLICATION_BINARY_PROTOSTUFF);
    }

    @Override
    @NullMarked
    protected boolean supports(final Class<?> aClass) {
        return AbstractRemoteEvent.class.isAssignableFrom(aClass);
    }

    @Override
    @NullMarked
    @Nullable
    protected Object convertFromInternal(final Message<?> message, final Class<?> targetClass, @Nullable final Object conversionHint) {
        final Object objectPayload = message.getPayload();
        if (objectPayload instanceof byte[] payload) {
            final byte[] clazzHeader = extractClazzHeader(payload);
            final byte[] content = extractContent(payload);

            final EventType eventType = readClassHeader(clazzHeader);
            return readContent(eventType, content);
        }
        return null;
    }

    @Override
    protected Object convertToInternal(final Object payload, final MessageHeaders headers, final Object conversionHint) {
        final byte[] clazzHeader = writeClassHeader(payload.getClass());
        final byte[] writeContent = writeContent(payload);
        return mergeClassHeaderAndContent(clazzHeader, writeContent);
    }

    private static Object readContent(final EventType eventType, final byte[] content) {
        final Class<?> targetClass = eventType.getTargetClass();
        if (targetClass == null) {
            log.error("Cannot read clazz header for given EventType value {}, missing mapping", eventType.getValue());
            throw new MessageConversionException("Missing mapping of EventType for value " + eventType.getValue());
        }
        @SuppressWarnings("unchecked") final Schema<Object> schema = (Schema<Object>) RuntimeSchema.getSchema(targetClass);
        final Object deserializeEvent = schema.newMessage();
        ProtobufIOUtil.mergeFrom(content, deserializeEvent, schema);
        return deserializeEvent;
    }

    private static byte[] mergeClassHeaderAndContent(final byte[] clazzHeader, final byte[] writeContent) {
        final byte[] body = new byte[clazzHeader.length + writeContent.length];
        System.arraycopy(clazzHeader, 0, body, 0, clazzHeader.length);
        System.arraycopy(writeContent, 0, body, clazzHeader.length, writeContent.length);
        return body;
    }

    private static byte[] extractClazzHeader(final byte[] payload) {
        ByteBuffer wrapper = ByteBuffer.wrap(payload);
        int headerLength = wrapper.getInt();
        byte[] clazzHeader = new byte[headerLength];
        wrapper.get(clazzHeader);
        return clazzHeader;
    }

    private static byte[] extractContent(final byte[] payload) {
        ByteBuffer wrapper = ByteBuffer.wrap(payload);
        int headerLength = wrapper.getInt();
        byte[] content = new byte[payload.length - HEADER_LENGTH_PREFIX_SIZE - headerLength];
        wrapper.position(HEADER_LENGTH_PREFIX_SIZE + headerLength);
        wrapper.get(content);
        return content;
    }

    private static EventType readClassHeader(final byte[] typeInformation) {
        final Schema<EventType> schema = RuntimeSchema.getSchema(EventType.class);
        final EventType deserializedType = schema.newMessage();
        ProtobufIOUtil.mergeFrom(typeInformation, deserializedType, schema);
        return deserializedType;
    }

    private static byte[] writeContent(final Object payload) {
        final Class<?> serializeClass = payload.getClass();
        @SuppressWarnings("unchecked") final Schema<Object> schema = (Schema<Object>) RuntimeSchema.getSchema(serializeClass);
        final LinkedBuffer buffer = LinkedBuffer.allocate();
        return ProtobufIOUtil.toByteArray(payload, schema, buffer);
    }

    private static byte[] writeClassHeader(final Class<?> clazz) {
        final EventType clazzEventType = EventType.from(clazz);
        if (clazzEventType == null) {
            log.error("There is no mapping to EventType for the given class {}", clazz);
            throw new MessageConversionException("Missing EventType for given class : " + clazz);
        }

        @SuppressWarnings("unchecked") final Schema<Object> schema = (Schema<Object>) RuntimeSchema.getSchema((Class<?>) EventType.class);
        final LinkedBuffer buffer = LinkedBuffer.allocate();
        byte[] typeBytes = ProtobufIOUtil.toByteArray(clazzEventType, schema, buffer);

        ByteBuffer result = ByteBuffer.allocate(HEADER_LENGTH_PREFIX_SIZE + typeBytes.length);
        result.putInt(typeBytes.length);
        result.put(typeBytes);
        return result.array();
    }

}