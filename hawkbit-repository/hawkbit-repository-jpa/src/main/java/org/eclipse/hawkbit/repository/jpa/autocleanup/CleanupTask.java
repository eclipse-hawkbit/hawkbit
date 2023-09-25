/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.autocleanup;

/**
 * Interface modeling a cleanup task.
 */
public interface CleanupTask extends Runnable {

    /**
     * Executes the cleanup task.
     */
    @Override
    void run();

    /**
     * @return The identifier of this cleanup task. Never null.
     */
    String getId();

}
