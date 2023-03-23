/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.event;

import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Optional;

/**
 * Payload event for tenant config changes
 */
public class TenantConfigChangedEventPayload {

    private final String tenant;

    private final String key;

    private final Object value;

    /**
     * Constructor
     * 
     * @param tenant
     *            the change relates to
     * @param key
     *            of the config
     * @param value
     *            of the config
     */
    public TenantConfigChangedEventPayload(@NotEmpty final String tenant, @NotEmpty final String key,
            @NotNull final TenantConfigurationValue<?> value) {
        this.tenant = tenant;
        this.key = key;
        this.value = value.getValue();
    }

    public Object getValue() {
        return value;
    }

    /**
     * Parse value into an given class if possible. If possible, the returned
     * instance of {@link Optional} contains the cast value. Otherwise, it's empty.
     */
    public <T> Optional<T> getValue(final Class<T> fromClass) {
        return fromClass.isInstance(value) ? Optional.of(fromClass.cast(value)) : Optional.empty();
    }

    public String getKey() {
        return key;
    }

    public String getTenant() {
        return tenant;
    }
}
