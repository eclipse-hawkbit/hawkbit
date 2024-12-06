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

import java.util.function.Consumer;

import jakarta.persistence.EntityManager;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.artifact.repository.ArtifactRepository;
import org.eclipse.hawkbit.cache.TenancyCacheManager;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.RolloutStatusCache;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantStatsManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.CurrentTenantCacheKeyGenerator;
import org.eclipse.hawkbit.repository.jpa.SystemManagementCacheKeyGenerator;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetFilterQueryRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TenantConfigurationRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TenantMetaDataRepository;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.report.model.SystemUsageReport;
import org.eclipse.hawkbit.repository.report.model.SystemUsageReportWithTenants;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link SystemManagement}.
 */
@Slf4j
@Transactional(readOnly = true)
@Validated
public class JpaSystemManagement implements CurrentTenantCacheKeyGenerator, SystemManagement {

    private static final int MAX_TENANTS_QUERY = 1000;

    private final String countArtifactQuery;
    private final String countSoftwareModulesQuery;

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TargetRepository targetRepository;
    @Autowired
    private TargetFilterQueryRepository targetFilterQueryRepository;
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
    private TargetTypeRepository targetTypeRepository;
    @Autowired
    private DistributionSetTagRepository distributionSetTagRepository;
    @Autowired
    private TenantConfigurationRepository tenantConfigurationRepository;
    @Autowired
    private RolloutRepository rolloutRepository;
    @Autowired
    private TenantAware tenantAware;
    @Autowired
    private TenantStatsManagement systemStatsManagement;
    @Autowired
    private TenancyCacheManager cacheManager;
    @Autowired
    private SystemManagementCacheKeyGenerator currentTenantCacheKeyGenerator;
    @Autowired
    private SystemSecurityContext systemSecurityContext;
    @Autowired
    private PlatformTransactionManager txManager;
    @Autowired
    private RolloutStatusCache rolloutStatusCache;
    @Autowired(required = false) // it's not required on dmf/ddi only instances
    private ArtifactRepository artifactRepository;
    @Autowired
    private RepositoryProperties repositoryProperties;

