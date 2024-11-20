/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Collection;
import java.util.Optional;

/**
 * Create and update builder DTO.
 *
 * @param <T> update or create builder interface
 */
public abstract class AbstractTargetTypeUpdateCreate<T> extends AbstractTypeUpdateCreate<T> {

    protected Collection<Long> compatible;

    /**
     * @param compatible list of ID
     * @return generic type
     */
    public T compatible(final Collection<Long> compatible) {
        this.compatible = compatible;
        return (T) this;
    }

    /**
     * @return List of ID
     */
    public Optional<Collection<Long>> getCompatible() {
        return Optional.ofNullable(compatible);
    }
}