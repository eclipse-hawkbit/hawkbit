/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm.context;

import org.eclipse.hawkbit.repository.acm.context.ContextRunner;

import java.util.function.Supplier;

/**
 * Default implementation of the {@link ContextRunner}.
 */
public class DefaultContextRunner implements ContextRunner {

    @Override
    public String getCurrentContext() {
        return null;
    }

    @Override
    public void runInContext(final String serializedContext, final Runnable runnable) {
        runnable.run();
    }

    @Override
    public <T> T runInAdminContext(final Supplier<T> supplier) {
        return supplier.get();
    }
}