    /**
     * Constructor.
     *
     * @param properties properties to get the underlying database
     */
    public JpaSystemManagement(final JpaProperties properties) {
        final String isDeleted = isPostgreSql(properties) ? "false" : "0";
        countArtifactQuery = "SELECT COUNT(a.id) FROM sp_artifact a INNER JOIN sp_base_software_module sm ON a.software_module = sm.id WHERE sm.deleted = " + isDeleted;
        countSoftwareModulesQuery = "select SUM(file_size) from sp_artifact a INNER JOIN sp_base_software_module sm ON a.software_module = sm.id WHERE sm.deleted = " + isDeleted;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public KeyGenerator currentTenantKeyGenerator() {
        return currentTenantCacheKeyGenerator.currentTenantKeyGenerator();
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteTenant(final String t) {
        if (artifactRepository == null) {
            throw new IllegalStateException("Artifact repository is not available. Can't delete tenant.");
        }

        final String tenant = t.toUpperCase();
        cacheManager.evictCaches(tenant);
        rolloutStatusCache.evictCaches(tenant);
        tenantAware.runAsTenant(tenant, () -> {
            entityManager.setProperty(PersistenceUnitProperties.MULTITENANT_PROPERTY_DEFAULT, tenant);
            tenantMetaDataRepository.deleteByTenantIgnoreCase(tenant);
            tenantConfigurationRepository.deleteByTenant(tenant);
            targetRepository.deleteByTenant(tenant);
            targetFilterQueryRepository.deleteByTenant(tenant);
            rolloutRepository.deleteByTenant(tenant);
            targetTypeRepository.deleteByTenant(tenant);
            targetTagRepository.deleteByTenant(tenant);
            distributionSetTagRepository.deleteByTenant(tenant);
            distributionSetRepository.deleteByTenant(tenant);
            distributionSetTypeRepository.deleteByTenant(tenant);
            softwareModuleRepository.deleteByTenant(tenant);
            artifactRepository.deleteByTenant(tenant);
            softwareModuleTypeRepository.deleteByTenant(tenant);
            return null;
        });
    }

    @Override
    public Page<String> findTenants(final Pageable pageable) {
        return tenantMetaDataRepository.findTenants(pageable);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    // Exception squid:S2229 - calling findTenants without transaction is
    // intended in this case
    @SuppressWarnings("squid:S2229")
    public void forEachTenant(final Consumer<String> consumer) {
        Page<String> tenants;
        Pageable query = PageRequest.of(0, MAX_TENANTS_QUERY);
        do {
            tenants = findTenants(query);
            tenants.forEach(tenant -> tenantAware.runAsTenant(tenant, () -> {
                try {
                    consumer.accept(tenant);
                } catch (final RuntimeException ex) {
                    log.debug("Exception on forEachTenant execution for tenant {}. Continue with next tenant.",
                            tenant, ex);
                    log.error("Exception on forEachTenant execution for tenant {} with error message [{}]. "
                            + "Continue with next tenant.", tenant, ex.getMessage());
                }
                return null;
            }));
        } while ((query = tenants.nextPageable()) != Pageable.unpaged());

    }

    @Override
    public SystemUsageReportWithTenants getSystemUsageStatisticsWithTenants() {
        final SystemUsageReportWithTenants result = (SystemUsageReportWithTenants) getSystemUsageStatistics();

        usageStatsPerTenant(result);

        return result;
    }

    @Override
    public SystemUsageReport getSystemUsageStatistics() {

        final Number count = (Number) entityManager.createNativeQuery(countSoftwareModulesQuery).getSingleResult();

        long sumOfArtifacts = 0;
        if (count != null) {
            sumOfArtifacts = count.longValue();
        }

        // we use native queries to punch through the tenant boundaries. This
        // has to be used with care!
        final long targets = ((Number) entityManager.createNativeQuery("SELECT COUNT(id) FROM sp_target")
                .getSingleResult()).longValue();

        final long artifacts = ((Number) entityManager.createNativeQuery(countArtifactQuery).getSingleResult())
                .longValue();

        final long actions = ((Number) entityManager.createNativeQuery("SELECT COUNT(id) FROM sp_action")
                .getSingleResult()).longValue();

        return new SystemUsageReportWithTenants(targets, artifacts, actions, sumOfArtifacts,
                tenantMetaDataRepository.count());
    }

    @Override
    public TenantMetaData getTenantMetadata() {
        final String tenant = tenantAware.getCurrentTenant();
        if (tenant == null) {
            throw new IllegalStateException("Tenant not set");
        }

        final TenantMetaData metaData = tenantMetaDataRepository.findByTenantIgnoreCase(tenant);
        if (metaData == null) {
            if (repositoryProperties.isImplicitTenantCreateAllowed()) {
                log.info("Tenant {} doesn't exist create metadata", tenant, new Exception("Thread dump"));
                return createTenantMetadata0(tenant);
            } else {
                throw new EntityNotFoundException(TenantMetaData.class, tenant);
            }
        } else {
            return metaData;
        }
    }

    @Override
    public TenantMetaData createTenantMetadata(final String tenant) {
        final TenantMetaData result = tenantMetaDataRepository.findByTenantIgnoreCase(tenant);
        // Create if it does not exist
        if (result == null) {
            return createTenantMetadata0(tenant);
        }
        return result;
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TenantMetaData updateTenantMetadata(final long defaultDsType) {
        final JpaTenantMetaData data = (JpaTenantMetaData) getTenantMetadata();

        data.setDefaultDsType(distributionSetTypeRepository.findById(defaultDsType)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, defaultDsType)));

        return tenantMetaDataRepository.save(data);
    }

    @Override
    public TenantMetaData getTenantMetadata(final long tenantId) {
        return tenantMetaDataRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException(TenantMetaData.class, tenantId));
    }

    private static boolean isPostgreSql(final JpaProperties properties) {
        return Database.POSTGRESQL == properties.getDatabase();
    }

    private void usageStatsPerTenant(final SystemUsageReportWithTenants report) {
        forEachTenant(tenant -> report.addTenantData(systemStatsManagement.getStatsOfTenant()));
    }

    private DistributionSetType createStandardSoftwareDataSetup() {
        final SoftwareModuleType app = softwareModuleTypeRepository
                .save(new JpaSoftwareModuleType(org.eclipse.hawkbit.repository.Constants.SMT_DEFAULT_APP_KEY,
                        org.eclipse.hawkbit.repository.Constants.SMT_DEFAULT_APP_NAME, "Application Addons",
                        Integer.MAX_VALUE));
        final SoftwareModuleType os = softwareModuleTypeRepository.save(new JpaSoftwareModuleType(
                org.eclipse.hawkbit.repository.Constants.SMT_DEFAULT_OS_KEY,
                org.eclipse.hawkbit.repository.Constants.SMT_DEFAULT_OS_NAME, "Core firmware or operation system", 1));

        // make sure the module types get their IDs
        entityManager.flush();

        distributionSetTypeRepository
                .save(new JpaDistributionSetType(org.eclipse.hawkbit.repository.Constants.DST_DEFAULT_OS_ONLY_KEY,
                        org.eclipse.hawkbit.repository.Constants.DST_DEFAULT_OS_ONLY_NAME,
                        "Default type with Firmware/OS only.").addMandatoryModuleType(os));

        distributionSetTypeRepository
                .save(new JpaDistributionSetType(org.eclipse.hawkbit.repository.Constants.DST_DEFAULT_APP_ONLY_KEY,
                        org.eclipse.hawkbit.repository.Constants.DST_DEFAULT_APP_ONLY_NAME,
                        "Default type with app(s) only.").addMandatoryModuleType(app));

        return distributionSetTypeRepository
                .save(new JpaDistributionSetType(org.eclipse.hawkbit.repository.Constants.DST_DEFAULT_OS_WITH_APPS_KEY,
                        org.eclipse.hawkbit.repository.Constants.DST_DEFAULT_OS_WITH_APPS_NAME,
                        "Default type with Firmware/OS and optional app(s).").addMandatoryModuleType(os)
                        .addOptionalModuleType(app));
    }

    private TenantMetaData createTenantMetadata0(final String tenant) {
        try {
            currentTenantCacheKeyGenerator.setTenantInCreation(tenant);
            return createInitialTenantMetaData(tenant);
        } finally {
            currentTenantCacheKeyGenerator.removeTenantInCreation();
        }
    }

    /**
     * Creating the initial tenant meta-data in a new transaction. Due to the
     * {@link org.eclipse.hawkbit.repository.jpa.configuration.MultiTenantJpaTransactionManager} is using the current tenant to
     * set the necessary tenant discriminator to the query. This is not working
     * if we don't have a current tenant set. Due to the
     * {@link #createTenantMetadata(String)} is maybe called without having a
     * current tenant we need to re-open a new transaction so the
     * {@link org.eclipse.hawkbit.repository.jpa.configuration.MultiTenantJpaTransactionManager} is called again and set the
     * tenant for this transaction.
     *
     * @param tenant the tenant to be created
     * @return the initial created {@link TenantMetaData}
     */
    private TenantMetaData createInitialTenantMetaData(final String tenant) {
        return systemSecurityContext.runAsSystemAsTenant(
                () -> DeploymentHelper.runInNewTransaction(txManager, "initial-tenant-creation", status -> {
                    final DistributionSetType defaultDsType = createStandardSoftwareDataSetup();
                    return tenantMetaDataRepository.save(new JpaTenantMetaData(defaultDsType, tenant));
                }), tenant);
    }
}
