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
public abstract class AbstractSoftwareModuleUpdateCreate<T> extends AbstractNamedEntityBuilder<T> {
    protected String version;
    protected String vendor;
    protected String type;

    public T type(final String type) {
        this.type = type;
        return (T) this;
    }

    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    public T vendor(final String vendor) {
        this.vendor = vendor;
        return (T) this;
    }

    public Optional<String> getVendor() {
        return Optional.ofNullable(vendor);
    }

    public T version(final String version) {
        this.version = version;
        return (T) this;
    }

    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }

}
