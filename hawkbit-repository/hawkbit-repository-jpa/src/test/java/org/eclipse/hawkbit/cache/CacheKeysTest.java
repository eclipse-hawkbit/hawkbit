/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.cache;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Repository")
@Stories("CacheKeys")
public class CacheKeysTest {

    @Test
    public void entitySpecificCacheKeyPattern() {
        final String knownEntityId = "123";
        final String knownCacheKey = "someField";
        final String entitySpecificCacheKey = CacheKeys.entitySpecificCacheKey(knownEntityId, knownCacheKey);
        assertThat(entitySpecificCacheKey).isEqualTo(knownEntityId + "." + knownCacheKey);
    }
}
