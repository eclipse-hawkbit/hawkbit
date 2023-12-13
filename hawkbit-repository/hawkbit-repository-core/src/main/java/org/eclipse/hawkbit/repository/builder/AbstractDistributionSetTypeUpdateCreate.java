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

import java.util.Collection;
import java.util.Optional;

import org.eclipse.hawkbit.repository.ValidString;
import org.springframework.util.StringUtils;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public abstract class AbstractDistributionSetTypeUpdateCreate<T> extends AbstractTypeUpdateCreate<T> {

    protected Collection<Long> mandatory;
    protected Collection<Long> optional;

    public T mandatory(final Collection<Long> mandatory) {
        this.mandatory = mandatory;
        return (T) this;
    }

    public T optional(final Collection<Long> optional) {
        this.optional = optional;
        return (T) this;
    }

    public Optional<Collection<Long>> getMandatory() {
        return Optional.ofNullable(mandatory);
    }

    public Optional<Collection<Long>> getOptional() {
        return Optional.ofNullable(optional);
    }
}
