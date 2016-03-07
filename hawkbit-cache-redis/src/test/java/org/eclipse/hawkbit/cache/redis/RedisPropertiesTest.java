/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.cache.redis;

import static org.fest.assertions.api.Assertions.assertThat;

import org.eclipse.hawkbit.cache.RedisProperties;
import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Cluster Cache")
@Stories("Redis Properties Test")
public class RedisPropertiesTest {

    @Test
    public void setAndGetProps() {
        final String knownHost = "bumlux";
        final int knownPort = 1234;

        final RedisProperties underTest = new RedisProperties();
        underTest.setHost(knownHost);
        underTest.setPort(knownPort);

        assertThat(underTest.getHost()).isEqualTo(knownHost);
        assertThat(underTest.getPort()).isEqualTo(knownPort);
    }

}
