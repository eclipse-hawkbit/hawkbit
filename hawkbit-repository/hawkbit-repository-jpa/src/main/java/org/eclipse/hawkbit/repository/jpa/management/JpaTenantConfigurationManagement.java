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

import static org.eclipse.hawkbit.auth.SpPermission.READ_GATEWAY_SECURITY_TOKEN;
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
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.context.SystemSecurityContext;
import org.eclipse.hawkbit.ql.jpa.QLSupport;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.TenantConfigurationValidatorException;
import org.eclipse.hawkbit.repository.exception.TenantConfigurationValueChangeNotAllowedException;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantConfiguration;
import org.eclipse.hawkbit.repository.jpa.repository.TenantConfigurationRepository;
import org.eclipse.hawkbit.repository.model.PollStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.eclipse.hawkbit.repository.qfields.TargetFields;
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager;
import org.eclipse.hawkbit.tenancy.configuration.DurationHelper;
import org.eclipse.hawkbit.tenancy.configuration.PollingTime;
import org.eclipse.hawkbit.tenancy.configuration.PollingTime.PollingInterval;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionException;
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

    private static final String CACHE_TENANT_CONFIGURATION_NAME = JpaTenantConfiguration.class.getSimpleName();
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
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public <T extends Serializable> TenantConfigurationValue<T> addOrUpdateConfiguration(final String keyName, final T value) {
        return addOrUpdateConfiguration0(Map.of(keyName, value)).values().iterator().next();
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public <T extends Serializable> Map<String, TenantConfigurationValue<T>> addOrUpdateConfiguration(final Map<String, T> configurations) {
        return addOrUpdateConfiguration0(configurations);
    }

    @Override
    public <T extends Serializable> TenantConfigurationValue<T> getConfigurationValue(final String keyName) {
        return getConfigurationValue0(keyName, null);
    }

    @Override
    public <T extends Serializable> TenantConfigurationValue<T> getConfigurationValue(final String keyName, final Class<T> propertyType) {
        return getConfigurationValue0(keyName, propertyType);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteConfiguration(final String keyName) {
        tenantConfigurationRepository.deleteByKey(keyName);
    }

    @Override
    @SuppressWarnings("java:S3776") // java:S3776 - not really too complex
    public Function<Target, PollStatus> pollStatusResolver() {
        final PollingTime pollingTime = new PollingTime(
                Objects.requireNonNull(getConfigurationValue0(TenantConfigurationKey.POLLING_TIME, String.class),
                                "Polling time shall always be non-null")
                        .getValue());
        final Duration pollingOverdueTime = DurationHelper.fromString(
                Objects.requireNonNull(getConfigurationValue0(TenantConfigurationKey.POLLING_OVERDUE_TIME, String.class),
                                "Polling overdue time shall always be non-null")
                        .getValue());
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

    /**
     * Validates the data type of the tenant configuration. If it is possible to cast to the given data type.
     */
    private static <T> void validateTenantConfigurationDataType(final TenantConfigurationKey configurationKey, final Class<T> propertyType) {
        if (!configurationKey.getDataType().isAssignableFrom(propertyType)) {
            throw new TenantConfigurationValidatorException(String.format(
                    "Cannot parse the database value of type %s into the type %s.", configurationKey.getDataType(), propertyType));
        }
    }

    private void checkAccess(final String keyName) {
        if (AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY.equalsIgnoreCase(keyName)) {
            if (!SystemSecurityContext.isCurrentThreadSystemCode() && !SpPermission.hasPermission(READ_GATEWAY_SECURITY_TOKEN)) {
                throw new InsufficientPermissionException(
                        "Can't read gateway security token! " + READ_GATEWAY_SECURITY_TOKEN + " is required!");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends Serializable> Map<String, TenantConfigurationValue<T>> addOrUpdateConfiguration0(final Map<String, T> configurations) {
        final List<JpaTenantConfiguration> configurationList = new ArrayList<>();
        configurations.forEach((keyName, value) -> {
            final TenantConfigurationKey configurationKey = tenantConfigurationProperties.fromKeyName(keyName);

            final Class<?> targetType = configurationKey.getDataType();
            Object convertedValue = getConvertedValue(value, targetType);
            validateConfigurationValue(value, configurationKey, convertedValue);

            JpaTenantConfiguration tenantConfiguration = tenantConfigurationRepository.findByKey(configurationKey.getKeyName());
            if (tenantConfiguration == null) {
                tenantConfiguration = new JpaTenantConfiguration(configurationKey.getKeyName(), convertedValue.toString());
            } else {
                tenantConfiguration.setValue(convertedValue.toString());
            }

            assertValueChangeIsAllowed(keyName, tenantConfiguration);
            configurationList.add(tenantConfiguration);
        });
        return tenantConfigurationRepository.saveAll(configurationList).
                stream().
                collect(Collectors.toMap(
                        JpaTenantConfiguration::getKey,
                        updatedTenantConfiguration -> TenantConfigurationValue.<T> builder()
                                .global(false)
                                .createdBy(updatedTenantConfiguration.getCreatedBy())
                                .createdAt(updatedTenantConfiguration.getCreatedAt())
                                .lastModifiedAt(updatedTenantConfiguration.getLastModifiedAt())
                                .lastModifiedBy(updatedTenantConfiguration.getLastModifiedBy())
                                .value(CONVERSION_SERVICE.convert(
                                        updatedTenantConfiguration.getValue(),
                                        (Class<T>) configurations.get(updatedTenantConfiguration.getKey()).getClass()))
                                .build()));
    }

    private <T extends Serializable> void validateConfigurationValue(final T value, final TenantConfigurationKey configurationKey,
            final Object convertedValue) {
        configurationKey.validate(convertedValue, applicationContext);
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
    }

    private static <T extends Serializable> Object getConvertedValue(final T value, final Class<?> targetType) {
        Object convertedValue = value;
        if (!targetType.isAssignableFrom(value.getClass())) {
            try {
                // if not assignable and it is a number - try conversion
                // for example tries to assign Integer to Long
                if (value instanceof Number number && Number.class.isAssignableFrom(targetType)) {
                    log.debug("Type {} not assignable from {} . Will try conversion.", targetType, value.getClass());
                    convertedValue = CONVERSION_SERVICE.convert(number, targetType);
                    if (convertedValue == null) {
                        throw new IllegalArgumentException(
                                String.format("Failed to convert %s. Convertor returned null as a result", value));
                    }
                } else {
                    throw new IllegalArgumentException(
                            String.format("Value %s is not a Number but %s and cannot perform conversion converted.", value, value.getClass()));
                }
            } catch (final ConversionException | IllegalArgumentException ex) {
                throw new TenantConfigurationValidatorException(String.format(
                        "Cannot convert the value %s of type %s to the type %s defined by the configuration key.",
                        value, value.getClass(), targetType));
            }
        }
        return convertedValue;
    }

    @SuppressWarnings("unchecked")
    private <T extends Serializable> TenantConfigurationValue<T> getConfigurationValue0(final String keyName, final Class<T> propertyType) {
        checkAccess(keyName);

        final TenantConfigurationKey key = tenantConfigurationProperties.fromKeyName(keyName);
        if (propertyType != null) {
            validateTenantConfigurationDataType(key, propertyType);
        }

        final TenantConfiguration tenantConfiguration = TenantAwareCacheManager.getInstance().getCache(CACHE_TENANT_CONFIGURATION_NAME)
                .get(key.getKeyName(), () -> tenantConfigurationRepository.findByKey(key.getKeyName()));
        return buildTenantConfigurationValueByKey(key, propertyType == null ? (Class<T>) key.getDataType() : propertyType, tenantConfiguration);
    }

    private <T extends Serializable> TenantConfigurationValue<T> buildTenantConfigurationValueByKey(
            final TenantConfigurationKey configurationKey, final Class<T> propertyType, final TenantConfiguration tenantConfiguration) {
        if (tenantConfiguration != null) {
            return TenantConfigurationValue.<T> builder().global(false)
                    .createdBy(tenantConfiguration.getCreatedBy())
                    .createdAt(tenantConfiguration.getCreatedAt())
                    .lastModifiedAt(tenantConfiguration.getLastModifiedAt())
                    .lastModifiedBy(tenantConfiguration.getLastModifiedBy())
                    .value(CONVERSION_SERVICE.convert(tenantConfiguration.getValue(), propertyType)).build();
        } else if (configurationKey.getDefaultValue() != null) {
            return TenantConfigurationValue.<T> builder().global(true)
                    .createdBy(null)
                    .createdAt(null)
                    .lastModifiedAt(null)
                    .lastModifiedBy(null)
                    .value(getGlobalConfigurationValue0(configurationKey.getKeyName(), propertyType)).build();
        } else {
            return null;
        }
    }

    private <T> T getGlobalConfigurationValue0(final String keyName, final Class<T> propertyType) {
        checkAccess(keyName);

        final TenantConfigurationKey key = tenantConfigurationProperties.fromKeyName(keyName);
        if (!key.getDataType().isAssignableFrom(propertyType)) {
            throw new TenantConfigurationValidatorException(
                    String.format("Cannot parse the database value of type %s into the type %s.", key.getDataType(), propertyType));
        }

        return CONVERSION_SERVICE.convert(key.getDefaultValue(), propertyType);
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
        assertAutoCloseValueChange(key);
        assertBatchAssignmentValueChange(key, valueChange);
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

    private void assertAutoCloseValueChange(final String key) {
        if (REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED.equals(key)
                && Boolean.TRUE.equals(getConfigurationValue0(MULTI_ASSIGNMENTS_ENABLED, Boolean.class).getValue())) {
            log.debug("The property '{}' must not be changed because the Multi-Assignments feature is currently enabled.", key);
            throw new TenantConfigurationValueChangeNotAllowedException();
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

    private static PollStatus pollStatus(final long lastTargetQuery, final PollingInterval pollingInterval, final Duration pollingOverdueTime) {
        final LocalDateTime currentDate = LocalDateTime.now();
        final LocalDateTime lastPollDate = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastTargetQuery), ZoneId.systemDefault());
        LocalDateTime nextPollDate = lastPollDate.plus(pollingInterval.getInterval());
        if (pollingInterval.getDeviationPercent() > 0) {
            nextPollDate = nextPollDate.plus(
                    pollingInterval.getInterval().toMillis() * pollingInterval.getDeviationPercent() / 100, ChronoUnit.MILLIS);
        }
        final LocalDateTime overdueDate = nextPollDate.plus(pollingOverdueTime);
        return new PollStatus(lastPollDate, nextPollDate, overdueDate, currentDate);
    }
}