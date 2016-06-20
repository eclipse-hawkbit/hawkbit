/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Optional;

import org.eclipse.hawkbit.repository.TenantStatsManagement;
import org.eclipse.hawkbit.repository.report.model.TenantUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_UNCOMMITTED)
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
