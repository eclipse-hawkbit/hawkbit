/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.hawkbit.eventbus.event.DownloadProgressEvent;
import org.eclipse.hawkbit.repository.TenantStatsManagement;
import org.eclipse.hawkbit.repository.report.model.TenantUsage;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.eventbus.Subscribe;

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

    private final Map<String, AtomicLong> traffic = new ConcurrentHashMap<>();

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_UNCOMMITTED)
    public TenantUsage getStatsOfTenant() {
        final String tenant = tenantAware.getCurrentTenant();

        final TenantUsage result = new TenantUsage(tenant);

        result.setTargets(targetRepository.count());

        final Long artifacts = artifactRepository.countBySoftwareModuleDeleted(false);
        result.setArtifacts(artifacts);

        final Optional<Long> artifactsSize = artifactRepository.getSumOfUndeletedArtifactSize();
        if (artifactsSize.isPresent()) {
            result.setOverallArtifactVolumeInBytes(artifactsSize.get());
        }

        result.setActions(actionRepository.count());
        if (traffic.containsKey(tenant)) {
            result.setOverallArtifactTrafficInBytes(traffic.get(tenant).get());
        }

        return result;

    }

    @Override
    public void resetTrafficStatsOfTenant() {
        traffic.remove(tenantAware.getCurrentTenant());
    }

    @Subscribe
    public void listen(final DownloadProgressEvent event) {
        if (traffic.containsKey(event.getTenant())) {
            traffic.get(event.getTenant()).addAndGet(event.getShippedBytes());
        } else {
            traffic.put(event.getTenant(), new AtomicLong(event.getShippedBytes()));
        }
    }

}
