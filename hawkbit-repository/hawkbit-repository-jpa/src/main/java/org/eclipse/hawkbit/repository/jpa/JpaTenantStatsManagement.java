/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.TenantStatsManagement;
import org.eclipse.hawkbit.repository.report.model.TenantUsage;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Management service for statistics of a single tenant.
 *
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
        artifactRepository.getSumOfUndeletedArtifactSize().ifPresent(result::setOverallArtifactVolumeInBytes);
        result.setActions(actionRepository.count());

        return result;

    }

}
