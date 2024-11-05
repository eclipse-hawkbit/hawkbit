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
public abstract class AbstractMetadataUpdateCreate<T> {

    @ValidString
    protected String key;

    @ValidString
    protected String value;

    public T key(final String key) {
        this.key = StringUtils.trimWhitespace(key);
        return (T) this;
    }

    public String getKey() {
        return key;
    }

    public T value(final String value) {
        this.value = StringUtils.trimWhitespace(value);
        return (T) this;
    }

    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }

}
