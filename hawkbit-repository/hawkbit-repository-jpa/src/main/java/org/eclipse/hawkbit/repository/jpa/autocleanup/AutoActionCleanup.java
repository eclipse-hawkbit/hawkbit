/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.autocleanup;

import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_ACTION_EXPIRY;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_ACTION_STATUS;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_ENABLED;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A cleanup task for {@link Action} entities which can be used to delete
 * actions which are in a certain {@link Action.Status}. It is recommended to
 * only clean up actions which have terminated already (i.e. actions in status
 * CANCELLED or ERROR).
 * 
 * The cleanup task can be enabled /disabled and configured on a per tenant
 * basis.
 */
public class AutoActionCleanup implements CleanupTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoActionCleanup.class);

    private static final String ID = "action-cleanup";
    private static final boolean ACTION_CLEANUP_ENABLED_DEFAULT = false;
    private static final long ACTION_CLEANUP_ACTION_EXPIRY_DEFAULT = TimeUnit.DAYS.toMillis(30);
    private static final EnumSet<Status> EMPTY_STATUS_SET = EnumSet.noneOf(Status.class);

    private final DeploymentManagement deploymentMgmt;
    private final TenantConfigurationManagement config;

    /**
     * Constructs the action cleanup handler.
     * 
     * @param deploymentMgmt
     *            The {@link DeploymentManagement} to operate on.
     * @param configMgmt
     *            The {@link TenantConfigurationManagement} service.
     */
    public AutoActionCleanup(final DeploymentManagement deploymentMgmt,
            final TenantConfigurationManagement configMgmt) {
        this.deploymentMgmt = deploymentMgmt;
        this.config = configMgmt;
    }

    @Override
    public void run() {

        if (!isEnabled()) {
            LOGGER.debug("Action cleanup is disabled for this tenant...");
            return;
        }

        final EnumSet<Status> status = getActionStatus();
        if (!status.isEmpty()) {
            final long lastModified = System.currentTimeMillis() - getExpiry();
            final int actionsCount = deploymentMgmt.deleteActionsByStatusAndLastModifiedBefore(status, lastModified);
            LOGGER.debug("Deleted {} actions in status {} which have not been modified since {} ({})", actionsCount,
                    status, Instant.ofEpochMilli(lastModified), lastModified);
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    private long getExpiry() {
        final TenantConfigurationValue<Long> expiry = getConfigValue(ACTION_CLEANUP_ACTION_EXPIRY, Long.class);
        return expiry != null ? expiry.getValue() : ACTION_CLEANUP_ACTION_EXPIRY_DEFAULT;
    }

    private EnumSet<Status> getActionStatus() {
        final TenantConfigurationValue<String> statusStr = getConfigValue(ACTION_CLEANUP_ACTION_STATUS, String.class);
        if (statusStr != null) {
            return Arrays.stream(statusStr.getValue().split("[;,]")).map(Status::valueOf)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(Status.class)));
        }
        return EMPTY_STATUS_SET;
    }

    private boolean isEnabled() {
        final TenantConfigurationValue<Boolean> isEnabled = getConfigValue(ACTION_CLEANUP_ENABLED, Boolean.class);
        return isEnabled != null ? isEnabled.getValue() : ACTION_CLEANUP_ENABLED_DEFAULT;
    }

    private <T extends Serializable> TenantConfigurationValue<T> getConfigValue(final String key,
            final Class<T> valueType) {
        return config.getConfigurationValue(key, valueType);
    }

}
