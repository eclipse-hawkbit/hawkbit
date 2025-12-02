/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

/**
 * An interface declaration which contains the check for the auto assignment logic.
 */
public interface AutoAssignHandler {

    /**
     * Checks all target filter queries with an auto assign distribution set and triggers the check and assignment to targets that don't have
     * the design DS yet
     */
    void handleAll();

    /**
     * Method performs an auto assign check for a specific device only
     *
     * @param controllerId of the device to check
     */
    void handleSingleTarget(String controllerId);
}