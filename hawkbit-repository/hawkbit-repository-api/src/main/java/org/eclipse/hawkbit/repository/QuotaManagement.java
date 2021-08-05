/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * Central service for defined limits of the repository.
 *
 */
public interface QuotaManagement {

    /**
     * @return maximum number of {@link ActionStatus} entries that the
     *         controller can report for an {@link Action}.
     */
    int getMaxStatusEntriesPerAction();

    /**
     * @return maximum number of attributes that the controller can report;
     */
    int getMaxAttributeEntriesPerTarget();

    /**
     * @return maximum number of allowed {@link RolloutGroup}s per
     *         {@link Rollout}.
     */
    int getMaxRolloutGroupsPerRollout();

    /**
     * @return maximum number of
     *         {@link ControllerManagement#getActionHistoryMessages(Long, int)}
     *         for an individual {@link ActionStatus}.
     */
    int getMaxMessagesPerActionStatus();

    /**
     * @return maximum number of meta data entries per software module
     */
    int getMaxMetaDataEntriesPerSoftwareModule();

    /**
     * @return maximum number of meta data entries per distribution set
     */
    int getMaxMetaDataEntriesPerDistributionSet();

    /**
     * @return maximum number of meta data entries per target
     */
    int getMaxMetaDataEntriesPerTarget();

    /**
     * @return maximum number of software modules per distribution set
     */
    int getMaxSoftwareModulesPerDistributionSet();

    /**
     * @return the maximum number of software module types per distribution set
     *         type
     */
    int getMaxSoftwareModuleTypesPerDistributionSetType();

    /**
     * @return the maximum number of artifacts per software module
     */
    int getMaxArtifactsPerSoftwareModule();

    /**
     * @return the maximum number of targets per rollout group
     */
    int getMaxTargetsPerRolloutGroup();

    /**
     * @return the maximum number of target distribution set assignments
     *         resulting from a manual assignment
     */
    int getMaxTargetDistributionSetAssignmentsPerManualAssignment();

    /**
     * @return the maximum number of targets for an automatic distribution set
     *         assignment
     */
    int getMaxTargetsPerAutoAssignment();

    /**
     * @return the maximum number of actions per target
     */
    int getMaxActionsPerTarget();

    /**
     * @return the maximum size of artifacts in bytes
     */
    long getMaxArtifactSize();

    /**
     * @return the accumulated maximum size of all artifacts in bytes
     */
    long getMaxArtifactStorage();

    /**
     * @return the maximum number of distribution set types per target type
     */
    int getMaxDistributionSetTypesPerTargetType();

}
