/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event.remote;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.core.JsonProcessingException;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - Repository")
@Stories("RemoteTenantAwareEvent Tests")
public class RemoteTenantAwareEventTest extends AbstractRemoteEventTest {

    @Test
    @Description("Verifies that the download progress reloading by remote events works")
    public void reloadDownloadProgessByRemoteEvent() throws JsonProcessingException {
        final DownloadProgressEvent downloadProgressEvent = new DownloadProgressEvent("DEFAULT", 3L, "Node");

        final Message<?> message = createMessage(downloadProgressEvent);

        final DownloadProgressEvent remoteEvent = (DownloadProgressEvent) getAbstractMessageConverter()
                .fromMessage(message, DownloadProgressEvent.class);
        assertThat(downloadProgressEvent).isEqualTo(remoteEvent);
    }

}
