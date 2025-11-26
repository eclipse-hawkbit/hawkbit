/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.TenantStatsManagement;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.ArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.model.report.TenantUsage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Management service for statistics of a single tenant.
 */
@Validated
@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "tenant-stats-management" }, matchIfMissing = true)
public class JpaTenantStatsManagement implements TenantStatsManagement {

    private final TargetRepository targetRepository;
    private final ArtifactRepository artifactRepository;
    private final ActionRepository actionRepository;

    protected JpaTenantStatsManagement(
            final TargetRepository targetRepository, final ArtifactRepository artifactRepository, final ActionRepository actionRepository) {
        this.targetRepository = targetRepository;
        this.artifactRepository = artifactRepository;
        this.actionRepository = actionRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TenantUsage getStatsOfTenant() {
        final String tenant = AccessContext.tenant();

        final TenantUsage result = new TenantUsage(tenant);

        result.setTargets(targetRepository.count());
        result.setArtifacts(artifactRepository.countBySoftwareModuleDeleted(false));
        artifactRepository.sumOfNonDeletedArtifactSize().ifPresent(result::setOverallArtifactVolumeInBytes);
        result.setActions(actionRepository.count());

        return result;
    }
}