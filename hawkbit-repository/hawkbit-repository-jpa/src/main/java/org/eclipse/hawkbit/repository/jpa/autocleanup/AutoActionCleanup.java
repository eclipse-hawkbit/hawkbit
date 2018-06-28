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
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.TenantConfigurationValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class AutoActionCleanup implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoActionCleanup.class);

    private static final boolean ACTION_CLEANUP_ENABLED_DEFAULT = false;

    private static final long ACTION_CLEANUP_ACTION_EXPIRY_DEFAULT = TimeUnit.DAYS.toMillis(30);

    private final ActionRepository actionRepository;
    private final TenantConfigurationManagement config;

    public AutoActionCleanup(final ActionRepository actionRepo, final TenantConfigurationManagement configMgmt) {
        this.actionRepository = actionRepo;
        this.config = configMgmt;
    }

    @Override
    public void run() {
        if (isEnabled()) {
            final Set<Action.Status> status = getActionStatus();
            if (!status.isEmpty()) {
                final long lastModified = System.currentTimeMillis() - getExpiry();
                actionRepository.deleteByStatusAndLastModifiedBefore(status, lastModified);
            }
        }
    }

    private long getExpiry() {
        final TenantConfigurationValue<Long> expiry = getConfigValue(ACTION_CLEANUP_ACTION_EXPIRY, Long.class);
        return expiry != null ? expiry.getValue() : ACTION_CLEANUP_ACTION_EXPIRY_DEFAULT;
    }

    private Set<Action.Status> getActionStatus() {
        final TenantConfigurationValue<String> statusStr = getConfigValue(ACTION_CLEANUP_ACTION_STATUS, String.class);
        if (statusStr != null) {
            return Arrays.stream(statusStr.getValue().split(";|,")).map(Action.Status::valueOf)
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();

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
