/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.repository.model;

public interface Statistic {

    /**
     * @return the key of the Statistic entity.
     */
    Object getName();

    /**
     * @return the value of the Statistic entity.
     */
    Object getData();
}
