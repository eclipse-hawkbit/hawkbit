/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.cache.TenancyCacheManager;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.report.model.SystemUsageReport;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Central system management operations of the SP server.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
@Validated
@Service
public class SystemManagement {
    @Autowired
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
    private ExternalArtifactRepository externalArtifactRepository;

    @Autowired
    private LocalArtifactRepository artifactRepository;

    @Autowired
    private ExternalArtifactProviderRepository externalArtifactProviderRepository;

    @Autowired
    private TenantConfigurationRepository tenantConfigurationRepository;

    @Autowired
    private RolloutRepository rolloutRepository;

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private TenantAware tenantAware;

    @Autowired
    private TenantStatsManagement systemStatsManagement;

    @Autowired
    private TenancyCacheManager cacheManager;

    private final ThreadLocal<String> createInitialTenant = new ThreadLocal<>();

    /**
     * Calculated system usage statistics, both overall for the entire system
     * and per tenant;
     *
     * @return SystemUsageReport of the current system
     */
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN)
    public SystemUsageReport getSystemUsageStatistics() {

        BigDecimal sumOfArtifacts = (BigDecimal) entityManager
                .createNativeQuery(
                        "select SUM(file_size) from sp_artifact a INNER JOIN sp_base_software_module sm ON a.software_module = sm.id WHERE sm.deleted = 0")
                .getSingleResult();

        if (sumOfArtifacts == null) {
            sumOfArtifacts = new BigDecimal(0);
        }

        // we use native queries to punch through the tenant boundaries. This
        // has to be used with care!
        final Long targets = (Long) entityManager.createNativeQuery("SELECT COUNT(id) FROM sp_target")
                .getSingleResult();

        final Long artifacts = (Long) entityManager
                .createNativeQuery(
                        "SELECT COUNT(a.id) FROM sp_artifact a INNER JOIN sp_base_software_module sm ON a.software_module = sm.id WHERE sm.deleted = 0")
                .getSingleResult();

        final Long actions = (Long) entityManager.createNativeQuery("SELECT COUNT(id) FROM sp_action")
                .getSingleResult();

        final SystemUsageReport result = new SystemUsageReport(targets, artifacts, actions,
                sumOfArtifacts.setScale(0, BigDecimal.ROUND_HALF_UP).longValue());

        usageStatsPerTenant(result);

        return result;
    }

    private void usageStatsPerTenant(final SystemUsageReport report) {
        final List<String> tenants = findTenants();

        tenants.forEach(tenant -> tenantAware.runAsTenant(tenant, () -> {
            report.addTenantData(systemStatsManagement.getStatsOfTenant(tenant));
            return null;
        }));
    }

    /**
     * Registers the key generator for the {@link #currentTenant()} method
     * because this key generator is aware of the {@link #createInitialTenant}
     * thread local in case we are currently creating a tenant and insert the
     * default distribution set types.
     *
     * @return the {@link CurrentTenantKeyGenerator}
     */
    @Bean
    public CurrentTenantKeyGenerator currentTenantKeyGenerator() {
        return new CurrentTenantKeyGenerator();
    }

    /**
     * Returns {@link TenantMetaData} of given and current tenant.
     *
     * DISCLAIMER: this variant is used during initial login (where the tenant
     * is not yet in the session). Please user {@link #getTenantMetadata()} for
     * regular requests.
     *
     * @param tenant
     * @return
     */
    @Cacheable(value = "tenantMetadata", key = "#tenant.toUpperCase()")
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    @NotNull
    public TenantMetaData getTenantMetadata(@NotNull final String tenant) {
        final TenantMetaData result = tenantMetaDataRepository.findByTenantIgnoreCase(tenant);

        // Create if it does not exist
        if (result == null) {
            try {
                createInitialTenant.set(tenant);
                cacheManager.getCache("currentTenant").evict(currentTenantKeyGenerator().generate(null, null));
                return tenantMetaDataRepository.save(new TenantMetaData(createStandardSoftwareDataSetup(), tenant));
            } finally {
                createInitialTenant.remove();
            }
        }

        return result;
    }

    /**
     *
     * @return list of all tenant names in the system.
     */
    @NotNull
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_SYSTEM_CODE)
    // tenant independent
    public List<String> findTenants() {
        return tenantMetaDataRepository.findAll().stream().map(md -> md.getTenant()).collect(Collectors.toList());
    }

    /**
     * Deletes all data related to a given tenant.
     *
     * @param tenant
     *            to delete
     */
    @CacheEvict(value = { "tenantMetadata" }, key = "#tenant.toUpperCase()")
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN)
    // tenant independent
    public void deleteTenant(@NotNull final String tenant) {
        cacheManager.evictCaches(tenant);
        cacheManager.getCache("currentTenant").evict(currentTenantKeyGenerator().generate(null, null));
        tenantAware.runAsTenant(tenant, () -> {
            entityManager.setProperty(PersistenceUnitProperties.MULTITENANT_PROPERTY_DEFAULT, tenant.toUpperCase());
            tenantMetaDataRepository.deleteByTenantIgnoreCase(tenant);
            tenantConfigurationRepository.deleteByTenantIgnoreCase(tenant);
            targetRepository.deleteByTenantIgnoreCase(tenant);
            actionRepository.deleteByTenantIgnoreCase(tenant);
            rolloutGroupRepository.deleteByTenantIgnoreCase(tenant);
            rolloutRepository.deleteByTenantIgnoreCase(tenant);
            artifactRepository.deleteByTenantIgnoreCase(tenant);
            externalArtifactRepository.deleteByTenantIgnoreCase(tenant);
            externalArtifactProviderRepository.deleteByTenantIgnoreCase(tenant);
            targetTagRepository.deleteByTenantIgnoreCase(tenant);
            distributionSetTagRepository.deleteByTenantIgnoreCase(tenant);
            distributionSetRepository.deleteByTenantIgnoreCase(tenant);
            distributionSetTypeRepository.deleteByTenantIgnoreCase(tenant);
            softwareModuleRepository.deleteByTenantIgnoreCase(tenant);
            softwareModuleTypeRepository.deleteByTenantIgnoreCase(tenant);
            return null;
        });
    }

    /**
     * @return {@link TenantMetaData} of {@link TenantAware#getCurrentTenant()}
     */
    @Cacheable(value = "tenantMetadata", keyGenerator = "tenantKeyGenerator")
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    @NotNull
    public TenantMetaData getTenantMetadata() {
        if (tenantAware.getCurrentTenant() == null) {
            throw new IllegalStateException("Tenant not set");
        }

        return getTenantMetadata(tenantAware.getCurrentTenant());
    }

    /**
     * Checks if a specific tenant exists. The tenant will not be created lazy.
     *
     * @param tenant
     *            the tenant to check
     * @return {@code true} in case the tenant exits or {@code false} if not
     */
    @Cacheable(value = "currentTenant", keyGenerator = "currentTenantKeyGenerator")
    // set transaction to not supported, due we call this in
    // BaseEntity#prePersist methods
    // and it seems that JPA committing the transaction when executing this
    // transactional method,
    // which then leads that the BaseEntity#prePersist is called again to
    // persist the un-persisted
    // entity and we landing again in the #currentTenant() method
    // suspend the transaction here to do a read-request against the medata
    // table, when the current
    // tenant is not cached anyway already.
    @Transactional(propagation = Propagation.NOT_SUPPORTED, isolation = Isolation.READ_UNCOMMITTED)
    public String currentTenant() {
        final String initialTenantCreation = createInitialTenant.get();
        if (initialTenantCreation == null) {
            final TenantMetaData findByTenant = tenantMetaDataRepository
                    .findByTenantIgnoreCase(tenantAware.getCurrentTenant());
            return findByTenant != null ? findByTenant.getTenant() : null;
        }
        return initialTenantCreation;
    }

    /**
     * Update call for {@link TenantMetaData}.
     *
     * @param metaData
     *            to update
     * @return updated {@link TenantMetaData} entity
     */
    @CachePut(value = "tenantMetadata", key = "#metaData.tenant.toUpperCase()")
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    @NotNull
    public TenantMetaData updateTenantMetadata(@NotNull final TenantMetaData metaData) {
        if (!tenantMetaDataRepository.exists(metaData.getId())) {
            throw new EntityNotFoundException("Metadata does not exist: " + metaData.getId());
        }

        return tenantMetaDataRepository.save(metaData);
    }

    private DistributionSetType createStandardSoftwareDataSetup() {
        final SoftwareModuleType eclApp = softwareModuleTypeRepository.save(new SoftwareModuleType("application",
                "ECL Application", "Edge Controller Linux base application type", 1));
        final SoftwareModuleType eclOs = softwareModuleTypeRepository
                .save(new SoftwareModuleType("os", "ECL OS", "Edge Controller Linux operation system image type", 1));
        final SoftwareModuleType eclJvm = softwareModuleTypeRepository.save(
                new SoftwareModuleType("runtime", "ECL JVM", "Edge Controller Linux java virtual machine type.", 1));

        distributionSetTypeRepository.save(
                new DistributionSetType("ecl_os", "OS only", "Standard Edge Controller Linux distribution set type.")
                        .addMandatoryModuleType(eclOs));

        distributionSetTypeRepository.save(new DistributionSetType("ecl_os_app", "OS with optional app",
                "Standard Edge Controller Linux distribution set type. OS only.").addMandatoryModuleType(eclOs)
                        .addOptionalModuleType(eclApp));

        return distributionSetTypeRepository
                .save(new DistributionSetType("ecl_os_app_jvm", "OS with optional app and jvm",
                        "Standard Edge Controller Linux distribution set type. OS with optional application.")
                                .addMandatoryModuleType(eclOs).addOptionalModuleType(eclApp)
                                .addOptionalModuleType(eclJvm));
    }

    /**
     * A implementation of the {@link KeyGenerator} to generate a key based on
     * either the {@code createInitialTenant} thread local and the
     * {@link TenantAware}, but in case we are in a tenant creation with its
     * default types we need to use the tenant the current tenant which is
     * currently created and not the one currently in the {@link TenantAware}.
     *
     */
    private class CurrentTenantKeyGenerator implements KeyGenerator {
        @Override
        public Object generate(final Object target, final Method method, final Object... params) {
            final String initialTenantCreation = createInitialTenant.get();
            if (initialTenantCreation == null) {
                return SimpleKeyGenerator.generateKey(tenantAware.getCurrentTenant().toUpperCase(),
                        tenantAware.getCurrentTenant().toUpperCase());
            }
            return SimpleKeyGenerator.generateKey(initialTenantCreation.toUpperCase(),
                    initialTenantCreation.toUpperCase());
        }
    }
}
