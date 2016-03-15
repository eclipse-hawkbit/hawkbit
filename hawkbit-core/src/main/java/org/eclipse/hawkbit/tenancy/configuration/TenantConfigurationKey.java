/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.tenancy.configuration;

import java.util.Arrays;
import java.util.Optional;

import org.eclipse.hawkbit.ControllerPollProperties;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationBooleanValidator;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationPollingDurationValidator;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationStringValidator;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationValidator;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationValidatorException;
import org.springframework.context.ApplicationContext;

/**
 * An enum which defines the tenant specific configurations which can be
 * configured for each tenant separately. The non overridable properties are
 * configured in {@link ControllerPollProperties} instead.
 *
 */
public enum TenantConfigurationKey {

    /**
     * boolean value {@code true} {@code false}.
     */
    AUTHENTICATION_MODE_HEADER_ENABLED("authentication.header.enabled", "hawkbit.server.ddi.security.authentication.header.enabled", Boolean.class, Boolean.FALSE.toString(), TenantConfigurationBooleanValidator.class),
    /**
    *
    */
    AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME("authentication.header.authority", "hawkbit.server.ddi.security.authentication.header.authority", String.class, Boolean.FALSE.toString(), TenantConfigurationStringValidator.class),
    /**
     * boolean value {@code true} {@code false}.
     */
    AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED("authentication.targettoken.enabled", "hawkbit.server.ddi.security.authentication.targettoken.enabled", Boolean.class, Boolean.FALSE.toString(), TenantConfigurationBooleanValidator.class),

    /**
     * boolean value {@code true} {@code false}.
     */
    AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED("authentication.gatewaytoken.enabled", "hawkbit.server.ddi.security.authentication.gatewaytoken.enabled", Boolean.class, Boolean.FALSE.toString(), TenantConfigurationBooleanValidator.class),
    /**
     * string value which holds the name of the security token key.
     */
    AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_NAME("authentication.gatewaytoken.name", "hawkbit.server.ddi.security.authentication.gatewaytoken.name", String.class, null, TenantConfigurationStringValidator.class),

    /**
     * string value which holds the actual security-key of the gateway security
     * token.
     */
    AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY("authentication.gatewaytoken.key", "hawkbit.server.ddi.security.authentication.gatewaytoken.key", String.class, null, TenantConfigurationStringValidator.class),

    /**
     * string value which holds the polling time interval in the format HH:mm:ss
     */
    POLLING_TIME_INTERVAL("pollingOverdueTime", "hawkbit.controller.pollingOverdueTime", String.class, null, TenantConfigurationPollingDurationValidator.class),

    /**
     * string value which holds the polling time interval in the format HH:mm:ss
     */
    POLLING_OVERDUE_TIME_INTERVAL("pollingTime", "hawkbit.controller.pollingTime", String.class, null, TenantConfigurationPollingDurationValidator.class);

    private final String keyName;
    private final String defaultKeyName;
    private final Class<?> dataType;
    private final String defaultValue;
    private final Class<? extends TenantConfigurationValidator> validator;

    /**
     * 
     * @param key
     *            the property key name
     * @param defaultKeyName
     *            the allowed values for this specific key
     * @param dataType
     *            the class of the property
     * @param defaultValue
     *            value which should be returned, in case there is no value in
     *            the database
     * @param validator
     *            Validator which validates, that property is of correct format
     */
    private TenantConfigurationKey(final String key, final String defaultKeyName, final Class<?> dataType,
            final String defaultValue, final Class<? extends TenantConfigurationValidator> validator) {
        this.keyName = key;
        this.dataType = dataType;
        this.defaultKeyName = defaultKeyName;
        this.defaultValue = defaultValue;
        this.validator = validator;
    }

    /**
     * @return the keyName
     */
    public String getKeyName() {
        return keyName;
    }

    /**
     * @return the defaultKeyName
     */
    public String getDefaultKeyName() {
        return defaultKeyName;
    }

    /**
     * @return the defaultValue
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * 
     * @return the data type of the tenant configuration value. (e.g.
     *         Integer.class, String.class)
     */
    public Class<?> getDataType() {
        return dataType;
    }

    /**
     * validates if a object matches the allowed data format of the
     * corresponding key
     * 
     * @param context
     *            application context
     * @param value
     *            which will be validated
     * @throws TenantConfigurationValidatorException
     *             is thrown, when object is invalid
     */
    public void validate(final ApplicationContext context, final Object value) {
        final TenantConfigurationValidator createdBean = context.getAutowireCapableBeanFactory().createBean(validator);
        try {
            createdBean.validate(value);
        } finally {
            context.getAutowireCapableBeanFactory().destroyBean(createdBean);
        }
    }

    /**
     * @param keyName
     *            name of the TenantConfigurationKey
     * @return the TenantConfigurationKey with the name keyName
     */
    public static TenantConfigurationKey fromKeyName(final String keyName) {

        final Optional<TenantConfigurationKey> optKey = Arrays.stream(TenantConfigurationKey.values())
                .filter(conf -> conf.getKeyName().equals(keyName)).findFirst();

        if (optKey.isPresent()) {
            return optKey.get();
        }
        throw new InvalidTenantConfigurationKeyException("The given configuration key name does not exist.");
    }
}
