/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.layout;

/**
 * Interface for master entity aware component
 *
 * @param <T>
 *            Master entity
 */
@FunctionalInterface
public interface MasterEntityAwareComponent<T> {

    /**
     * Master entity changed
     *
     * @param masterEntity
     *            Generic type
     */
    void masterEntityChanged(final T masterEntity);
}
