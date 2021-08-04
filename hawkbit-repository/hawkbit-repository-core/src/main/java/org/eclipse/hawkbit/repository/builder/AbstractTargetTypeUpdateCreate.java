/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.ValidString;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Optional;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public abstract class AbstractTargetTypeUpdateCreate<T> extends AbstractNamedEntityBuilder<T> {
    @ValidString
    protected String colour;

    protected Collection<Long> compatible;

    /**
     * @param compatible
     *          list of ID
     * @return generic type
     */
    public T compatible(final Collection<Long> compatible) {
        this.compatible = compatible;
        return (T) this;
    }

    /**
     * @return  List of ID
     */
    public Optional<Collection<Long>> getCompatible() {
        return Optional.ofNullable(compatible);
    }

    /**
     * @param colour
     *          Colour value
     * @return generic type
     */
    public T colour(final String colour) {
        this.colour = StringUtils.trimWhitespace(colour);
        return (T) this;
    }

    /**
     * @return colour
     */
    public Optional<String> getColour() {
        return Optional.ofNullable(colour);
    }

}
