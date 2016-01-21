/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Optional;

import org.eclipse.hawkbit.report.model.TenantUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Management service for stats of a single tenant.
 *
 */
@Validated
@Service
public class TenantStatsManagement {

    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private LocalArtifactRepository artifactRepository;

    @Autowired
    private ActionRepository actionRepository;

    /**
     * Service for stats of a single tenant. Opens a new transaction and as a
     * result can an be used for multiple tenants, i.e. to allow in one session
     * to collect data of all tenants in the system.
     *
     * @param tenant
     *            to collect for
     * @return collected statistics
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TenantUsage getStatsOfTenant(final String tenant) {
        final TenantUsage result = new TenantUsage(tenant);

        result.setTargets(targetRepository.count());

        final Long artifacts = artifactRepository.countBySoftwareModuleDeleted(false);
        result.setArtifacts(artifacts);

        final Optional<Long> artifactsSize = artifactRepository.getSumOfUndeletedArtifactSize();
        if (artifactsSize.isPresent()) {
            result.setOverallArtifactVolumeInBytes(artifactsSize.get());
        }

        result.setActions(actionRepository.count());

        return result;

    }

}
