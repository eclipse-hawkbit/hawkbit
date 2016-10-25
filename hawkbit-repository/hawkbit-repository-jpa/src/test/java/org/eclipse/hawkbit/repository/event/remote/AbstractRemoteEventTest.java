/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import java.util.Map;

import org.eclipse.hawkbit.event.BusProtoStuffMessageConverter;
import org.eclipse.hawkbit.repository.event.TenantAwareEvent;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;

import com.google.common.collect.Maps;

/**
 * Test the remote entity events.
 */
public abstract class AbstractRemoteEventTest extends AbstractJpaIntegrationTest {

    @Autowired
    private BusProtoStuffMessageConverter abstractMessageConverter;

    protected AbstractMessageConverter getAbstractMessageConverter() {
        return abstractMessageConverter;
    }

    protected Message<?> createMessage(final TenantAwareEvent event) {
        final Map<String, Object> headers = Maps.newLinkedHashMap();
        headers.put(MessageHeaders.CONTENT_TYPE, BusProtoStuffMessageConverter.APPLICATION_BINARY_PROTOSTUFF);
        return abstractMessageConverter.toMessage(event, new MutableMessageHeaders(headers));
    }

    protected Message<?> createMessageWithImmutableHeader(final TenantAwareEvent event) {
        final Map<String, Object> headers = Maps.newLinkedHashMap();
        return abstractMessageConverter.toMessage(event, new MessageHeaders(headers));
    }
}
