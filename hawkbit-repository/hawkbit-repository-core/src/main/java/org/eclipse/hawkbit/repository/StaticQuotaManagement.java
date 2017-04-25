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

public class StaticQuotaManagement implements QuotaManagement {

    private final HawkbitSecurityProperties securityProperties;

    public StaticQuotaManagement(final HawkbitSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public int getMaxStatusEntriesPerAction() {
        return securityProperties.getDos().getMaxStatusEntriesPerAction();
    }

    public int getMaxAttributeEntriesPerTarget() {
        return securityProperties.getDos().getMaxAttributeEntriesPerTarget();
    }

    public int getMaxRolloutGroupsPerRollout() {
        return securityProperties.getDos().getMaxRolloutGroupsPerRollout();
    }

}
