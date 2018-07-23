/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
