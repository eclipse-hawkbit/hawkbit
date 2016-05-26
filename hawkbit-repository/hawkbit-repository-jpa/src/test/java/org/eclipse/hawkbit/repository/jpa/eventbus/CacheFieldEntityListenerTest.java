/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.eventbus;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.hawkbit.repository.jpa.cache.CacheField;
import org.eclipse.hawkbit.repository.jpa.cache.CacheKeys;
import org.eclipse.hawkbit.repository.jpa.repository.model.helper.CacheManagerHolder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.hateoas.Identifiable;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Repository")
@Stories("EventBus")
@RunWith(MockitoJUnitRunner.class)
public class CacheFieldEntityListenerTest {

    private static final String TEST_CACHE_FIELD = "testCacheField";

    @Mock
    private CacheManager cacheManagerMock;

    @Mock
    private Cache cacheMock;

    private CacheFieldEntityListener underTest;

    @Before
    public void before() {
        // mock
        when(cacheManagerMock.getCache(anyString())).thenReturn(cacheMock);

        underTest = new CacheFieldEntityListener();
        CacheManagerHolder.getInstance().setCacheManager(cacheManagerMock);
    }

    @Test
    public void postLoadSetsCacheFields() {
        final String entityId = "123";
        final String normalFieldValue = "bumlux";
        final TestEntity testObject = new TestEntity(entityId, normalFieldValue);
        final int expectedTestValue = -1;

        when(cacheMock.get(CacheKeys.entitySpecificCacheKey(entityId, TEST_CACHE_FIELD)))
                .thenReturn(new SimpleValueWrapper(expectedTestValue));

        // pre verify everything is ok
        assertThat(testObject.getCacheField()).isNotEqualTo(expectedTestValue);
        assertThat(testObject.getNormalField()).isEqualTo(normalFieldValue);

        // test
        underTest.postLoad(testObject);

        assertThat(testObject.getNormalField()).isEqualTo(normalFieldValue);
        // now cache value should be like the value in the mock of the cache
        assertThat(testObject.getCacheField()).isEqualTo(expectedTestValue);
    }

    @Test
    public void postRemoveEvictsCacheField() {
        final String entityId = "123";
        final String normalFieldValue = "bumlux";
        final TestEntity testObject = new TestEntity(entityId, normalFieldValue);
        // test
        underTest.postDelete(testObject);

        verify(cacheMock).evict(eq(CacheKeys.entitySpecificCacheKey(entityId, TEST_CACHE_FIELD)));
    }

    private final class TestEntity implements Identifiable<String> {

        private final String id;

        private final String normalField;

        @CacheField(key = TEST_CACHE_FIELD)
        private int cacheField;

        private TestEntity(final String id, final String normalFieldValue) {
            this.id = id;
            this.normalField = normalFieldValue;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.springframework.hateoas.Identifiable#getId()
         */
        @Override
        public String getId() {
            return id;
        }

        /**
         * @return the normalField
         */
        String getNormalField() {
            return normalField;
        }

        /**
         * @return the cacheField
         */
        int getCacheField() {
            return cacheField;
        }
    }
}
