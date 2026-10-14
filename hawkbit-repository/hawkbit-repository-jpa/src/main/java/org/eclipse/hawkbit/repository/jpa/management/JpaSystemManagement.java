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

import static org.eclipse.hawkbit.context.AccessContext.asSystemAsTenant;
import static org.eclipse.hawkbit.context.AccessContext.asTenant;

import java.util.Locale;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.persistence.EntityManager;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.artifact.ArtifactStorage;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.TenantSoftDeletedException;
import org.eclipse.hawkbit.repository.helper.SystemManagementHolder;
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
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager;
import org.eclipse.hawkbit.tenancy.TenantAwareCacheManager.CacheEvictEvent;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.resilience.annotation.Retryable;
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
public class JpaSystemManagement implements CurrentTenantCacheKeyGenerator, SystemManagement, ApplicationListener<ContextRefreshedEvent> {

    private static final int MAX_TENANTS_QUERY = 1000;
    private static final String CACHE_TENANT_SOFT_DELETED = "tenant-soft-deleted";

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
        // fail fast (the application doesn't boot)
        if (repositoryProperties.getTenantHardDeleteAfter().isPositive()
                && repositoryProperties.getTenantSoftDeleteAfter().compareTo(repositoryProperties.getTenantHardDeleteAfter()) > 0) {
            throw new IllegalStateException(String.format(
                    "Invalid tenant soft delete configuration: soft delete after (%s) must not be greater than hard delete after (%s)",
                    repositoryProperties.getTenantSoftDeleteAfter(), repositoryProperties.getTenantHardDeleteAfter()));
        }
    }

    @Autowired(required = false) // it's not required on dmf/ddi only instances
    public void setArtifactStorage(final ArtifactStorage artifactStorage) {
        this.artifactStorage = artifactStorage;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public void onApplicationEvent(@NonNull final ContextRefreshedEvent event) {
        // sets the proxy / bean from the context in order to be used via proxy and honor things like @PreAuthorize and @Transactional
        SystemManagementHolder.setInstance(event.getApplicationContext().getBean(JpaSystemManagement.class));
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public KeyGenerator currentTenantKeyGenerator() {
        return currentTenantCacheKeyGenerator.currentTenantKeyGenerator();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void forEachTenantAsSystem(final Consumer<String> consumer) {
        forEachTenantAsSystem(false, consumer);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    // Exception squid:S2229 - calling findTenants without transaction is intended in this case
    @SuppressWarnings("squid:S2229")
    public void forEachTenantAsSystem(final boolean includeSoftDeleted, final Consumer<String> consumer) {
        Page<String> tenants;
        Pageable query = PageRequest.of(0, MAX_TENANTS_QUERY);
        // same time for all pages - consistent paging
        final long now = System.currentTimeMillis();
        do {
            // with IS_SYSTEM_CODE so we could find all tenants
            tenants = includeSoftDeleted ? tenantMetaDataRepository.findAllTenants(query) : tenantMetaDataRepository.findActiveTenants(now, query);
            tenants.forEach(tenant -> asSystemAsTenant(tenant, () -> {
                try {
                    consumer.accept(tenant);
                } catch (final RuntimeException ex) {
                    log.error("Exception on forEachTenant execution for tenant {} with error message [{}]. Continue with next tenant.",
                            tenant, ex.getMessage());
                }
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
    public TenantMetaData createTenantMetadata(final String t) {
        // the tenant identity is the trimmed upper case name - " acme" and "ACME" are the same tenant
        final String tenant = normalizeTenant(t);
        final String storedExactTenant = tenantMetaDataRepository.findStoredTenantIgnoreCase(tenant);
        // Create if it does not exist
        if (storedExactTenant == null) {
            return createTenantMetadata0(tenant);
        }
        // an existing (legacy) tenant stored not normalized (another case / surrounding spaces) is a conflict
        if (!storedExactTenant.equals(tenant)) {
            throw new EntityAlreadyExistsException("Tenant `" + tenant + "` conflicts with existing tenant `" + storedExactTenant + "`");
        }
        // return existing - unless soft deleted
        final JpaTenantMetaData existing = (JpaTenantMetaData) tenantMetaDataRepository.findByTenantIgnoreCase(tenant);
        if (isSoftDeleted(existing.getDeletedAt(), System.currentTimeMillis())) {
            throw new EntityAlreadyExistsException("Tenant {" + tenant + "} exists and is soft deleted.");
        }
        return existing;
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public TenantMetaData updateTenantMetadata(final long defaultDsType) {
        final JpaTenantMetaData data = (JpaTenantMetaData) getTenantMetadataWithoutDetails();
        data.setDefaultDsType(distributionSetTypeRepository.getById(defaultDsType));
        return tenantMetaDataRepository.save(data);
    }

    @Override
    public boolean isTenantSoftDeleted() {
        final String tenant = AccessContext.tenant();
        if (tenant == null) {
            throw new IllegalStateException("AccessContext not set");
        }

        // caches the deletedAt (not the result) - so a scheduled soft delete takes effect on time even with a warm cache.
        // Optional since the tenant aware cache doesn't cache null values
        final Optional<Long> deletedAt = TenantAwareCacheManager.getInstance()
                .getCache(CACHE_TENANT_SOFT_DELETED)
                .get(tenant, () -> Optional.ofNullable((JpaTenantMetaData) tenantMetaDataRepository.findByTenantIgnoreCase(tenant))
                        .map(JpaTenantMetaData::getDeletedAt));
        return isSoftDeleted(deletedAt.orElse(null), System.currentTimeMillis());
    }

    @Override
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public void deleteTenant(final String t) {
        if (artifactStorage == null) {
            throw new IllegalStateException("Artifact repository is not available. Can't delete tenant.");
        }

        final String tenant = normalizeTenant(t);
        asTenant(tenant, () -> DeploymentHelper.runInNewTransaction("deleteTenant", status -> {
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
        }, txManager));
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new CacheEvictEvent.Default(tenant, null, null));
    }

    @Override
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public void softDeleteTenant(final String t) {
        if (!repositoryProperties.getTenantHardDeleteAfter().isPositive()) {
            throw new IllegalStateException("Soft deletes are not enabled - hard delete after is not positive!");
        }
        // both soft and hard delete times are counted from now (the request time)
        final Instant now = Instant.now();
        softDeleteTenant(
                t,
                repositoryProperties.getTenantSoftDeleteAfter().isPositive()
                        ? now.plusMillis(repositoryProperties.getTenantSoftDeleteAfter().toMillis())
                        : now,
                now.plusMillis(repositoryProperties.getTenantHardDeleteAfter().toMillis()));
    }

    @Override
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public void softDeleteTenant(final String t, final Instant softDeleteAt, final Instant hardDeleteAt) {
        // keeps the invariant hardDeleteAt >= deletedAt - the hard delete scheduler relies on it to never pick a pending soft delete
        if (hardDeleteAt.isBefore(softDeleteAt)) {
            throw new IllegalArgumentException(
                    "Hard delete time " + hardDeleteAt + " must not be before the soft delete time " + softDeleteAt + "!");
        }
        final String tenant = normalizeTenant(t);
        final String actor = AccessContext.actor();
        // runs in the tenant context (as the hard delete) - the persistence context needs a tenant, e.g. when called as system
        asTenant(tenant, () -> DeploymentHelper.runInNewTransaction("softDeleteTenant", status -> {
            final JpaTenantMetaData metaData = findTenantMetaData(tenant);
            final long now = System.currentTimeMillis();
            // an already soft deleted tenant can't be soft deleted again (keeps its marks), an active or pending one is (re)scheduled
            if (isSoftDeleted(metaData.getDeletedAt(), now)) {
                throw new IllegalStateException("Tenant {" + tenant + "} is softDeleted.");
            }
            metaData.setDeletedAt(softDeleteAt.toEpochMilli());
            metaData.setDeletedBy(actor);
            metaData.setHardDeleteAt(hardDeleteAt.toEpochMilli());
            tenantMetaDataRepository.save(metaData);
            return null;
        }, txManager));
        evictTenantCachesLocally(tenant);
    }

    @Override
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public void restoreTenant(final String t) {
        final String tenant = normalizeTenant(t);
        // runs in the tenant context (as the hard delete) - the persistence context needs a tenant, e.g. when called as system
        asTenant(tenant, () -> DeploymentHelper.runInNewTransaction("restoreTenant", status -> {
            final JpaTenantMetaData metaData = findTenantMetaData(tenant);
            // active tenant - nothing to change
            if (metaData.getDeletedAt() != null || metaData.getDeletedBy() != null || metaData.getHardDeleteAt() != null) {
                metaData.setDeletedAt(null);
                metaData.setDeletedBy(null);
                metaData.setHardDeleteAt(null);
                tenantMetaDataRepository.save(metaData);
            }
            return null;
        }, txManager));
        evictTenantCachesLocally(tenant);
    }

    // Evicts the tenant's caches on this node synchronously (after the commit), so the own node sees a soft delete / restore at
    // once. Publishing the CacheEvictEvent is not enough for that - the application event multicaster dispatches listeners
    // asynchronously, so a check right after the call could still read the stale cached value.
    private static void evictTenantCachesLocally(final String tenant) {
        TenantAwareCacheManager.getInstance().onCacheEvictEvent(new CacheEvictEvent.Default(tenant, null, null));
    }

    public static String normalizeTenant(final String tenant) {
        final String normalizedTenant = tenant.trim().toUpperCase(Locale.ROOT);
        if (normalizedTenant.isEmpty()) {
            throw new IllegalArgumentException("Tenant must not be empty or blank");
        }
        return normalizedTenant;
    }

    private static boolean isSoftDeleted(final Long deletedAt, final long now) {
        return deletedAt != null && deletedAt <= now;
    }

    private JpaTenantMetaData findTenantMetaData(final String tenant) {
        final JpaTenantMetaData metaData = (JpaTenantMetaData) tenantMetaDataRepository.findByTenantIgnoreCase(tenant);
        if (metaData == null) {
            throw new EntityNotFoundException(TenantMetaData.class, tenant);
        }
        return metaData;
    }

    private TenantMetaData getTenantMetadata0(final boolean withDetails) {
        final String tenant = AccessContext.tenant();
        if (tenant == null) {
            throw new IllegalStateException("AccessContext not set");
        }

        final TenantMetaData metaData = withDetails
                ? tenantMetaDataRepository.findWitDetailsByTenantIgnoreCase(tenant)
                : tenantMetaDataRepository.findByTenantIgnoreCase(tenant);
        if (metaData != null && isSoftDeleted(((JpaTenantMetaData) metaData).getDeletedAt(), System.currentTimeMillis())) {
            // checked before the implicit creation, so a soft deleted tenant is never re-created
            throw new TenantSoftDeletedException(tenant);
        } else if (metaData == null) {
            if (repositoryProperties.isImplicitTenantCreateAllowed()) {
                log.info("AccessContext {} doesn't exist create metadata", tenant, new Exception("Thread dump"));
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
        return asSystemAsTenant(tenant, () -> DeploymentHelper.runInNewTransaction("initial-tenant-creation", status -> {
            final DistributionSetType defaultDsType = createStandardSoftwareDataSetup();
            return tenantMetaDataRepository.save(new JpaTenantMetaData(tenant, defaultDsType));
        }, txManager));
    }
}
