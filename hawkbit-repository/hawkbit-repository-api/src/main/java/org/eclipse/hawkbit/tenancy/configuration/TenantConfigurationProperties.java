/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.tenancy.configuration;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import lombok.Data;
import lombok.ToString;
import org.eclipse.hawkbit.repository.exception.InvalidTenantConfigurationKeyException;
import org.eclipse.hawkbit.repository.exception.TenantConfigurationValidatorException;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationBooleanValidator;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationIntegerValidator;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationLongValidator;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationStringValidator;
import org.eclipse.hawkbit.tenancy.configuration.validator.TenantConfigurationValidator;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;

/**
 * Properties for tenant configuration default values.
 * <p/>
 * Configuration are read from the properties where their type and validators could be specified also. Default type and validator is String.
 */
@Data
@ToString
@ConfigurationProperties("hawkbit.server.tenant")
public class TenantConfigurationProperties {

    private final Map<String, TenantConfigurationKey> configuration = new HashMap<>();

    /**
     * @return full list of {@link TenantConfigurationKey}s
     */
    public Collection<TenantConfigurationKey> getConfigurationKeys() {
        return configuration.values();
    }

    /**
     * @param keyName name of the TenantConfigurationKey
     * @return the TenantConfigurationKey with the name keyName
     */
    public TenantConfigurationKey fromKeyName(final String keyName) {
        return configuration.values().stream()
                .filter(conf -> conf.getKeyName().equals(keyName))
                .findAny()
                .orElseThrow(() -> new InvalidTenantConfigurationKeyException("The given configuration key " + keyName + " does not exist."));
    }

    /**
     * AccessContext specific configurations which can be configured for each tenant separately by means of override of the system defaults.
     */
    @Data
    @ToString
    public static class TenantConfigurationKey {

        /**
         * Header based authentication enabled.
         */
        public static final String AUTHENTICATION_HEADER_ENABLED = "authentication.header.enabled";
        /**
         * Header based authentication authority(-ies, could be list).
         */
        public static final String AUTHENTICATION_HEADER_AUTHORITY = "authentication.header.authority";
        /**
         * Target token based authentication enabled.
         */
        public static final String AUTHENTICATION_TARGET_SECURITY_TOKEN_ENABLED = "authentication.targettoken.enabled";
        /**
         * Gateway token based authentication enabled.
         */
        public static final String AUTHENTICATION_GATEWAY_SECURITY_TOKEN_ENABLED = "authentication.gatewaytoken.enabled";
        /**
         * Gateway token value.
         */
        public static final String AUTHENTICATION_GATEWAY_SECURITY_TOKEN_KEY = "authentication.gatewaytoken.key";
        /**
         * See system default in {@link ControllerPollProperties#getPollingTime()}.
         */
        public static final String POLLING_TIME = "pollingTime";
        /**
         * See system default in {@link ControllerPollProperties#getPollingOverdueTime()}.
         */
        public static final String POLLING_OVERDUE_TIME = "pollingOverdueTime";
        /**
         * See system default in {@link ControllerPollProperties#getMaintenanceWindowPollCount()}.
         */
        public static final String MAINTENANCE_WINDOW_POLL_COUNT = "maintenanceWindowPollCount";
        /**
         * Represents setting if approval for a rollout is needed.
         */
        public static final String ROLLOUT_APPROVAL_ENABLED = "rollout.approval.enabled";
        /**
         * Represents setting if approval for an auto assignment is needed.
         */
        public static final String AUTO_ASSIGNMENT_APPROVAL_ENABLED = "auto.assignment.approval.enabled";
        /**
         * Repository on autoclose mode instead of canceling in case of new Distribution Set assignment over active actions.
         */
        public static final String REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED = "repository.actions.autoclose.enabled";
        /**
         * Specifies the action expiry in milliseconds.
         */
        public static final String ACTION_CLEANUP_AUTO_EXPIRY = "action.cleanup.auto.expiry";
        /**
         * Specifies the action status.
         */
        public static final String ACTION_CLEANUP_AUTO_STATUS = "action.cleanup.auto.status";
        /**
         * Configuration value for percentage of oldest actions to be cleaned if @maxActionsPerTarget quota is hit
         */
        public static final String ACTION_CLEANUP_ON_QUOTA_HIT_PERCENTAGE = "action.cleanup.onQuotaHit.percent";
        /**
         * Configuration value for allowed action statuses that can be deleted by user
         */
        public static final String ACTION_USER_DELETION_ALLOWED_STATUSES = "action.delete.allowed.statuses";
        /**
         * Switch to enable/disable the batch-assignment feature.
         */
        public static final String BATCH_ASSIGNMENTS_ENABLED = "batch.assignments.enabled";
        /**
         * Switch to enable/disable the user-confirmation flow
         */
        public static final String USER_CONFIRMATION_FLOW_ENABLED = "user.confirmation.flow.enabled";
        /**
         * Switch to enable/disable the implicit locking
         */
        public static final String IMPLICIT_LOCK_ENABLED = "implicit.lock.enabled";

