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
import org.springframework.context.annotation.Description;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Component Tests - UI")
@Stories("Threads with NamingThreadFactory")
@RunWith(MockitoJUnitRunner.class)
public class NamingThreadFactoryTest {
    @Mock
    private final Runnable runnableMock = mock(Runnable.class);

    @Test
    @Description("Correct name of threads when created through NamingThreadFactory.")
    public void setsNameForThreads() {
        final String knownName = "knownName";
        final ThreadFactory threadFactory = new NamingThreadFactory(knownName);
        final Thread newThread1 = threadFactory.newThread(runnableMock);
        final Thread newThread2 = threadFactory.newThread(runnableMock);

        assertThat(newThread1.getName()).as("Name of the thread").isEqualTo(NamingThreadFactory.SP_PREFIX + knownName);
        assertThat(newThread2.getName()).as("Name of the thread").isEqualTo(NamingThreadFactory.SP_PREFIX + knownName);
    }

    @Test
    @Description("Correct name of threads when created through NamingThreadFactory with formated name.")
    public void setsFormatedNameForThreads() {
        final String nameFormat = "knownName-%d";
        final String knownName1 = "knownName-0";
        final String knownName2 = "knownName-1";
        final ThreadFactory threadFactory = new NamingThreadFactory(nameFormat);
        final Thread newThread1 = threadFactory.newThread(runnableMock);
        final Thread newThread2 = threadFactory.newThread(runnableMock);

        assertThat(newThread1.getName()).as("Name of the thread").isEqualTo(NamingThreadFactory.SP_PREFIX + knownName1);
        assertThat(newThread2.getName()).as("Name of the thread").isEqualTo(NamingThreadFactory.SP_PREFIX + knownName2);
    }

    @Test
    @Description("Created threads run are running.")
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
