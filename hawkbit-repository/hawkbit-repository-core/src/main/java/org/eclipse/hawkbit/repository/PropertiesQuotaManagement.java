/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link QuotaManagement} implementation based on spring boot
 * {@link ConfigurationProperties}.
 */
public class PropertiesQuotaManagement implements QuotaManagement {

    private final HawkbitSecurityProperties securityProperties;

    /**
     * @param securityProperties that holds the quota definitions
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

    @Override
    public int getMaxMetaDataEntriesPerSoftwareModule() {
        return securityProperties.getDos().getMaxMetaDataEntriesPerSoftwareModule();
    }

    @Override
    public int getMaxMetaDataEntriesPerDistributionSet() {
        return securityProperties.getDos().getMaxMetaDataEntriesPerDistributionSet();
    }

    @Override
    public int getMaxMetaDataEntriesPerTarget() {
        return securityProperties.getDos().getMaxMetaDataEntriesPerTarget();
    }

    @Override
    public int getMaxSoftwareModulesPerDistributionSet() {
        return securityProperties.getDos().getMaxSoftwareModulesPerDistributionSet();
    }

    @Override
    public int getMaxSoftwareModuleTypesPerDistributionSetType() {
        return securityProperties.getDos().getMaxSoftwareModuleTypesPerDistributionSetType();
    }

    @Override
    public int getMaxArtifactsPerSoftwareModule() {
        return securityProperties.getDos().getMaxArtifactsPerSoftwareModule();
    }

    @Override
    public int getMaxTargetsPerRolloutGroup() {
        return securityProperties.getDos().getMaxTargetsPerRolloutGroup();
    }

    @Override
    public int getMaxTargetDistributionSetAssignmentsPerManualAssignment() {
        return securityProperties.getDos().getMaxTargetDistributionSetAssignmentsPerManualAssignment();
    }

    @Override
    public int getMaxTargetsPerAutoAssignment() {
        return securityProperties.getDos().getMaxTargetsPerAutoAssignment();
    }

    @Override
    public int getMaxActionsPerTarget() {
        return securityProperties.getDos().getMaxActionsPerTarget();
    }

    @Override
    public long getMaxArtifactSize() {
        return securityProperties.getDos().getMaxArtifactSize();
    }

    @Override
    public long getMaxArtifactStorage() {
        return securityProperties.getDos().getMaxArtifactStorage();
    }

    @Override
    public int getMaxDistributionSetTypesPerTargetType() {
        return securityProperties.getDos().getMaxDistributionSetTypesPerTargetType();
    }
}
