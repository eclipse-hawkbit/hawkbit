/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.cache;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.hawkbit.repository.event.remote.DownloadProgressEvent;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Repository")
@Stories("CacheWriteNotify")
@RunWith(MockitoJUnitRunner.class)
public class CacheWriteNotifyTest {

    private static final long KNOWN_STATUS_ID = 1;

    @Mock
    private ApplicationEventPublisher eventPublisherMock;

    @Mock
    private CacheManager cacheManagerMock;

    @Mock
    private Cache cacheMock;

    @Mock
    private TenantAware tenantAwareMock;

    @Mock
    private ApplicationContext applicationContextMock;

    private CacheWriteNotify underTest;

    @Before
    public void before() {
        underTest = new CacheWriteNotify();
        underTest.setEventPublisher(eventPublisherMock);
        underTest.setCacheManager(cacheManagerMock);
        underTest.setTenantAware(tenantAwareMock);
        underTest.setApplicationContext(applicationContextMock);
    }

    @Test
    public void downloadgProgressIsCachedAndEventSent() {

        when(cacheManagerMock.getCache(JpaActionStatus.class.getName())).thenReturn(cacheMock);
        when(tenantAwareMock.getCurrentTenant()).thenReturn("default");

        underTest.downloadProgress(KNOWN_STATUS_ID, 500L, 100L, 100L);

        verify(cacheMock).put(KNOWN_STATUS_ID + "." + CacheKeys.DOWNLOAD_PROGRESS_PERCENT, 20);
        verify(eventPublisherMock).publishEvent(any(DownloadProgressEvent.class));
    }
}
