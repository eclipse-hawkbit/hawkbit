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

import java.util.Set;
import java.util.function.Consumer;

import jakarta.persistence.EntityManager;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.artifact.ArtifactStorage;
import org.eclipse.hawkbit.context.System;
import org.eclipse.hawkbit.context.Tenant;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
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
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager.CacheEvictEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
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
@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "system-management" }, matchIfMissing = true)
public class JpaSystemManagement implements CurrentTenantCacheKeyGenerator, SystemManagement {

    private static final int MAX_TENANTS_QUERY = 1000;

    private final TargetRepository targetRepository;
    private final TargetTypeRepository targetTypeRepository;
    private final TargetTagRepository targetTagRepository;
    private final TargetFilterQueryRepository targetFilterQueryRepository;
    private final SoftwareModuleRepository softwareModuleRepository;
    private final SoftwareModuleTypeRepository softwareModuleTypeRepository;
    private final DistributionSetRepository distributionSetRepository;
    private final DistributionSetTypeRepository distributionSetTypeRepository;
    private final DistributionSetTagRepository distributionSetTagRepository;
    private final RolloutRepository rolloutRepository;
    private final TenantConfigurationRepository tenantConfigurationRepository;
    private final TenantMetaDataRepository tenantMetaDataRepository;
    private final SystemManagementCacheKeyGenerator currentTenantCacheKeyGenerator;
    private final PlatformTransactionManager txManager;
    private final EntityManager entityManager;
    private final RepositoryProperties repositoryProperties;

    @Nullable
    private ArtifactStorage artifactStorage;

    @SuppressWarnings("squid:S00107")
    protected JpaSystemManagement(
            final TargetRepository targetRepository, final TargetTypeRepository targetTypeRepository,
            final TargetTagRepository targetTagRepository, final TargetFilterQueryRepository targetFilterQueryRepository,
            final SoftwareModuleRepository softwareModuleRepository, final SoftwareModuleTypeRepository softwareModuleTypeRepository,
            final DistributionSetRepository distributionSetRepository, final DistributionSetTypeRepository distributionSetTypeRepository,
            final DistributionSetTagRepository distributionSetTagRepository, final RolloutRepository rolloutRepository,
            final TenantConfigurationRepository tenantConfigurationRepository, final TenantMetaDataRepository tenantMetaDataRepository,
            final SystemManagementCacheKeyGenerator currentTenantCacheKeyGenerator,
            final PlatformTransactionManager txManager,
            final EntityManager entityManager, final RepositoryProperties repositoryProperties,
            final JpaProperties properties) {
        this.targetRepository = targetRepository;
        this.targetTypeRepository = targetTypeRepository;
        this.targetTagRepository = targetTagRepository;
        this.targetFilterQueryRepository = targetFilterQueryRepository;
        this.softwareModuleRepository = softwareModuleRepository;
        this.softwareModuleTypeRepository = softwareModuleTypeRepository;
        this.distributionSetRepository = distributionSetRepository;
        this.distributionSetTypeRepository = distributionSetTypeRepository;
        this.distributionSetTagRepository = distributionSetTagRepository;
        this.rolloutRepository = rolloutRepository;
        this.tenantConfigurationRepository = tenantConfigurationRepository;
        this.tenantMetaDataRepository = tenantMetaDataRepository;
        this.currentTenantCacheKeyGenerator = currentTenantCacheKeyGenerator;
        this.txManager = txManager;
        this.entityManager = entityManager;
        this.repositoryProperties = repositoryProperties;
    }

