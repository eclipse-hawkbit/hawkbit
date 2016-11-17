/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Optional;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public abstract class AbstractSoftwareModuleTypeUpdateCreate<T> extends AbstractNamedEntityBuilder<T> {
    protected String colour;
    protected String key;
    protected int maxAssignments = 1;

    public T maxAssignments(final int maxAssignments) {
        this.maxAssignments = maxAssignments;
        return (T) this;
    }

    public int getMaxAssignments() {
        return maxAssignments;
    }

    public T colour(final String colour) {
        this.colour = colour;
        return (T) this;
    }

    public Optional<String> getColour() {
        return Optional.ofNullable(colour);
    }

    public T key(final String key) {
        this.key = key;
        return (T) this;
    }

    public Optional<String> getKey() {
        return Optional.ofNullable(key);
    }

}
