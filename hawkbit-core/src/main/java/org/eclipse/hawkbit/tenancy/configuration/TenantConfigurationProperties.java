/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.tenancy.configuration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationStringValidator;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationValidator;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationValidatorException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;

/**
 * Properties for tenant configuration default values.
 *
 */
@ConfigurationProperties("hawkbit.server.tenant")
public class TenantConfigurationProperties {

    private final Map<String, TenantConfigurationKey> configuration = new HashMap<>();

    public Map<String, TenantConfigurationKey> getConfiguration() {
        return configuration;
    }

    public Collection<TenantConfigurationKey> getConfigurationKeys() {
        return configuration.values();
    }

    /**
     * @param keyName
     *            name of the TenantConfigurationKey
     * @return the TenantConfigurationKey with the name keyName
     */
    public TenantConfigurationKey fromKeyName(final String keyName) {
        return configuration.values().stream().filter(conf -> conf.getKeyName().equals(keyName)).findFirst()
                .orElseThrow(() -> new InvalidTenantConfigurationKeyException(
                        "The given configuration key " + keyName + " does not exist."));
    }

    public static class TenantConfigurationKey {
        public static final String AUTHENTICATION_MODE_HEADER_ENABLED = "authentication.header.enabled";
        public static final String AUTHENTICATION_MODE_HEADER_AUTHORITY_NAME = "authentication.header.authority";
        public static final String AUTHENTICATION_MODE_TARGET_SECURITY_TOKEN_ENABLED = "authentication.targettoken.enabled";
        public static final String AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_ENABLED = "authentication.gatewaytoken.enabled";
        public static final String AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_NAME = "authentication.gatewaytoken.name";
        public static final String AUTHENTICATION_MODE_GATEWAY_SECURITY_TOKEN_KEY = "authentication.gatewaytoken.key";
        public static final String POLLING_TIME_INTERVAL = "pollingTime";
        public static final String POLLING_OVERDUE_TIME_INTERVAL = "pollingOverdueTime";
        public static final String ANONYMOUS_DOWNLOAD_MODE_ENABLED = "anonymous.download.enabled";

        private String keyName;
        private String defaultValue;
        private Class<?> dataType = String.class;
        private Class<? extends TenantConfigurationValidator> validator = TenantConfigurationStringValidator.class;

        public String getKeyName() {
            return keyName;
        }

        public void setKeyName(final String keyName) {
            this.keyName = keyName;
        }

        /**
         * 
         * @return the data type of the tenant configuration value. (e.g.
         *         Integer.class, String.class)
         */
        @SuppressWarnings("unchecked")
        public <T> Class<T> getDataType() {
            return (Class<T>) dataType;
        }

        public void setDataType(final Class<?> dataType) {
            this.dataType = dataType;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(final String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public Class<? extends TenantConfigurationValidator> getValidator() {
            return validator;
        }

        public void setValidator(final Class<? extends TenantConfigurationValidator> validator) {
            this.validator = validator;
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
            final TenantConfigurationValidator createdBean = context.getAutowireCapableBeanFactory()
                    .createBean(validator);
            try {
                createdBean.validate(value);
            } finally {
                context.getAutowireCapableBeanFactory().destroyBean(createdBean);
            }
        }

    }
}
