/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of {@link ThreadFactory} that sets thread names according to
 * given name format. All threads are created by
 * {@link Executors#defaultThreadFactory() #newThread(Runnable)}.
 * 
 *
 */
public final class NamingThreadFactory implements ThreadFactory {
    static final String SP_PREFIX = "SP-";

    private final String nameFormat;
    private final AtomicLong counter = new AtomicLong();

    /**
     * Creates a new {@link NamingThreadFactory}.
     * 
     * @param nameFormat
     *            a {@link String#format(String, Object...)}-compatible format
     *            String, to which a unique integer (0, 1, etc.) will be
     *            supplied as the single parameter. This integer will be
     *            assigned sequentially. For example, "rpc-pool-%d" will
     *            generate thread names like "rpc-pool-0", "rpc-pool-1",
     *            "rpc-pool-2", etc.
     */
    public NamingThreadFactory(final String nameFormat) {
        this.nameFormat = SP_PREFIX + nameFormat;
    }

    @Override
    public Thread newThread(final Runnable r) {
        final Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setName(String.format(nameFormat, counter.getAndIncrement()));
        return thread;
    }
}