        /**
         * Optional status code written on the initial action status of an online assignment that starts without waiting for a
         * confirmation (direct assignment, rollout group start, or confirmation given by the initiator). Not defined = no code.
         */
        public static final String ACTION_STATUS_CODE_ASSIGNED = "action.status.code.assigned";
        /**
         * Optional status code written on the initial (finished) action status of an offline assignment. Not defined = no code.
         */
        public static final String ACTION_STATUS_CODE_OFFLINE_ASSIGNED = "action.status.code.offline-assigned";
        /**
         * Optional status code written on the action status of a rollout action that is skipped (closed) because the
         * distribution set is already assigned to the target. Not defined = no code.
         */
        public static final String ACTION_STATUS_CODE_SKIPPED = "action.status.code.skipped";
        /**
         * Optional status code written on the retrieved action status when the device retrieves an action. Not defined = no code.
         */
        public static final String ACTION_STATUS_CODE_RETRIEVED = "action.status.code.retrieved";
        /**
         * Optional status code written on the download action status when the device downloads an artifact. Does not change the
         * last action status code of the action. Not defined = no code.
         */
        public static final String ACTION_STATUS_CODE_DOWNLOAD = "action.status.code.download";
        /**
         * Optional status code written on the action status that confirms an action due to an active auto-confirmation. Not
         * defined = no code.
         */
        public static final String ACTION_STATUS_CODE_AUTO_CONFIRMATION = "action.status.code.auto-confirmation";
        /**
         * Optional status code written on the initial action status of an action waiting for confirmation. Not defined = no code.
         */
        public static final String ACTION_STATUS_CODE_WAIT_FOR_CONFIRMATION = "action.status.code.wait-for-confirmation";
        /**
         * Optional status code written on the action status of all server-side cancellations (manual cancel, cancel due to a new
         * assignment, rollout or distribution set invalidation soft cancel, lapsed maintenance window). Not defined = no code.
         */
        public static final String ACTION_STATUS_CODE_CANCELING = "action.status.code.canceling";
        /**
         * Optional status code written on the action status of server-side closings (force quit, auto close due to a new
         * assignment, rollout force cancel). Not defined = no code.
         */
        public static final String ACTION_STATUS_CODE_CANCELED = "action.status.code.canceled";

        private static final Map<Class<? extends Serializable>, TenantConfigurationValidator> DEFAULT_TYPE_VALIDATORS = Map.of(
                Boolean.class, new TenantConfigurationBooleanValidator(),
                Integer.class, new TenantConfigurationIntegerValidator(),
                Long.class, new TenantConfigurationLongValidator(),
                String.class, new TenantConfigurationStringValidator());

        private String keyName;
        private String defaultValue = "";
        private Class<? extends Serializable> dataType = String.class;
        private Class<? extends TenantConfigurationValidator> validator;

        /**
         * Validates if an object matches the allowed data format of the corresponding key
         *
         * @param value which will be validated
         * @param context application context
         * @throws TenantConfigurationValidatorException is thrown, when object is invalid
         */
        public void validate(final Object value, final ApplicationContext context) {
            if (validator == null) {
                Objects.requireNonNull(DEFAULT_TYPE_VALIDATORS.get(dataType), "No validator defined for " + keyName).validate(value);
            } else {
                final TenantConfigurationValidator createdBean = context.getAutowireCapableBeanFactory().createBean(validator);
                try {
                    createdBean.validate(value);
                } finally {
                    context.getAutowireCapableBeanFactory().destroyBean(createdBean);
                }
            }
        }
    }
}
