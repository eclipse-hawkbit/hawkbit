/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.acm.context;

import java.util.function.Supplier;

public interface ContextRunner {

    /**
     * Serialize the current context to be able to reset it again with
     * {@link ContextRunner#runInContext(String, Runnable)}. Needed for scheduled
     * background operations like auto assignments. See
     * {@link JpaTargetFilterQuery#getAccessControlContext()} and
     * {@link AutoAssignChecker#checkAllTargets()}
     * 
     * @return null if there is nothing to serialize. Context will not be restored
     *         in background tasks.
     */
    String getCurrentContext();

    /**
     * Wrap a specific execution in a known and pre-serialized context.
     * 
     * @param serializedContext
     *            created by {@link ContextRunner#getCurrentContext()}
     * @param runnable
     *            operation to execute in the reconstructed context
     */
    void runInContext(String serializedContext, Runnable runnable);

    /**
     * Wrap a specific execution in an admin context to avoid access limitations.
     * Needed to check for duplicates.
     * 
     * @param supplier
     *            define execution having a return.
     * @param <T>
     *            to return supplied answer.
     * @return the execution result
     */
    <T> T runInAdminContext(Supplier<T> supplier);

}
