/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.ThreadFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NamingThreadFactoryTest {
    @Mock
    private final Runnable runnableMock = mock(Runnable.class);

    @Test
    public void setsNameForThreads() {
        final String knownName = "knownName";
        final ThreadFactory threadFactory = new NamingThreadFactory(knownName);
        final Thread newThread1 = threadFactory.newThread(runnableMock);
        final Thread newThread2 = threadFactory.newThread(runnableMock);

        assertThat(newThread1.getName()).isEqualTo(NamingThreadFactory.SP_PREFIX + knownName);
        assertThat(newThread2.getName()).isEqualTo(NamingThreadFactory.SP_PREFIX + knownName);
    }

    @Test
    public void setsFormatedNameForThreads() {
        final String nameFormat = "knownName-%d";
        final String knownName1 = "knownName-0";
        final String knownName2 = "knownName-1";
        final ThreadFactory threadFactory = new NamingThreadFactory(nameFormat);
        final Thread newThread1 = threadFactory.newThread(runnableMock);
        final Thread newThread2 = threadFactory.newThread(runnableMock);

        assertThat(newThread1.getName()).isEqualTo(NamingThreadFactory.SP_PREFIX + knownName1);
        assertThat(newThread2.getName()).isEqualTo(NamingThreadFactory.SP_PREFIX + knownName2);
    }

    @Test
    public void setsRunnableForThreads() {
        final String knownName = "knownName";
        final ThreadFactory threadFactory = new NamingThreadFactory(knownName);
        final Thread newThread1 = threadFactory.newThread(runnableMock);
        final Thread newThread2 = threadFactory.newThread(runnableMock);

        newThread1.run();
        newThread2.run();

        verify(runnableMock, times(2)).run();
    }
}
