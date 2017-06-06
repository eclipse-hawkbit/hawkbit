/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link QuotaManagement} implementation based on spring boot
 * {@link ConfigurationProperties}.
 *
 */
public class PropertiesQuotaManagement implements QuotaManagement {

    private final HawkbitSecurityProperties securityProperties;

    /**
     * @param securityProperties
     *            that holds the quota definitions
     */
    public PropertiesQuotaManagement(final HawkbitSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public int getMaxStatusEntriesPerAction() {
        return securityProperties.getDos().getMaxStatusEntriesPerAction();
    }

    @Override
    public int getMaxAttributeEntriesPerTarget() {
        return securityProperties.getDos().getMaxAttributeEntriesPerTarget();
    }

    @Override
    public int getMaxRolloutGroupsPerRollout() {
        return securityProperties.getDos().getMaxRolloutGroupsPerRollout();
    }

    @Override
    public int getMaxMessagesPerActionStatus() {
        return securityProperties.getDos().getMaxMessagesPerActionStatus();
    }

}
