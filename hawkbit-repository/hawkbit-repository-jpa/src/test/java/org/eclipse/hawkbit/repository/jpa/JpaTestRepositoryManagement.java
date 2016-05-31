/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.eclipse.hawkbit.cache.TenantAwareCacheManager;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.util.TestRepositoryManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.transaction.annotation.Transactional;

public class JpaTestRepositoryManagement implements TestRepositoryManagement {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TargetRepository targetRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private DistributionSetRepository distributionSetRepository;

    @Autowired
    private SoftwareModuleRepository softwareModuleRepository;

    @Autowired
    private TenantMetaDataRepository tenantMetaDataRepository;

    @Autowired
    private DistributionSetTypeRepository distributionSetTypeRepository;

    @Autowired
    private SoftwareModuleTypeRepository softwareModuleTypeRepository;

    @Autowired
    private TargetTagRepository targetTagRepository;

    @Autowired
    private DistributionSetTagRepository distributionSetTagRepository;

    @Autowired
    private SoftwareModuleMetadataRepository softwareModuleMetadataRepository;

    @Autowired
    private ActionStatusRepository actionStatusRepository;

    @Autowired
    private ExternalArtifactRepository externalArtifactRepository;

    @Autowired
    private LocalArtifactRepository artifactRepository;

    @Autowired
    private TargetInfoRepository targetInfoRepository;

    @Autowired
    private GridFsOperations operations;

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private RolloutRepository rolloutRepository;

    @Autowired
    private TenantAwareCacheManager cacheManager;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    @Autowired
    private SystemManagement systemManagement;

    @Override
    public void clearTestRepository() {
        deleteAllRepos();
        cacheManager.getDirectCacheNames().forEach(name -> cacheManager.getDirectCache(name).clear());
    }

    @Transactional
    public void deleteAllRepos() {
        final List<String> tenants = systemSecurityContext.runAsSystem(() -> systemManagement.findTenants());
        tenants.forEach(tenant -> {
            try {
                systemSecurityContext.runAsSystem(() -> {
                    systemManagement.deleteTenant(tenant);
                    return null;
                });
            } catch (final Exception e) {
                e.printStackTrace();
            }
        });
    }
}
