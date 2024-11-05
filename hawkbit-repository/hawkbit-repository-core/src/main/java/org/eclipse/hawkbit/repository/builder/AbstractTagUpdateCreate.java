/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Optional;

import org.eclipse.hawkbit.repository.ValidString;
import org.springframework.util.StringUtils;

/**
 * Create and update builder DTO.
 *
 * @param <T> update or create builder interface
 */
public class AbstractTagUpdateCreate<T> extends AbstractNamedEntityBuilder<T> {

    @ValidString
    protected String colour;

    public T colour(final String colour) {
        this.colour = StringUtils.trimWhitespace(colour);
        return (T) this;
    }

    public Optional<String> getColour() {
        return Optional.ofNullable(colour);
    }

}
