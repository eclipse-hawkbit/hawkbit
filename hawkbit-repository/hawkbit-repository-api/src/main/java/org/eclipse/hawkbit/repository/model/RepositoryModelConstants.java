/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

/**
 * Repository model constants.
 *
 */
public final class RepositoryModelConstants {

    /**
     * indicating that target action has no force time which is only needed in
     * case of {@link Action.ActionType#TIMEFORCED}.
     */
    public static final Long NO_FORCE_TIME = 0L;

    private RepositoryModelConstants() {
        // Utility class.
    }
}