/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.tenancy.configuration;

import org.eclipse.hawkbit.tenancy.configuration.validator.BooleanValidator;
import org.eclipse.hawkbit.tenancy.configuration.validator.PollTimeValidator;
import org.eclipse.hawkbit.tenancy.configuration.validator.StringValidator;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationValidator;
import org.springframework.context.ApplicationContext;

/**
 * An enum which defines the tenant specific configurations which can be
 * configured for each tenant seperately.
 *
 *
 *
 *
 */
public enum TenantConfigurationKey {

    /**
     * boolean value {@code true} {@code false}.
     */
    AUTHENTICATION_MODE_HEADER_ENABLED("authentication.header.enabled",
            "hawkbit.server.controller.security.authentication.header.enabled", Boolean.class, Boolean.FALSE.toString(),
            BooleanValidator.class),
    /**
    *
    */
    AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME("authentication.header.authority",
            "hawkbit.server.controller.security.authentication.header.authority", Boolean.class,
            Boolean.FALSE.toString(), BooleanValidator.class),
    /**
     * boolean value {@code true} {@code false}.
     */
    AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED("authentication.targettoken.enabled",
            "hawkbit.server.controller.security.authentication.targettoken.enabled", Boolean.class,
            Boolean.FALSE.toString(), BooleanValidator.class),

    /**
     * boolean value {@code true} {@code false}.
     */
    AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED("authentication.gatewaytoken.enabled",
            "hawkbit.server.controller.security.authentication.gatewaytoken.enabled", Boolean.class,
            Boolean.FALSE.toString(), BooleanValidator.class),
    /**
     * string value which holds the name of the security token key.
     */
    AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_NAME("authentication.gatewaytoken.name",
            "hawkbit.server.controller.security.authentication.gatewaytoken.name", String.class, null,
            StringValidator.class),

    /**
     * string value which holds the actual security-key of the gateway security
     * token.
     */
    AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY("authentication.gatewaytoken.key",
            "hawkbit.server.controller.security.authentication.gatewaytoken.key", String.class, null,
            StringValidator.class),

    /**
     * string value which holds the polling time interval in the format HH:mm:ss
     */
    POLLING_TIME_INTERVAL("pollingOverdueTime", "hawkbit.controller.pollingOverdueTime", String.class, null,
            PollTimeValidator.class),

    /**
     * string value which holds the polling time interval in the format HH:mm:ss
     */
    POLLING_OVERDUE_TIME_INTERVAL("pollingTime", "hawkbit.controller.pollingTime", String.class, null,
            PollTimeValidator.class);

    private final String keyName;
    private final String defaultKeyName;
    private final Class<?> dataType;
    private final String defaultValue;
    private final Class<? extends TenantConfigurationValidator> validator;

    /**
     * @param key
     *            the property key name
     * @param allowedValues
     *            the allowed values for this specific key
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
     * @return the datatype of the tenant configuration value
     */
    public Class<?> getDataType() {
        return dataType;
    }

    public boolean validate(final ApplicationContext context, final Object value) {
        final TenantConfigurationValidator createBean = context.getAutowireCapableBeanFactory().createBean(validator);
        final boolean isValid = createBean.validate(value);
        context.getAutowireCapableBeanFactory().destroyBean(createBean);
        return isValid;
    }

}