    @Autowired(required = false) // it's not required on dmf/ddi only instances
    public void setArtifactStorage(final ArtifactStorage artifactStorage) {
        this.artifactStorage = artifactStorage;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public KeyGenerator currentTenantKeyGenerator() {
        return currentTenantCacheKeyGenerator.currentTenantKeyGenerator();
    }

    @Override
    public Page<String> findTenants(final Pageable pageable) {
        return tenantMetaDataRepository.findTenants(pageable);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    // Exception squid:S2229 - calling findTenants without transaction is intended in this case
    @SuppressWarnings("squid:S2229")
    public void forEachTenant(final Consumer<String> consumer) {
        Page<String> tenants;
        Pageable query = PageRequest.of(0, MAX_TENANTS_QUERY);
        do {
            tenants = findTenants(query); // its with IS_SYSTEM_CODE so we could find all tenants
            tenants.forEach(tenant -> System.asSystemAsTenant(tenant, () -> {
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
    public TenantMetaData getTenantMetadata() {
        return getTenantMetadata0(true);
    }

    @Override
    public TenantMetaData getTenantMetadataWithoutDetails() {
        return getTenantMetadata0(false);
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
        final JpaTenantMetaData data = (JpaTenantMetaData) getTenantMetadataWithoutDetails();
        data.setDefaultDsType(distributionSetTypeRepository.getById(defaultDsType));
        return tenantMetaDataRepository.save(data);
    }

    @Override
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteTenant(final String t) {
        if (artifactStorage == null) {
            throw new IllegalStateException("Artifact repository is not available. Can't delete tenant.");
        }

        final String tenant = t.toUpperCase();
        System.asSystemAsTenant(tenant, () -> DeploymentHelper.runInNewTransaction(txManager, "deleteTenant", status -> {
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
            artifactStorage.deleteByTenant(tenant);
            softwareModuleTypeRepository.deleteByTenant(tenant);
            return null;
        }));
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new CacheEvictEvent.Default(tenant, null, null));
    }

    private TenantMetaData getTenantMetadata0(final boolean withDetails) {
        final String tenant = Tenant.currentTenant();
        if (tenant == null) {
            throw new IllegalStateException("Tenant not set");
        }

        final TenantMetaData metaData = withDetails
                ? tenantMetaDataRepository.findWitDetailsByTenantIgnoreCase(tenant)
                : tenantMetaDataRepository.findByTenantIgnoreCase(tenant);
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

    private DistributionSetType createStandardSoftwareDataSetup() {
        final SoftwareModuleType app = softwareModuleTypeRepository.save(
                new JpaSoftwareModuleType(
                        org.eclipse.hawkbit.repository.Constants.SMT_DEFAULT_APP_KEY,
                        org.eclipse.hawkbit.repository.Constants.SMT_DEFAULT_APP_NAME,
                        "Application Addons", Integer.MAX_VALUE));
        final SoftwareModuleType os = softwareModuleTypeRepository.save(
                new JpaSoftwareModuleType(
                        org.eclipse.hawkbit.repository.Constants.SMT_DEFAULT_OS_KEY,
                        org.eclipse.hawkbit.repository.Constants.SMT_DEFAULT_OS_NAME,
                        "Core firmware or operating system", 1));

        // make sure the module types get their IDs
        entityManager.flush();

        distributionSetTypeRepository
                .save(new JpaDistributionSetType(org.eclipse.hawkbit.repository.Constants.DST_DEFAULT_OS_ONLY_KEY,
                        org.eclipse.hawkbit.repository.Constants.DST_DEFAULT_OS_ONLY_NAME,
                        "Default type with Firmware/OS only.").setMandatoryModuleTypes(Set.of(os)));

        distributionSetTypeRepository
                .save(new JpaDistributionSetType(org.eclipse.hawkbit.repository.Constants.DST_DEFAULT_APP_ONLY_KEY,
                        org.eclipse.hawkbit.repository.Constants.DST_DEFAULT_APP_ONLY_NAME,
                        "Default type with app(s) only.").setMandatoryModuleTypes(Set.of(app)));

        return distributionSetTypeRepository
                .save(new JpaDistributionSetType(org.eclipse.hawkbit.repository.Constants.DST_DEFAULT_OS_WITH_APPS_KEY,
                        org.eclipse.hawkbit.repository.Constants.DST_DEFAULT_OS_WITH_APPS_NAME,
                        "Default type with Firmware/OS and optional app(s).")
                        .setMandatoryModuleTypes(Set.of(os))
                        .setOptionalModuleTypes(Set.of(app)));
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
     * Creating the initial tenant meta-data in a new transaction. Due to the tenant support it is using the current tenant to
     * set the necessary tenant discriminator to the query. This is not working if we don't have a current tenant set.
     * Due to the {@link #createTenantMetadata(String)} is maybe called without having a
     * current tenant we need to re-open a new transaction so the tenant support is called again and set the
     * tenant for this transaction.
     *
     * @param tenant the tenant to be created
     * @return the initial created {@link TenantMetaData}
     */
    private TenantMetaData createInitialTenantMetaData(final String tenant) {
        return System.asSystemAsTenant(
                tenant, () -> DeploymentHelper.runInNewTransaction(txManager, "initial-tenant-creation", status -> {
                    final DistributionSetType defaultDsType = createStandardSoftwareDataSetup();
                    return tenantMetaDataRepository.save(new JpaTenantMetaData(defaultDsType, tenant));
                }));
    }
}
