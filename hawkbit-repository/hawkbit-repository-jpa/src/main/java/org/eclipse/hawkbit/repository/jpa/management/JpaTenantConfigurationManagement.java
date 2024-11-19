/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.BATCH_ASSIGNMENTS_ENABLED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.TenantConfigurationValidatorException;
import org.eclipse.hawkbit.repository.exception.TenantConfigurationValueChangeNotAllowedException;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantConfiguration;
import org.eclipse.hawkbit.repository.jpa.repository.TenantConfigurationRepository;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.repository.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
@Slf4j
@Transactional(readOnly = true)
@Validated
public class JpaTenantConfigurationManagement implements TenantConfigurationManagement {

    private static final ConfigurableConversionService conversionService = new DefaultConversionService();
    @Autowired
    private TenantConfigurationRepository tenantConfigurationRepository;
    @Autowired
    private TenantConfigurationProperties tenantConfigurationProperties;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private AfterTransactionCommitExecutor afterCommitExecutor;

    @Override
    @CacheEvict(value = "tenantConfiguration", key = "#configurationKeyName")
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public <T extends Serializable> TenantConfigurationValue<T> addOrUpdateConfiguration(
            final String configurationKeyName, final T value) {
        return addOrUpdateConfiguration0(Collections.singletonMap(configurationKeyName, value)).values().iterator().next();
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public <T extends Serializable> Map<String, TenantConfigurationValue<T>> addOrUpdateConfiguration(Map<String, T> configurations) {
        // Register a callback to be invoked after the transaction is committed - for cache eviction
        afterCommitExecutor.afterCommit(() -> {
            Cache cache = cacheManager.getCache("tenantConfiguration");
            if (cache != null) {
                configurations.keySet().forEach(cache::evict);
            }
        });

        return addOrUpdateConfiguration0(configurations);
    }

    @Override
    @CacheEvict(value = "tenantConfiguration", key = "#configurationKeyName")
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteConfiguration(final String configurationKeyName) {
        tenantConfigurationRepository.deleteByKey(configurationKeyName);
    }

    @Override
    @Cacheable(value = "tenantConfiguration", key = "#configurationKeyName")
    public <T extends Serializable> TenantConfigurationValue<T> getConfigurationValue(final String configurationKeyName) {
        checkAccess(configurationKeyName);

        final TenantConfigurationKey configurationKey = tenantConfigurationProperties.fromKeyName(configurationKeyName);

        return getConfigurationValue(configurationKeyName, (Class<T>) configurationKey.getDataType());
    }

    @Override
    @Cacheable(value = "tenantConfiguration", key = "#configurationKeyName")
    public <T extends Serializable> TenantConfigurationValue<T> getConfigurationValue(
            final String configurationKeyName, final Class<T> propertyType) {
        checkAccess(configurationKeyName);

        final TenantConfigurationKey configurationKey = tenantConfigurationProperties.fromKeyName(configurationKeyName);

        validateTenantConfigurationDataType(configurationKey, propertyType);

        final TenantConfiguration tenantConfiguration = tenantConfigurationRepository.findByKey(configurationKey.getKeyName());

        return buildTenantConfigurationValueByKey(configurationKey, propertyType, tenantConfiguration);
    }

    @Override
    public <T> T getGlobalConfigurationValue(final String configurationKeyName, final Class<T> propertyType) {
        checkAccess(configurationKeyName);

        final TenantConfigurationKey key = tenantConfigurationProperties.fromKeyName(configurationKeyName);

        if (!key.getDataType().isAssignableFrom(propertyType)) {
            throw new TenantConfigurationValidatorException(String.format(
                    "Cannot parse the database value of type %s into the type %s.", key.getDataType(), propertyType));
        }

        return conversionService.convert(key.getDefaultValue(), propertyType);
    }

    /**
     * Validates the data type of the tenant configuration. If it is possible to
     * cast to the given data type.
     *
     * @param configurationKey the key
     * @param propertyType the class
     */
    private static <T> void validateTenantConfigurationDataType(final TenantConfigurationKey configurationKey,
            final Class<T> propertyType) {

        if (!configurationKey.getDataType().isAssignableFrom(propertyType)) {
            throw new TenantConfigurationValidatorException(
                    String.format("Cannot parse the database value of type %s into the type %s.",
                            configurationKey.getDataType(), propertyType));
        }
    }

    private void checkAccess(final String configurationKeyName) {
        if (TenantConfigurationProperties.TenantConfigurationKey.AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY
                .equalsIgnoreCase(configurationKeyName)) {
            final SystemSecurityContext systemSecurityContext =
                    SystemSecurityContextHolder.getInstance().getSystemSecurityContext();
            if (!systemSecurityContext.isCurrentThreadSystemCode() &&
                    !systemSecurityContext.hasPermission(SpPermission.READ_GATEWAY_SEC_TOKEN)) {
                throw new InsufficientPermissionException(
                        "Can't read gateway security token! " + SpPermission.READ_GATEWAY_SEC_TOKEN + " is required!");
            }
        }
    }

    private <T extends Serializable> Map<String, TenantConfigurationValue<T>> addOrUpdateConfiguration0(Map<String, T> configurations) {
        List<JpaTenantConfiguration> configurationList = new ArrayList<>();
        configurations.forEach((configurationKeyName, value) -> {
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

            assertValueChangeIsAllowed(configurationKeyName, tenantConfiguration);
            configurationList.add(tenantConfiguration);
        });

        List<JpaTenantConfiguration> jpaTenantConfigurations = tenantConfigurationRepository
                .saveAll(configurationList);

        return jpaTenantConfigurations.stream().collect(Collectors.toMap(
                JpaTenantConfiguration::getKey,
                updatedTenantConfiguration -> {

                    @SuppressWarnings("unchecked") final Class<T> clazzT = (Class<T>) configurations.get(updatedTenantConfiguration.getKey())
                            .getClass();
                    return TenantConfigurationValue.<T> builder().global(false)
                            .createdBy(updatedTenantConfiguration.getCreatedBy())
                            .createdAt(updatedTenantConfiguration.getCreatedAt())
                            .lastModifiedAt(updatedTenantConfiguration.getLastModifiedAt())
                            .lastModifiedBy(updatedTenantConfiguration.getLastModifiedBy())
                            .value(conversionService.convert(updatedTenantConfiguration.getValue(), clazzT))
                            .build();
                }));
    }

    private <T extends Serializable> TenantConfigurationValue<T> buildTenantConfigurationValueByKey(
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

    /**
     * Asserts that the requested configuration value change is allowed. Throws
     * a {@link TenantConfigurationValueChangeNotAllowedException} otherwise.
     *
     * @param key The configuration key.
     * @param valueChange The configuration to be validated.
     * @throws TenantConfigurationValueChangeNotAllowedException if the requested configuration change is not allowed.
     */
    private void assertValueChangeIsAllowed(final String key, final JpaTenantConfiguration valueChange) {
        assertMultiAssignmentsValueChange(key, valueChange);
        assertAutoCloseValueChange(key, valueChange);
        assertBatchAssignmentValueChange(key, valueChange);
    }

    @SuppressWarnings("squid:S1172")
    private void assertAutoCloseValueChange(final String key, final JpaTenantConfiguration valueChange) {
        if (REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED.equals(key)
                && getConfigurationValue(MULTI_ASSIGNMENTS_ENABLED, Boolean.class).getValue()) {
            log.debug(
                    "The property '{}' must not be changed because the Multi-Assignments feature is currently enabled.",
                    key);
            throw new TenantConfigurationValueChangeNotAllowedException();
        }
    }

    private void assertMultiAssignmentsValueChange(final String key, final JpaTenantConfiguration valueChange) {
        if (MULTI_ASSIGNMENTS_ENABLED.equals(key) && !Boolean.parseBoolean(valueChange.getValue())) {
            log.debug("The Multi-Assignments '{}' feature cannot be disabled.", key);
            throw new TenantConfigurationValueChangeNotAllowedException();
        }
        if (MULTI_ASSIGNMENTS_ENABLED.equals(key) && Boolean.parseBoolean(valueChange.getValue())) {
            JpaTenantConfiguration batchConfig = tenantConfigurationRepository.findByKey(BATCH_ASSIGNMENTS_ENABLED);
            if (batchConfig != null && Boolean.parseBoolean(batchConfig.getValue())) {
                log.debug("The Multi-Assignments '{}' feature cannot be enabled as it contradicts with " +
                        "The Batch-Assignments feature, which is already enabled .", key);
                throw new TenantConfigurationValueChangeNotAllowedException();
            }
        }
    }

    private void assertBatchAssignmentValueChange(final String key, final JpaTenantConfiguration valueChange) {
        if (BATCH_ASSIGNMENTS_ENABLED.equals(key) && Boolean.parseBoolean(valueChange.getValue())) {
            JpaTenantConfiguration multiConfig = tenantConfigurationRepository.findByKey(MULTI_ASSIGNMENTS_ENABLED);
            if (multiConfig != null && Boolean.parseBoolean(multiConfig.getValue())) {
                log.debug("The Batch-Assignments '{}' feature cannot be enabled as it contradicts with " +
                        "The Multi-Assignments feature, which is already enabled .", key);
                throw new TenantConfigurationValueChangeNotAllowedException();
            }
        }
    }
}
