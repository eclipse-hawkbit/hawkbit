/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.event;

import java.util.Objects;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.converter.MessageConverter;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = EventJacksonConfiguration.class)
class EventJacksonMessageConverterTest extends AbstractEventMessageConverterTest {

    private MessageConverter messageConverter;

    @Autowired
    void setJsonMapper(final JsonMapper jsonMapper) {
        messageConverter = new EventJacksonMessageConverter(jsonMapper);
    }

    protected MessageConverter messageConverter() {
        return Objects.requireNonNull(messageConverter, "MessageConverter has not been initialized");
    }
}