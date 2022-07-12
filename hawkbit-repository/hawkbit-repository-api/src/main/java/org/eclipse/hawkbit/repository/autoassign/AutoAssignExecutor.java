/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.autoassign;

/**
 * An interface declaration which contains the check for the auto assignment
 * logic.
 */
public interface AutoAssignExecutor {

    /**
     * Checks all target filter queries with an auto assign distribution set and
     * triggers the check and assignment to targets that don't have the design DS
     * yet
     */
    void checkAllTargets();

    /**
     * Method performs an auto assign check for a specific device only
     *
     * @param controllerId
     *            of the device to check
     */
    void checkSingleTarget(String controllerId);

}
