/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleValueWrapper;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Unit Tests - Cache")
@Story("Download ID Cache")
@ExtendWith(MockitoExtension.class)
public class DefaultDownloadIdCacheTest {

    @Mock
    private CacheManager cacheManagerMock;

    @Mock
    private TenancyCacheManager tenancyCacheManagerMock;

    @Mock
    private Cache cacheMock;

    @Captor
    private ArgumentCaptor<String> cacheManagerKeyCaptor;

    @Captor
    private ArgumentCaptor<DownloadArtifactCache> cacheManagerValueCaptor;

    private DefaultDownloadIdCache underTest;

    private final String knownKey = "12345";

    @BeforeEach
    public void before() {
        underTest = new DefaultDownloadIdCache(cacheManagerMock);
        lenient().when(cacheManagerMock.getCache(DefaultDownloadIdCache.DOWNLOAD_ID_CACHE)).thenReturn(cacheMock);
    }

    @Test
    @Description("Verifies that putting key and value is delegated to the CacheManager implementation")
    public void putKeyAndValueIsDelegatedToCacheManager() {
        final DownloadArtifactCache value = new DownloadArtifactCache(DownloadType.BY_SHA1, knownKey);

        underTest.put(knownKey, value);

        verify(cacheMock).put(cacheManagerKeyCaptor.capture(), cacheManagerValueCaptor.capture());

        assertThat(cacheManagerKeyCaptor.getValue()).isEqualTo(knownKey);
        assertThat(cacheManagerValueCaptor.getValue()).isEqualTo(value);
    }

    @Test
    @Description("Verifies that evicting a key is delegated to the CacheManager implementation")
    public void evictKeyIsDelegatedToCacheManager() {

        underTest.evict(knownKey);

        verify(cacheMock).evict(cacheManagerKeyCaptor.capture());

        assertThat(cacheManagerKeyCaptor.getValue()).isEqualTo(knownKey);
    }

    @Test
    @Description("Verifies that retrieving a value for a specific key is delegated to the CacheManager implementation")
    public void getValueReturnsTheAssociatedValueForKey() {
        final String knownKey = "12345";
        final DownloadArtifactCache knownValue = new DownloadArtifactCache(DownloadType.BY_SHA1, knownKey);

        when(cacheMock.get(knownKey)).thenReturn(new SimpleValueWrapper(knownValue));

        final DownloadArtifactCache downloadArtifactCache = underTest.get(knownKey);

        assertThat(downloadArtifactCache).isEqualTo(knownValue);
    }

    @Test
    @Description("Verifies that retrieving a null value for a specific key is delegated to the CacheManager implementation")
    public void getValueReturnsNullIfNoKeyIsAssociated() {

        when(cacheMock.get(knownKey)).thenReturn(new SimpleValueWrapper(null));

        final DownloadArtifactCache downloadArtifactCache = underTest.get(knownKey);

        assertThat(downloadArtifactCache).isNull();
    }

    @Test
    @Description("Verifies that TenancyCacheManager is using direct cache because download-ids are global unique and don't need to run as tenant aware")
    public void tenancyCacheManagerIsUsingDirectCache() {

        when(tenancyCacheManagerMock.getDirectCache(DefaultDownloadIdCache.DOWNLOAD_ID_CACHE)).thenReturn(cacheMock);
        underTest = new DefaultDownloadIdCache(tenancyCacheManagerMock);

        final DownloadArtifactCache value = new DownloadArtifactCache(DownloadType.BY_SHA1, knownKey);

        underTest.put(knownKey, value);

        verify(cacheMock).put(cacheManagerKeyCaptor.capture (), cacheManagerValueCaptor.capture());

        verify(tenancyCacheManagerMock).getDirectCache(DefaultDownloadIdCache.DOWNLOAD_ID_CACHE);
        assertThat(cacheManagerKeyCaptor.getValue()).isEqualTo(knownKey);
        assertThat(cacheManagerValueCaptor.getValue()).isEqualTo(value);
    }
}
