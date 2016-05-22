/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantConfiguration;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationKey;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationValidatorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Central tenant configuration management operations of the SP server.
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
@Validated
@Service
public class JpaTenantConfigurationManagement implements EnvironmentAware, TenantConfigurationManagement {

    @Autowired
    private TenantConfigurationRepository tenantConfigurationRepository;

    @Autowired
    private ApplicationContext applicationContext;

    private static final ConfigurableConversionService conversionService = new DefaultConversionService();

    private Environment environment;

    @Override
    @Cacheable(value = "tenantConfiguration", key = "#configurationKey.getKeyName()")
    public <T> TenantConfigurationValue<T> getConfigurationValue(final TenantConfigurationKey configurationKey,
            final Class<T> propertyType) {
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
    public <T> TenantConfigurationValue<T> buildTenantConfigurationValueByKey(
            final TenantConfigurationKey configurationKey, final Class<T> propertyType,
            final TenantConfiguration tenantConfiguration) {
        if (tenantConfiguration != null) {
            return TenantConfigurationValue.<T> builder().global(false).createdBy(tenantConfiguration.getCreatedBy())
                    .createdAt(tenantConfiguration.getCreatedAt())
                    .lastModifiedAt(tenantConfiguration.getLastModifiedAt())
                    .lastModifiedBy(tenantConfiguration.getLastModifiedBy())
                    .value(conversionService.convert(tenantConfiguration.getValue(), propertyType)).build();

        } else if (configurationKey.getDefaultKeyName() != null) {

            return TenantConfigurationValue.<T> builder().global(true).createdBy(null).createdAt(null)
                    .lastModifiedAt(null).lastModifiedBy(null)
                    .value(getGlobalConfigurationValue(configurationKey, propertyType)).build();
        }
        return null;
    }

    @Override
    public TenantConfigurationValue<?> getConfigurationValue(final TenantConfigurationKey configurationKey) {
        return getConfigurationValue(configurationKey, configurationKey.getDataType());
    }

    @Override
    public <T> T getGlobalConfigurationValue(final TenantConfigurationKey configurationKey,
            final Class<T> propertyType) {

        if (!configurationKey.getDataType().isAssignableFrom(propertyType)) {
            throw new TenantConfigurationValidatorException(
                    String.format("Cannot parse the database value of type %s into the type %s.",
                            configurationKey.getDataType(), propertyType));
        }

        final T valueInProperties = environment.getProperty(configurationKey.getDefaultKeyName(), propertyType);

        if (valueInProperties == null) {
            return conversionService.convert(configurationKey.getDefaultValue(), propertyType);
        }

        return valueInProperties;
    }

    @Override
    @CacheEvict(value = "tenantConfiguration", key = "#configurationKey.getKeyName()")
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public <T> TenantConfigurationValue<T> addOrUpdateConfiguration(final TenantConfigurationKey configurationKey,
            final T value) {

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

        final Class<T> clazzT = (Class<T>) value.getClass();

        return TenantConfigurationValue.<T> builder().global(false).createdBy(updatedTenantConfiguration.getCreatedBy())
                .createdAt(updatedTenantConfiguration.getCreatedAt())
                .lastModifiedAt(updatedTenantConfiguration.getLastModifiedAt())
                .lastModifiedBy(updatedTenantConfiguration.getLastModifiedBy())
                .value(conversionService.convert(updatedTenantConfiguration.getValue(), clazzT)).build();
    }

    @Override
    @CacheEvict(value = "tenantConfiguration", key = "#configurationKey.getKeyName()")
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public void deleteConfiguration(final TenantConfigurationKey configurationKey) {
        tenantConfigurationRepository.deleteByKey(configurationKey.getKeyName());
    }

    @Override
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }
}
