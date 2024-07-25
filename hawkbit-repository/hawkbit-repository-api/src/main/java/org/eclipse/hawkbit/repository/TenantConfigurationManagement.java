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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.PollStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationValidatorException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for tenant configurations.
 *
 */
public interface TenantConfigurationManagement {

    /**
     * Adds or updates a specific configuration for a specific tenant.
     * 
     * 
     * @param configurationKeyName
     *            the key of the configuration
     * @param value
     *            the configuration value which will be written into the
     *            database.
     * @return the configuration value which was just written into the database.
     * @throws TenantConfigurationValidatorException
     *             if the {@code propertyType} and the value in general does not
     *             match the expected type and format defined by the Key
     * @throws ConversionFailedException
     *             if the property cannot be converted to the given
     */
    @PreAuthorize(value = SpringEvalExpressions.HAS_AUTH_TENANT_CONFIGURATION)
    <T extends Serializable> TenantConfigurationValue<T> addOrUpdateConfiguration(String configurationKeyName, T value);

    /**
     * Adds or updates a specific configuration for a specific tenant.
     *
     *
     * @param configurations
     *            map containing the key - value of the configuration
     * @return map of all configuration values which were written into the database.
     * @throws TenantConfigurationValidatorException
     *             if the {@code propertyType} and the value in general does not
     *             match the expected type and format defined by the Key
     * @throws ConversionFailedException
     *             if the property cannot be converted to the given
     */
    @PreAuthorize(value = SpringEvalExpressions.HAS_AUTH_TENANT_CONFIGURATION)
    <T extends Serializable> Map<String, TenantConfigurationValue<T>>  addOrUpdateConfiguration(Map<String, T> configurations);

    /**
     * Deletes a specific configuration for the current tenant. Does nothing in
     * case there is no tenant specific configuration value.
     *
     * @param configurationKey
     *            the configuration key to be deleted
     */
    @PreAuthorize(value = SpringEvalExpressions.HAS_AUTH_TENANT_CONFIGURATION)
    void deleteConfiguration(String configurationKey);

    /**
     * Retrieves a configuration value from the e.g. tenant overwritten
     * configuration values or in case the tenant does not a have a specific
     * configuration the global default value hold in the {@link Environment}.
     * 
     * @param configurationKeyName
     *            the key of the configuration
     * @return the converted configuration value either from the tenant specific
     *         configuration stored or from the fall back default values or
     *         {@code null} in case key has not been configured and not default
     *         value exists
     * @throws TenantConfigurationValidatorException
     *             if the {@code propertyType} and the value in general does not
     *             match the expected type and format defined by the Key
     * @throws ConversionFailedException
     *             if the property cannot be converted to the given
     *             {@code propertyType}
     */
    @PreAuthorize(value = SpringEvalExpressions.HAS_AUTH_TENANT_CONFIGURATION_READ)
    <T extends Serializable> TenantConfigurationValue<T> getConfigurationValue(String configurationKeyName);

    /**
     * Retrieves a configuration value from the e.g. tenant overwritten
     * configuration values or in case the tenant does not a have a specific
     * configuration the global default value hold in the {@link Environment}.
     * 
     * @param <T>
     *            the type of the configuration value
     * @param configurationKeyName
     *            the key of the configuration
     * @param propertyType
     *            the type of the configuration value, e.g. {@code String.class}
     *            , {@code Integer.class}, etc
     * @return the converted configuration value either from the tenant specific
     *         configuration stored or from the fallback default values or
     *         {@code null} in case key has not been configured and not default
     *         value exists
     * @throws TenantConfigurationValidatorException
     *             if the {@code propertyType} and the value in general does not
     *             match the expected type and format defined by the Key
     * @throws ConversionFailedException
     *             if the property cannot be converted to the given
     *             {@code propertyType}
     */
    @PreAuthorize(value = SpringEvalExpressions.HAS_AUTH_TENANT_CONFIGURATION_READ)
    <T extends Serializable> TenantConfigurationValue<T> getConfigurationValue(String configurationKeyName,
            Class<T> propertyType);

    /**
     * returns the global configuration property either defined in the property
     * file or an default value otherwise.
     * 
     * @param <T>
     *            the type of the configuration value
     * @param configurationKeyName
     *            the key of the configuration
     * @param propertyType
     *            the type of the configuration value, e.g. {@code String.class}
     *            , {@code Integer.class}, etc
     * @return the global configured value
     * @throws TenantConfigurationValidatorException
     *             if the {@code propertyType} and the value in the property
     *             file or the default value does not match the expected type
     *             and format defined by the Key
     * @throws ConversionFailedException
     *             if the property cannot be converted to the given
     *             {@code propertyType}
     */
    @PreAuthorize(value = SpringEvalExpressions.HAS_AUTH_TENANT_CONFIGURATION_READ)
    <T> T getGlobalConfigurationValue(String configurationKeyName, Class<T> propertyType);

    // PreAuthorize for TENANT_CONFIGURATION_READ won't be applied but actually we want just read target
    @PreAuthorize(value = SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    default Function<Target, PollStatus> pollStatusResolver() {
        final Duration pollTime = DurationHelper.formattedStringToDuration(
                getConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL, String.class).getValue());
        final Duration overdueTime = DurationHelper.formattedStringToDuration(
                getConfigurationValue(TenantConfigurationKey.POLLING_OVERDUE_TIME_INTERVAL, String.class)
                        .getValue());
        return target -> {
            final Long lastTargetQuery = target.getLastTargetQuery();
            if (lastTargetQuery == null) {
                return null;
            }
            final LocalDateTime currentDate = LocalDateTime.now();
            final LocalDateTime lastPollDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastTargetQuery),
                    ZoneId.systemDefault());
            final LocalDateTime nextPollDate = lastPollDate.plus(pollTime);
            final LocalDateTime overdueDate = nextPollDate.plus(overdueTime);
            return new PollStatus(lastPollDate, nextPollDate, overdueDate, currentDate);
        };
    }
}
