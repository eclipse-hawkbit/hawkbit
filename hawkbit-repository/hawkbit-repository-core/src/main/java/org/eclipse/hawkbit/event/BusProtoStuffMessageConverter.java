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

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.MimeType;

/**
 * A customize message converter for the spring cloud events. The converter is
 * registered for the application/binary+protostuff type.
 *
 * The clazz-type-information is encoded into the message payload infront with a
 * length of {@link #EVENT_TYPE_LENGTH}. This is necessary due in case of
 * rabbitMQ batching the message headers will be merged together and custom
 * message header information will get lost. So in this implementation the
 * information about the event-type is encoded in the payload of the message
 * directly using the encoded values of {@link EventType}.
 */
@Slf4j
public class BusProtoStuffMessageConverter extends AbstractMessageConverter {

    public static final MimeType APPLICATION_BINARY_PROTOSTUFF = new MimeType("application", "binary+protostuff");

    /**
     * The length of the class type length of the payload.
     */
    private static final byte EVENT_TYPE_LENGTH = 2;

    public BusProtoStuffMessageConverter() {
        super(APPLICATION_BINARY_PROTOSTUFF);
    }

    @Override
    protected boolean supports(final Class<?> aClass) {
        return RemoteApplicationEvent.class.isAssignableFrom(aClass);
    }

    @Override
    public Object convertFromInternal(final Message<?> message, final Class<?> targetClass,
            final Object conversionHint) {
        final Object objectPayload = message.getPayload();
        if (objectPayload instanceof byte[] payload) {
            final byte[] clazzHeader = extractClazzHeader(payload);
            final byte[] content = extraxtContent(payload);

            final EventType eventType = readClassHeader(clazzHeader);
            return readContent(eventType, content);
        }
        return null;
    }

    @Override
    protected Object convertToInternal(final Object payload, final MessageHeaders headers,
            final Object conversionHint) {

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
        final byte[] clazzHeader = new byte[EVENT_TYPE_LENGTH];
        System.arraycopy(payload, 0, clazzHeader, 0, EVENT_TYPE_LENGTH);
        return clazzHeader;
    }

    private static byte[] extraxtContent(final byte[] payload) {
        final byte[] content = new byte[payload.length - EVENT_TYPE_LENGTH];
        System.arraycopy(payload, EVENT_TYPE_LENGTH, content, 0, content.length);
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
        @SuppressWarnings("unchecked") final Schema<Object> schema = (Schema<Object>) RuntimeSchema
                .getSchema((Class<?>) EventType.class);
        final LinkedBuffer buffer = LinkedBuffer.allocate();
        return ProtobufIOUtil.toByteArray(clazzEventType, schema, buffer);
    }
}
