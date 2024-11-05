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

import org.eclipse.hawkbit.repository.TenantStatsManagement;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.LocalArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.report.model.TenantUsage;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Management service for statistics of a single tenant.
 */
@Validated
public class JpaTenantStatsManagement implements TenantStatsManagement {

    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private LocalArtifactRepository artifactRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private TenantAware tenantAware;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TenantUsage getStatsOfTenant() {
        final String tenant = tenantAware.getCurrentTenant();

        final TenantUsage result = new TenantUsage(tenant);

        result.setTargets(targetRepository.count());
        result.setArtifacts(artifactRepository.countBySoftwareModuleDeleted(false));
        artifactRepository.sumOfNonDeletedArtifactSize().ifPresent(result::setOverallArtifactVolumeInBytes);
        result.setActions(actionRepository.count());

        return result;
    }
}
