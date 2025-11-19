/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.TenantConfigurationValidatorException;
import org.eclipse.hawkbit.repository.model.PollStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for tenant configurations.
 */
public interface TenantConfigurationManagement extends PermissionSupport {

    @Override
    default String permissionGroup() {
        return SpPermission.TENANT_CONFIGURATION;
    }

    /**
     * Adds or updates a specific configuration for a specific tenant.
     *
     * @param keyName the key of the configuration
     * @param value the configuration value which will be written into the database.
     * @return the configuration value which was just written into the database.
     * @throws TenantConfigurationValidatorException if the {@code propertyType} and the value in general does not match the expected type and
     *         format defined by the Key
     * @throws ConversionFailedException if the property cannot be converted to the given
     */
    @PreAuthorize(value = SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    <T extends Serializable> TenantConfigurationValue<T> addOrUpdateConfiguration(String keyName, T value);

    /**
     * Adds or updates a specific configuration for a specific tenant.
     *
     * @param configurations map containing the key - value of the configuration
     * @return map of all configuration values which were written into the database.
     * @throws TenantConfigurationValidatorException if the {@code propertyType} and the value in general does not
     *         match the expected type and format defined by the Key
     * @throws ConversionFailedException if the property cannot be converted to the given
     */
    @PreAuthorize(value = SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    <T extends Serializable> Map<String, TenantConfigurationValue<T>> addOrUpdateConfiguration(Map<String, T> configurations);

    /**
     * Retrieves a configuration value from the e.g. tenant overwritten configuration values or in case the tenant does not a have a specific
     * configuration the global default value hold in the {@link Environment}.
     *
     * @param keyName the key of the configuration
     * @return the converted configuration value either from the tenant specific configuration stored or from the fallback default values or
     *         {@code null} in case key has not been configured and not default value exists
     * @throws TenantConfigurationValidatorException if the {@code propertyType} and the value in general does not
     *         match the expected type and format defined by the Key
     * @throws ConversionFailedException if the property cannot be converted to the given {@code propertyType}
     */
    @PreAuthorize(value = SpringEvalExpressions.HAS_READ_REPOSITORY)
    <T extends Serializable> TenantConfigurationValue<T> getConfigurationValue(String keyName);

    /**
     * Retrieves a configuration value from the e.g. tenant overwritten configuration values or in case the tenant does not a have a specific
     * configuration the global default value hold in the {@link Environment}.
     *
     * @param <T> the type of the configuration value
     * @param keyName the key of the configuration
     * @param propertyType the type of the configuration value, e.g. {@code String.class}, {@code Integer.class}, etc
     * @return the converted configuration value either from the tenant specific configuration stored or from the fallback default values or
     *         {@code null} in case key has not been configured and not default value exists
     * @throws TenantConfigurationValidatorException if the {@code propertyType} and the value in general does not
     *         match the expected type and format defined by the Key
     * @throws ConversionFailedException if the property cannot be converted to the given {@code propertyType}
     */
    @PreAuthorize(value = SpringEvalExpressions.HAS_READ_REPOSITORY)
    <T extends Serializable> TenantConfigurationValue<T> getConfigurationValue(String keyName, Class<T> propertyType);

    /**
     * Deletes a specific configuration for the current tenant. Does nothing in case there is no tenant specific configuration value.
     *
     * @param configurationKey the configuration key to be deleted
     */
    @PreAuthorize(value = SpringEvalExpressions.HAS_DELETE_REPOSITORY)
    void deleteConfiguration(String configurationKey);

    @PreAuthorize(value = "hasAuthority('READ_" + SpPermission.TARGET + "')")
    Function<Target, PollStatus> pollStatusResolver();
}