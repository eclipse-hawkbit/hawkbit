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

import static org.eclipse.hawkbit.im.authentication.SpPermission.READ_GATEWAY_SECURITY_TOKEN;
import static org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor.afterCommit;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.BATCH_ASSIGNMENTS_ENABLED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.MULTI_ASSIGNMENTS_ENABLED;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.POLLING_TIME;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.event.remote.TenantConfigurationDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RemoteEntityEvent;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.TenantConfigurationValidatorException;
import org.eclipse.hawkbit.repository.exception.TenantConfigurationValueChangeNotAllowedException;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantConfiguration;
import org.eclipse.hawkbit.repository.jpa.ql.QLSupport;
import org.eclipse.hawkbit.repository.jpa.repository.TenantConfigurationRepository;
import org.eclipse.hawkbit.repository.model.PollStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.repository.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.PollingTime;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;

/**
 * Central tenant configuration management operations of the SP server.
 */
@Slf4j
@Transactional(readOnly = true)
@Validated
@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "tenant-configuration-management" }, matchIfMissing = true)
public class JpaTenantConfigurationManagement implements TenantConfigurationManagement {

    private static final String CACHE_TENANT_CONFIGURATION_NAME = "tenantConfiguration";
    private static final ConfigurableConversionService CONVERSION_SERVICE = new DefaultConversionService();

    private final TenantConfigurationRepository tenantConfigurationRepository;
    private final TenantConfigurationProperties tenantConfigurationProperties;
    private final ApplicationContext applicationContext;

    public JpaTenantConfigurationManagement(
            final TenantConfigurationRepository tenantConfigurationRepository,
            final TenantConfigurationProperties tenantConfigurationProperties,
            final ApplicationContext applicationContext) {
        this.tenantConfigurationRepository = tenantConfigurationRepository;
        this.tenantConfigurationProperties = tenantConfigurationProperties;
        this.applicationContext = applicationContext;
    }

    @Override
    @CacheEvict(value = CACHE_TENANT_CONFIGURATION_NAME, key = "#configurationKeyName")
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public <T extends Serializable> TenantConfigurationValue<T> addOrUpdateConfiguration(final String configurationKeyName, final T value) {
        return addOrUpdateConfiguration0(Map.of(configurationKeyName, value)).values().iterator().next();
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public <T extends Serializable> Map<String, TenantConfigurationValue<T>> addOrUpdateConfiguration(final Map<String, T> configurations) {
        // Register a callback to be invoked after the transaction is committed - for cache eviction
        afterCommit(() -> {
            final Cache cache = TenantAwareCacheManager.getInstance().getCache(CACHE_TENANT_CONFIGURATION_NAME);
            if (cache != null) {
                configurations.keySet().forEach(cache::evict);
            }
        });

        return addOrUpdateConfiguration0(configurations);
    }

    @Override
    @CacheEvict(value = CACHE_TENANT_CONFIGURATION_NAME, key = "#configurationKeyName")
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteConfiguration(final String configurationKeyName) {
        tenantConfigurationRepository.deleteByKey(configurationKeyName);
    }

    @Override
// TODO - check if cache works
//    @Cacheable(value = CACHE_TENANT_CONFIGURATION_NAME, key = "#configurationKeyName")
    public <T extends Serializable> TenantConfigurationValue<T> getConfigurationValue(final String configurationKeyName) {
        checkAccess(configurationKeyName);

        final TenantConfigurationKey configurationKey = tenantConfigurationProperties.fromKeyName(configurationKeyName);

        return getConfigurationValue(configurationKeyName, (Class<T>) configurationKey.getDataType());
    }

    @Override
    @Cacheable(value = CACHE_TENANT_CONFIGURATION_NAME, key = "#configurationKeyName")
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
            throw new TenantConfigurationValidatorException(
                    String.format("Cannot parse the database value of type %s into the type %s.", key.getDataType(), propertyType));
        }

