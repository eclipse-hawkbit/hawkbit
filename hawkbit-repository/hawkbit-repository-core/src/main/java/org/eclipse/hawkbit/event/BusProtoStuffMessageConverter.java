/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.event;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.MimeType;

import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * A customize message converter for the spring cloud events. The converter is
 * registered for the application/binary+protostuff type.
 *
 */
public class BusProtoStuffMessageConverter extends AbstractMessageConverter {

    public static final MimeType APPLICATION_BINARY_PROTOSTUFF = new MimeType("application", "binary+protostuff");
    private static final Logger LOG = LoggerFactory.getLogger(BusProtoStuffMessageConverter.class);
    private static final String DEFAULT_CLASS_FIELD_NAME = "__Class__";

    /**
     * Constructor.
     */
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
        final Object payload = message.getPayload();

        try {
            final Class<?> deserializeClass = ClassUtils
                    .getClass(message.getHeaders().get(DEFAULT_CLASS_FIELD_NAME).toString());
            if (payload instanceof byte[]) {
                @SuppressWarnings("unchecked")
                final Schema<Object> schema = (Schema<Object>) RuntimeSchema.getSchema(deserializeClass);
                final Object deserializeEvent = schema.newMessage();
                ProtobufIOUtil.mergeFrom((byte[]) message.getPayload(), deserializeEvent, schema);
                return deserializeEvent;
            }
        } catch (final ClassNotFoundException | RuntimeException e) {
            throw new MessageConversionException(message, "Failed to read payload", e);
        }

        return null;
    }

    @Override
    protected Object convertToInternal(final Object payload, final MessageHeaders headers,
            final Object conversionHint) {
        checkIfHeaderMutable(headers);
        final Class<? extends Object> serializeClass = payload.getClass();
        @SuppressWarnings("unchecked")
        final Schema<Object> schema = (Schema<Object>) RuntimeSchema.getSchema(serializeClass);
        final LinkedBuffer buffer = LinkedBuffer.allocate();
        final byte[] serializeByte;
        try {
            serializeByte = ProtostuffIOUtil.toByteArray(payload, schema, buffer);
        } finally {
            buffer.clear();
        }

        headers.put(DEFAULT_CLASS_FIELD_NAME, serializeClass.getName());
        return serializeByte;
    }

    private static void checkIfHeaderMutable(final MessageHeaders headers) {
        if (isAccessorMutable(headers) || headers instanceof MutableMessageHeaders) {
            return;
        }
        LOG.error("Protostuff cannot set serializae class because message header is not mutable");
        throw new MessageConversionException(
                "Cannot set the serialize class to message header. Need Mutable message header");
    }

    private static boolean isAccessorMutable(final MessageHeaders headers) {
        final MessageHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(headers, MessageHeaderAccessor.class);
        return accessor != null && accessor.isMutable();
    }

}
