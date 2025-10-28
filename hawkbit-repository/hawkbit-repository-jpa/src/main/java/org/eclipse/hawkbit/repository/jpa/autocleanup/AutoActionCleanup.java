/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.autocleanup;

import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_AUTO_EXPIRY;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_AUTO_STATUS;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;

/**
 * A cleanup task for {@link Action} entities which can be used to delete actions which are in a certain {@link Action.Status}.
 * It is recommended to only clean up actions which have terminated already (i.e. actions in status CANCELLED or ERROR).
 * <p/>
 * The cleanup task can be enabled /disabled and configured on a per-tenant basis.
 */
@Slf4j
public class AutoActionCleanup implements AutoCleanupScheduler.CleanupTask {

    private static final String ID = "action-cleanup";
    private static final EnumSet<Status> EMPTY_STATUS_SET = EnumSet.noneOf(Status.class);

    private final DeploymentManagement deploymentMgmt;
    private final TenantConfigurationManagement config;

    public AutoActionCleanup(final DeploymentManagement deploymentMgmt, final TenantConfigurationManagement configMgmt) {
        this.deploymentMgmt = deploymentMgmt;
        this.config = configMgmt;
    }

    @Override
    public void run() {
        final TenantConfigurationValue<Long> expiryCV = config.getConfigurationValue(ACTION_CLEANUP_AUTO_EXPIRY, Long.class);
        final long expiry = expiryCV != null ? expiryCV.getValue() : -1L;
        if (expiry < 0L) {
            log.debug("Action cleanup is disabled for this tenant...");
        } else {
            final EnumSet<Status> status = getActionStatus();
            if (status.isEmpty()) {
                log.debug("Action cleanup is disabled for this tenant...");
            } else {
                final long lastModified = System.currentTimeMillis() - expiry;
                final int actionsCount = deploymentMgmt.deleteActionsByStatusAndLastModifiedBefore(status, lastModified);
                log.debug("Deleted {} actions in status {} which have not been modified since {} ({})",
                        actionsCount, status, Instant.ofEpochMilli(lastModified), lastModified);
            }
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    private EnumSet<Status> getActionStatus() {
        final TenantConfigurationValue<String> statusStr = config.getConfigurationValue(ACTION_CLEANUP_AUTO_STATUS, String.class);
        if (statusStr != null) {
            return Arrays.stream(statusStr.getValue().split("[;,]"))
                    .map(Status::valueOf).collect(Collectors.toCollection(() -> EnumSet.noneOf(Status.class)));
        }
        return EMPTY_STATUS_SET;
    }
}