        return CONVERSION_SERVICE.convert(key.getDefaultValue(), propertyType);
    }

    /**
     * Ensures that cache eviction takes place in microservice mode in case of deletions.
     *
     * @param event The event indicating that a configuration value has been deleted.
     */
    @EventListener
    public void onTenantConfigurationDeletedEvent(final TenantConfigurationDeletedEvent event) {
        evictCacheEntryByKeyIfPresent(event.getConfigKey());
    }

    /**
     * Ensures that cache eviction takes place in microservice mode in case of creation or update events.
     *
     * @param event The event indicating that a configuration value has been created or updated.
     */
    @EventListener
    public void onTenantConfigurationRemoteEntityEvent(final RemoteEntityEvent<TenantConfiguration> event) {
        event.getEntity().ifPresent(tenantConfiguration -> evictCacheEntryByKeyIfPresent(tenantConfiguration.getKey()));
    }

    @Override
    public Function<Target, PollStatus> pollStatusResolver() {
        final PollingTime pollingTime = new PollingTime(
                getConfigurationValue(TenantConfigurationKey.POLLING_TIME, String.class).getValue());
        final Duration pollingOverdueTime = DurationHelper.fromString(
                getConfigurationValue(TenantConfigurationKey.POLLING_OVERDUE_TIME, String.class).getValue());
        return target -> {
            final Long lastTargetQuery = target.getLastTargetQuery();
            if (lastTargetQuery == null) {
                return null;
            }

            if (!ObjectUtils.isEmpty(pollingTime.getOverrides()) && target instanceof JpaTarget jpaTarget) {
                for (final PollingTime.Override override : pollingTime.getOverrides()) {
                    try {
                        if (QLSupport.getInstance().entityMatcher(override.qlStr(), TargetFields.class).match(jpaTarget)) {
                            return pollStatus(lastTargetQuery, override.pollingInterval(), pollingOverdueTime);
                        }
                    } catch (final Exception e) {
                        log.warn("Error while evaluating polling override for target {}: {}", jpaTarget.getId(), e.getMessage());
                    }
                }
            }
            // returns default - no overrides or not applicable for the target
            return pollStatus(lastTargetQuery, pollingTime.getPollingInterval(), pollingOverdueTime);
        };
    }

    private static PollStatus pollStatus(
            final long lastTargetQuery,
            final PollingTime.PollingInterval pollingInterval, final Duration pollingOverdueTime) {
        final LocalDateTime currentDate = LocalDateTime.now();
        final LocalDateTime lastPollDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastTargetQuery), ZoneId.systemDefault());
        LocalDateTime nextPollDate = lastPollDate.plus(pollingInterval.getInterval());
        if (pollingInterval.getDeviationPercent() > 0) {
            nextPollDate = nextPollDate.plus(
                    pollingInterval.getInterval().toMillis() * pollingInterval.getDeviationPercent() / 100,
                    ChronoUnit.MILLIS);
        }
        final LocalDateTime overdueDate = nextPollDate.plus(pollingOverdueTime);
        return new PollStatus(lastPollDate, nextPollDate, overdueDate, currentDate);
    }

    /**
     * Validates the data type of the tenant configuration. If it is possible to cast to the given data type.
     */
    private static <T> void validateTenantConfigurationDataType(final TenantConfigurationKey configurationKey, final Class<T> propertyType) {
        if (!configurationKey.getDataType().isAssignableFrom(propertyType)) {
            throw new TenantConfigurationValidatorException(
                    String.format(
                            "Cannot parse the database value of type %s into the type %s.",
                            configurationKey.getDataType(), propertyType));
        }
    }

    private void checkAccess(final String configurationKeyName) {
        if (AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY.equalsIgnoreCase(configurationKeyName)) {
            final SystemSecurityContext systemSecurityContext = SystemSecurityContextHolder.getInstance().getSystemSecurityContext();
            if (!SystemSecurityContext.isCurrentThreadSystemCode() && !systemSecurityContext.hasPermission(READ_GATEWAY_SECURITY_TOKEN)) {
                throw new InsufficientPermissionException(
                        "Can't read gateway security token! " + READ_GATEWAY_SECURITY_TOKEN + " is required!");
            }
        }
    }

    private <T extends Serializable> Map<String, TenantConfigurationValue<T>> addOrUpdateConfiguration0(final Map<String, T> configurations) {
        final List<JpaTenantConfiguration> configurationList = new ArrayList<>();
        configurations.forEach((configurationKeyName, value) -> {
            final TenantConfigurationKey configurationKey = tenantConfigurationProperties.fromKeyName(configurationKeyName);
            if (!configurationKey.getDataType().isAssignableFrom(value.getClass())) {
                throw new TenantConfigurationValidatorException(String.format(
                        "Cannot parse the value %s of type %s into the type %s defined by the configuration key.", value,
                        value.getClass(), configurationKey.getDataType()));
            }
            configurationKey.validate(value, applicationContext);
            // additional validation for specific configuration keys
            if (POLLING_TIME.equals(configurationKey.getKeyName())) {
                final PollingTime pollingTime = new PollingTime(value.toString());
                if (!ObjectUtils.isEmpty(pollingTime.getOverrides())) {
                    // validate that the QL strings are valid RSQL queries,
                    // nevertheless always when parse them we shall be prepared to catch exceptions if the parsers
                    // has been changed in not backward compatible way
                    pollingTime.getOverrides().forEach(override -> QLSupport.getInstance().entityMatcher(override.qlStr(), TargetFields.class));
                }
            }

            JpaTenantConfiguration tenantConfiguration = tenantConfigurationRepository.findByKey(configurationKey.getKeyName());
            if (tenantConfiguration == null) {
                tenantConfiguration = new JpaTenantConfiguration(configurationKey.getKeyName(), value.toString());
            } else {
                tenantConfiguration.setValue(value.toString());
            }

            assertValueChangeIsAllowed(configurationKeyName, tenantConfiguration);
            configurationList.add(tenantConfiguration);
        });

        final List<JpaTenantConfiguration> jpaTenantConfigurations = tenantConfigurationRepository.saveAll(configurationList);
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
                            .value(CONVERSION_SERVICE.convert(updatedTenantConfiguration.getValue(), clazzT))
                            .build();
                }));
    }

    private <T extends Serializable> TenantConfigurationValue<T> buildTenantConfigurationValueByKey(
            final TenantConfigurationKey configurationKey, final Class<T> propertyType, final TenantConfiguration tenantConfiguration) {
        if (tenantConfiguration != null) {
            return TenantConfigurationValue.<T> builder().global(false).createdBy(tenantConfiguration.getCreatedBy())
                    .createdAt(tenantConfiguration.getCreatedAt())
                    .lastModifiedAt(tenantConfiguration.getLastModifiedAt())
                    .lastModifiedBy(tenantConfiguration.getLastModifiedBy())
                    .value(CONVERSION_SERVICE.convert(tenantConfiguration.getValue(), propertyType)).build();
        } else if (configurationKey.getDefaultValue() != null) {
            return TenantConfigurationValue.<T> builder().global(true).createdBy(null).createdAt(null)
                    .lastModifiedAt(null).lastModifiedBy(null)
                    .value(getGlobalConfigurationValue(configurationKey.getKeyName(), propertyType)).build();
        } else {
            return null;
        }
    }

    /**
     * Asserts that the requested configuration value change is allowed. Throws a {@link TenantConfigurationValueChangeNotAllowedException}
     * otherwise.
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
                && Boolean.TRUE.equals(getConfigurationValue(MULTI_ASSIGNMENTS_ENABLED, Boolean.class).getValue())) {
            log.debug("The property '{}' must not be changed because the Multi-Assignments feature is currently enabled.", key);
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
                log.debug(
                        "The Multi-Assignments '{}' feature cannot be enabled as it contradicts with the Batch-Assignments feature, which is already enabled .",
                        key);
                throw new TenantConfigurationValueChangeNotAllowedException();
            }
        }
    }

    private void assertBatchAssignmentValueChange(final String key, final JpaTenantConfiguration valueChange) {
        if (BATCH_ASSIGNMENTS_ENABLED.equals(key) && Boolean.parseBoolean(valueChange.getValue())) {
            JpaTenantConfiguration multiConfig = tenantConfigurationRepository.findByKey(MULTI_ASSIGNMENTS_ENABLED);
            if (multiConfig != null && Boolean.parseBoolean(multiConfig.getValue())) {
                log.debug(
                        "The Batch-Assignments '{}' feature cannot be enabled as it contradicts with the Multi-Assignments feature, which is already enabled .",
                        key);
                throw new TenantConfigurationValueChangeNotAllowedException();
            }
        }
    }

    private void evictCacheEntryByKeyIfPresent(final String key) {
        final Cache cache = TenantAwareCacheManager.getInstance().getCache(CACHE_TENANT_CONFIGURATION_NAME);
        if (cache != null) {
            cache.evictIfPresent(key);
        }
    }
}
