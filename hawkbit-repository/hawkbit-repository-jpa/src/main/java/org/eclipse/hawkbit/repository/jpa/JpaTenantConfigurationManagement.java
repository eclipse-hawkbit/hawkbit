/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.io.Serializable;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantConfiguration;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationValidatorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Central tenant configuration management operations of the SP server.
 */
@Transactional(readOnly = true)
@Validated
public class JpaTenantConfigurationManagement implements TenantConfigurationManagement {

    @Autowired
    private TenantConfigurationRepository tenantConfigurationRepository;

    @Autowired
    private TenantConfigurationProperties tenantConfigurationProperties;

    @Autowired
    private ApplicationContext applicationContext;

    private static final ConfigurableConversionService conversionService = new DefaultConversionService();

    @Override
    @Cacheable(value = "tenantConfiguration", key = "#configurationKeyName")
    public <T extends Serializable> TenantConfigurationValue<T> getConfigurationValue(final String configurationKeyName,
            final Class<T> propertyType) {

        final TenantConfigurationKey configurationKey = tenantConfigurationProperties.fromKeyName(configurationKeyName);

        validateTenantConfigurationDataType(configurationKey, propertyType);

        final TenantConfiguration tenantConfiguration = tenantConfigurationRepository
                .findByKey(configurationKey.getKeyName());

        return buildTenantConfigurationValueByKey(configurationKey, propertyType, tenantConfiguration);
    }

    /**
     * Validates the data type of the tenant configuration. If it is possible to
     * cast to the given data type.
     * 
     * @param configurationKey
     *            the key
     * @param propertyType
     *            the class
     */
    static <T> void validateTenantConfigurationDataType(final TenantConfigurationKey configurationKey,
            final Class<T> propertyType) {

        if (!configurationKey.getDataType().isAssignableFrom(propertyType)) {
            throw new TenantConfigurationValidatorException(
                    String.format("Cannot parse the database value of type %s into the type %s.",
                            configurationKey.getDataType(), propertyType));
        }
    }

    @Override
    public <T extends Serializable> TenantConfigurationValue<T> buildTenantConfigurationValueByKey(
            final TenantConfigurationKey configurationKey, final Class<T> propertyType,
            final TenantConfiguration tenantConfiguration) {
        if (tenantConfiguration != null) {
            return TenantConfigurationValue.<T> builder().global(false).createdBy(tenantConfiguration.getCreatedBy())
                    .createdAt(tenantConfiguration.getCreatedAt())
                    .lastModifiedAt(tenantConfiguration.getLastModifiedAt())
                    .lastModifiedBy(tenantConfiguration.getLastModifiedBy())
                    .value(conversionService.convert(tenantConfiguration.getValue(), propertyType)).build();

        } else if (configurationKey.getDefaultValue() != null) {

            return TenantConfigurationValue.<T> builder().global(true).createdBy(null).createdAt(null)
                    .lastModifiedAt(null).lastModifiedBy(null)
                    .value(getGlobalConfigurationValue(configurationKey.getKeyName(), propertyType)).build();
        }
        return null;
    }

    @Override
    public <T extends Serializable> TenantConfigurationValue<T> getConfigurationValue(
            final String configurationKeyName) {
        final TenantConfigurationKey configurationKey = tenantConfigurationProperties.fromKeyName(configurationKeyName);

        return getConfigurationValue(configurationKeyName, configurationKey.getDataType());
    }

    @Override
    public <T> T getGlobalConfigurationValue(final String configurationKeyName, final Class<T> propertyType) {

        final TenantConfigurationKey key = tenantConfigurationProperties.fromKeyName(configurationKeyName);

        if (!key.getDataType().isAssignableFrom(propertyType)) {
            throw new TenantConfigurationValidatorException(String.format(
                    "Cannot parse the database value of type %s into the type %s.", key.getDataType(), propertyType));
        }

        return conversionService.convert(key.getDefaultValue(), propertyType);
    }

    @Override
    @CacheEvict(value = "tenantConfiguration", key = "#configurationKeyName")
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public <T extends Serializable> TenantConfigurationValue<T> addOrUpdateConfiguration(
            final String configurationKeyName, final T value) {

        final TenantConfigurationKey configurationKey = tenantConfigurationProperties.fromKeyName(configurationKeyName);

        if (!configurationKey.getDataType().isAssignableFrom(value.getClass())) {
            throw new TenantConfigurationValidatorException(String.format(
                    "Cannot parse the value %s of type %s into the type %s defined by the configuration key.", value,
                    value.getClass(), configurationKey.getDataType()));
        }

        configurationKey.validate(applicationContext, value);

        JpaTenantConfiguration tenantConfiguration = tenantConfigurationRepository
                .findByKey(configurationKey.getKeyName());

        if (tenantConfiguration == null) {
            tenantConfiguration = new JpaTenantConfiguration(configurationKey.getKeyName(), value.toString());
        } else {
            tenantConfiguration.setValue(value.toString());
        }

        final JpaTenantConfiguration updatedTenantConfiguration = tenantConfigurationRepository
                .save(tenantConfiguration);

        @SuppressWarnings("unchecked")
        final Class<T> clazzT = (Class<T>) value.getClass();

        return TenantConfigurationValue.<T> builder().global(false).createdBy(updatedTenantConfiguration.getCreatedBy())
                .createdAt(updatedTenantConfiguration.getCreatedAt())
                .lastModifiedAt(updatedTenantConfiguration.getLastModifiedAt())
                .lastModifiedBy(updatedTenantConfiguration.getLastModifiedBy())
                .value(conversionService.convert(updatedTenantConfiguration.getValue(), clazzT)).build();
    }

    @Override
    @CacheEvict(value = "tenantConfiguration", key = "#configurationKeyName")
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteConfiguration(final String configurationKeyName) {
        tenantConfigurationRepository.deleteByKey(configurationKeyName);
    }
}
