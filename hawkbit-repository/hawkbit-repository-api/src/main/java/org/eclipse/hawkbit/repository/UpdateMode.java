/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

/**
 * Enumerates the supported update modes. Each mode represents an attribute update strategy.
 *
 * @see ControllerManagement
 */
public enum UpdateMode {

    /**
     * Merge update strategy
     */
    MERGE,

    /**
     * Replacement update strategy
     */
    REPLACE,

    /**
     * Removal update strategy
     */
    REMOVE;
}