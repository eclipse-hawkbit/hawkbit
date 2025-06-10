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

import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.IMPLICIT_LOCK_ENABLED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.builder.GenericDistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.DeletedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.builder.JpaDistributionSetCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetFilterQueryRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Statistic;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link DistributionSetManagement}.
 */
@Transactional(readOnly = true)
@Validated
public class JpaDistributionSetManagement implements DistributionSetManagement {

    private final EntityManager entityManager;
    private final DistributionSetRepository distributionSetRepository;
    private final DistributionSetTagManagement distributionSetTagManagement;
    private final SystemManagement systemManagement;
    private final DistributionSetTypeManagement distributionSetTypeManagement;
    private final QuotaManagement quotaManagement;
    private final TargetRepository targetRepository;
    private final TargetFilterQueryRepository targetFilterQueryRepository;
    private final ActionRepository actionRepository;
    private final TenantConfigHelper tenantConfigHelper;
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final SoftwareModuleRepository softwareModuleRepository;
    private final DistributionSetTagRepository distributionSetTagRepository;
    private final Database database;
    private final RepositoryProperties repositoryProperties;

    @SuppressWarnings("java:S107")
    public JpaDistributionSetManagement(
            final EntityManager entityManager,
            final DistributionSetRepository distributionSetRepository,
            final DistributionSetTagManagement distributionSetTagManagement, final SystemManagement systemManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement, final QuotaManagement quotaManagement,
            final TargetRepository targetRepository,
            final TargetFilterQueryRepository targetFilterQueryRepository, final ActionRepository actionRepository,
            final TenantConfigHelper tenantConfigHelper,
            final VirtualPropertyReplacer virtualPropertyReplacer,
            final SoftwareModuleRepository softwareModuleRepository,
            final DistributionSetTagRepository distributionSetTagRepository,
            final Database database,
            final RepositoryProperties repositoryProperties) {
        this.entityManager = entityManager;
        this.distributionSetRepository = distributionSetRepository;
        this.distributionSetTagManagement = distributionSetTagManagement;
        this.systemManagement = systemManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.quotaManagement = quotaManagement;
        this.targetRepository = targetRepository;
        this.targetFilterQueryRepository = targetFilterQueryRepository;
        this.actionRepository = actionRepository;
        this.tenantConfigHelper = tenantConfigHelper;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.softwareModuleRepository = softwareModuleRepository;
        this.distributionSetTagRepository = distributionSetTagRepository;
        this.database = database;
        this.repositoryProperties = repositoryProperties;
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSet> create(final Collection<DistributionSetCreate> creates) {
        final List<JpaDistributionSet> toCreate = creates.stream().map(JpaDistributionSetCreate.class::cast)
                .map(this::setDefaultTypeIfMissing)
                .map(JpaDistributionSetCreate::build)
                .toList();
        return Collections.unmodifiableList(distributionSetRepository.saveAll(AccessController.Operation.CREATE, toCreate));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet create(final DistributionSetCreate c) {
        final JpaDistributionSetCreate create = (JpaDistributionSetCreate) c;
        setDefaultTypeIfMissing(create);
        return distributionSetRepository.save(AccessController.Operation.CREATE, create.build());
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    @SuppressWarnings("java:S1066") // javaS1066 - better readable that way
    public DistributionSet update(final DistributionSetUpdate u) {
        final GenericDistributionSetUpdate update = (GenericDistributionSetUpdate) u;

        final JpaDistributionSet set = (JpaDistributionSet) getValid(update.getId());

        update.getName().ifPresent(set::setName);
        update.getDescription().ifPresent(set::setDescription);
        update.getVersion().ifPresent(set::setVersion);

        // lock/unlock ONLY if locked flag is present!
        if (Boolean.TRUE.equals(update.locked())) {
            if (!set.isLocked()) {
                lockSoftwareModules(set);
                set.lock();
            }
        } else if (Boolean.FALSE.equals(update.locked())) {
            if (set.isLocked()) {
                set.unlock();
            }
        }

        if (update.isRequiredMigrationStep() != null
                && !update.isRequiredMigrationStep().equals(set.isRequiredMigrationStep())) {
            assertDistributionSetIsNotAssignedToTargets(update.getId());
            set.setRequiredMigrationStep(update.isRequiredMigrationStep());
        }

        return distributionSetRepository.save(set);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long id) {
        delete0(List.of(id));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final Collection<Long> distributionSetIDs) {
        delete0(distributionSetIDs);
    }
    private void delete0(final Collection<Long> distributionSetIDs) {
        getDistributionSets(distributionSetIDs); // throws EntityNotFoundException if any of these do not exists
        final List<JpaDistributionSet> setsFound = distributionSetRepository.findAll(
                AccessController.Operation.DELETE, distributionSetRepository.byIdsSpec(distributionSetIDs));
        if (setsFound.size() < distributionSetIDs.size()) {
            throw new InsufficientPermissionException("No DELETE access to some of distribution sets!");
        }

        final List<Long> assigned = distributionSetRepository
                .findAssignedToTargetDistributionSetsById(distributionSetIDs);
        assigned.addAll(distributionSetRepository.findAssignedToRolloutDistributionSetsById(distributionSetIDs));

        // soft delete assigned
        if (!assigned.isEmpty()) {
            distributionSetRepository.saveAll(
                    setsFound.stream()
                            .filter(set -> assigned.contains(set.getId()))
                            .map(toSoftDelete -> {
                                // don't use peek since it is by documentation mainly for debugging and could be skipped in some cases
                                toSoftDelete.setDeleted(true);
                                return toSoftDelete;
                            })
                            .toList());
            targetFilterQueryRepository.unsetAutoAssignDistributionSetAndActionTypeAndAccessContext(assigned.toArray(new Long[0]));
        }

        // mark the rest as hard delete
        final List<Long> toHardDelete = distributionSetIDs.stream().filter(setId -> !assigned.contains(setId)).toList();

        // hard delete the rest if exists
        if (!toHardDelete.isEmpty()) {
            targetFilterQueryRepository.unsetAutoAssignDistributionSetAndActionTypeAndAccessContext(
                    toHardDelete.toArray(new Long[0]));
            // don't give the delete statement an empty list, JPA/Oracle cannot
            // handle the empty list
            distributionSetRepository.deleteAllById(toHardDelete);
        }
    }

    @Override
    public Optional<DistributionSet> get(final long id) {
        return distributionSetRepository.findById(id).map(DistributionSet.class::cast);
    }

    @Override
    public List<DistributionSet> get(final Collection<Long> ids) {
        return Collections.unmodifiableList(getDistributionSets(ids));
    }

    @Override
    public boolean exists(final long id) {
        return distributionSetRepository.existsById(id);
    }

    @Override
    public long count() {
        return distributionSetRepository.count(DistributionSetSpecification.isNotDeleted());
    }

    @Override
    public Slice<DistributionSet> findAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, List.of(
                DistributionSetSpecification.isNotDeleted()), pageable);
    }

    @Override
    public Page<DistributionSet> findByRsql(final String rsqlParam, final Pageable pageable) {
        return JpaManagementHelper.findAllWithCountBySpec(distributionSetRepository, List.of(
                RSQLUtility.buildRsqlSpecification(rsqlParam, DistributionSetFields.class, virtualPropertyReplacer, database),
                DistributionSetSpecification.isNotDeleted()), pageable);
    }

    @Override
    public Optional<DistributionSet> getWithDetails(final long id) {
        return distributionSetRepository
                .findOne(distributionSetRepository.byIdSpec(id), JpaDistributionSet_.GRAPH_DISTRIBUTION_SET_DETAIL)
                .map(DistributionSet.class::cast);
    }

    @Override
    @Transactional
    public void invalidate(final DistributionSet distributionSet) {
        final JpaDistributionSet jpaSet = (JpaDistributionSet) distributionSet;
        jpaSet.invalidate();
        distributionSetRepository.save(jpaSet);
    }

    @Override
    public DistributionSet getOrElseThrowException(final long id) {
        return getById(id);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet assignSoftwareModules(final long id, final Collection<Long> softwareModuleId) {
        final JpaDistributionSet set = (JpaDistributionSet) getValid(id);
        assertDistributionSetIsNotAssignedToTargets(id);
        assertSoftwareModuleQuota(id, softwareModuleId.size());

        final Collection<JpaSoftwareModule> modules = softwareModuleRepository.findAllById(softwareModuleId);
        if (modules.size() < softwareModuleId.size()) {
            throw new EntityNotFoundException(SoftwareModule.class, softwareModuleId,
                    modules.stream().map(SoftwareModule::getId).toList());
        }

        modules.forEach(set::addModule);

        return distributionSetRepository.save(set);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DistributionSet unassignSoftwareModule(final long id, final long moduleId) {
        final JpaDistributionSet set = (JpaDistributionSet) getValid(id);
        assertDistributionSetIsNotAssignedToTargets(id);

        final JpaSoftwareModule module = findSoftwareModuleAndThrowExceptionIfNotFound(moduleId);
        set.removeModule(module);

        return distributionSetRepository.save(set);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSet> assignTag(final Collection<Long> ids, final long dsTagId) {
        return updateTag(ids, dsTagId, (tag, distributionSet) -> {
            if (distributionSet.getTags().contains(tag)) {
                return distributionSet;
            } else {
                distributionSet.addTag(tag);
                return distributionSetRepository.save(distributionSet);
            }
        });
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<DistributionSet> unassignTag(final Collection<Long> ids, final long dsTagId) {
        return updateTag(ids, dsTagId, (tag, distributionSet) -> {
            if (distributionSet.getTags().contains(tag)) {
                distributionSet.removeTag(tag);
                return distributionSetRepository.save(distributionSet);
            } else {
                return distributionSet;
            }
        });
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void createMetadata(final long id, final Map<String, String> md) {
        final JpaDistributionSet distributionSet = (JpaDistributionSet) getValid(id);

        // get the modifiable metadata map
        final Map<String, String> metadata = distributionSet.getMetadata();
        md.keySet().forEach(key -> {
            if (metadata.containsKey(key)) {
                throw new EntityAlreadyExistsException("Metadata entry with key '" + key + "' already exists");
            }
        });
        metadata.putAll(md);

        assertMetaDataQuota(id, metadata.size());

        distributionSetRepository.save(distributionSet);
    }

    @Override
    public Map<String, String> getMetadata(final long id) {
        assertDistributionSetExists(id);
        return getMap(id, JpaDistributionSet_.metadata);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void updateMetadata(final long id, final String key, final String value) {
        final JpaDistributionSet distributionSet = (JpaDistributionSet) getValid(id);

        // get the modifiable metadata map
        final Map<String, String> metadata = distributionSet.getMetadata();
        if (!metadata.containsKey(key)) {
            throw new EntityNotFoundException("DistributionSet metadata", id + ":" + key);
        }
        metadata.put(key, value);

        distributionSetRepository.save(distributionSet);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteMetadata(final long id, final String key) {
        final JpaDistributionSet distributionSet = (JpaDistributionSet) getValid(id);

        // get the modifiable metadata map
        final Map<String, String> metadata = distributionSet.getMetadata();
        if (metadata.remove(key) == null) {
            throw new EntityNotFoundException("DistributionSet metadata", id + ":" + key);
        }

        distributionSetRepository.save(distributionSet);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void lock(final long id) {
        final JpaDistributionSet distributionSet = getById(id);
        if (!distributionSet.isLocked()) {
            lockSoftwareModules(distributionSet);
            distributionSet.lock();
            distributionSetRepository.save(distributionSet);
        }
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void unlock(final long id) {
        final JpaDistributionSet distributionSet = getById(id);
        if (distributionSet.isLocked()) {
            distributionSet.unlock();
            distributionSetRepository.save(distributionSet);
        }
    }

    @Override
    public Optional<DistributionSet> findByAction(final long actionId) {
        return actionRepository
                .findById(actionId)
                .map(action -> {
                    if (!targetRepository.exists(TargetSpecifications.hasId(action.getTarget().getId()))) {
                        throw new InsufficientPermissionException("Target not accessible (or not found)!");
                    }
                    return distributionSetRepository
                            .findOne(DistributionSetSpecification.byIdFetch(action.getDistributionSet().getId()))
                            .orElseThrow(() ->
                                    new InsufficientPermissionException("DistributionSet not accessible (or not found)!"));
                })
                .map(DistributionSet.class::cast)
                .or(() -> {
                    throw new EntityNotFoundException(Action.class, actionId);
                });
    }

    @Override
    public Optional<DistributionSet> findByNameAndVersion(final String distributionName, final String version) {
        return distributionSetRepository
                .findOne(DistributionSetSpecification.equalsNameAndVersionIgnoreCase(distributionName, version))
                .map(DistributionSet.class::cast);

    }

    @Override
    public DistributionSet getValidAndComplete(final long id) {
        final DistributionSet distributionSet = getValid(id);

        if (!distributionSet.isComplete()) {
            throw new IncompleteDistributionSetException("Distribution set of type "
                    + distributionSet.getType().getKey() + " is incomplete: " + distributionSet.getId());
        }

        if (distributionSet.isDeleted()) {
            throw new DeletedException(DistributionSet.class, id);
        }

        return distributionSet;
    }

    @Override
    public DistributionSet getValid(final long id) {
        final DistributionSet distributionSet = getOrElseThrowException(id);

        if (!distributionSet.isValid()) {
            throw new InvalidDistributionSetException("Distribution set of type " + distributionSet.getType().getKey()
                    + " is invalid: " + distributionSet.getId());
        }

        return distributionSet;
    }

    @Override
    public Slice<DistributionSet> findByCompleted(final Pageable pageReq, final Boolean complete) {
        final List<Specification<JpaDistributionSet>> specifications = buildSpecsByComplete(complete);

        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, specifications, pageReq);
    }

    @Override
    public long countByCompleted(final Boolean complete) {
        final List<Specification<JpaDistributionSet>> specifications = buildSpecsByComplete(complete);

        return JpaManagementHelper.countBySpec(distributionSetRepository, specifications);
    }

    @Override
    public Slice<DistributionSet> findByDistributionSetFilter(final DistributionSetFilter distributionSetFilter, final Pageable pageable) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(
                distributionSetFilter);

        return JpaManagementHelper.findAllWithoutCountBySpec(distributionSetRepository, specList, pageable);
    }

    @Override
    public long countByDistributionSetFilter(@NotNull final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(distributionSetFilter);
        return JpaManagementHelper.countBySpec(distributionSetRepository, specList);
    }

    @Override
    public Page<DistributionSet> findByTag(final long tagId, final Pageable pageable) {
        assertDsTagExists(tagId);

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetRepository, List.of(
                DistributionSetSpecification.hasTag(tagId)), pageable);
    }

    @Override
    public Page<DistributionSet> findByRsqlAndTag(final String rsqlParam, final long tagId, final Pageable pageable) {
        assertDsTagExists(tagId);

        return JpaManagementHelper.findAllWithCountBySpec(distributionSetRepository, List.of(
                RSQLUtility.buildRsqlSpecification(rsqlParam, DistributionSetFields.class, virtualPropertyReplacer,
                        database),
                DistributionSetSpecification.hasTag(tagId), DistributionSetSpecification.isNotDeleted()), pageable);
    }

    @Override
    public long countByTypeId(final long typeId) {
        if (!distributionSetTypeManagement.exists(typeId)) {
            throw new EntityNotFoundException(DistributionSetType.class, typeId);
        }

        return distributionSetRepository.count(DistributionSetSpecification.byType(typeId));
    }

    @Override
    public boolean isInUse(final long id) {
        assertDistributionSetExists(id);

        return actionRepository.countByDistributionSetId(id) > 0;
    }

    @Override
    public List<Statistic> countRolloutsByStatusForDistributionSet(final Long id) {
        assertDistributionSetExists(id);

        return distributionSetRepository.countRolloutsByStatusForDistributionSet(id).stream().map(Statistic.class::cast).toList();
    }

    @Override
    public List<Statistic> countActionsByStatusForDistributionSet(final Long id) {
        assertDistributionSetExists(id);

        return distributionSetRepository.countActionsByStatusForDistributionSet(id).stream()
                .map(Statistic.class::cast).toList();
    }

    @Override
    public Long countAutoAssignmentsForDistributionSet(final Long id) {
        assertDistributionSetExists(id);

        return distributionSetRepository.countAutoAssignmentsForDistributionSet(id);
    }

    // check if it shall implicitly lock a distribution set
    boolean isImplicitLockApplicable(final DistributionSet distributionSet) {
        final JpaDistributionSet jpaDistributionSet = (JpaDistributionSet) distributionSet;
        if (jpaDistributionSet.isLocked()) {
            // already locked
            return false;
        }

        if (Boolean.FALSE.equals(tenantConfigHelper.getConfigValue(IMPLICIT_LOCK_ENABLED, Boolean.class))) {
            // implicit lock disabled
            return false;
        }

        final List<String> skipForTags = repositoryProperties.getSkipImplicitLockForTags();
        if (!ObjectUtils.isEmpty(skipForTags)) {
            final Set<DistributionSetTag> tags = jpaDistributionSet.getTags();
            if (!ObjectUtils.isEmpty(tags)) {
                for (final DistributionSetTag tag : tags) {
                    if (skipForTags.contains(tag.getName())) {
                        // has a skip tag
                        return false;
                    }
                }
            }
        }

        // finally - implicit lock is applicable
        return true;
    }

    private static List<Specification<JpaDistributionSet>> buildDistributionSetSpecifications(
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = new ArrayList<>(10);

        Specification<JpaDistributionSet> spec;

        if (distributionSetFilter.getIsComplete() != null) {
            spec = DistributionSetSpecification.isCompleted(distributionSetFilter.getIsComplete());
            specList.add(spec);
        }

        if (distributionSetFilter.getIsDeleted() != null) {
            spec = DistributionSetSpecification.isDeleted(distributionSetFilter.getIsDeleted());
            specList.add(spec);
        }

        if (distributionSetFilter.getIsValid() != null) {
            spec = DistributionSetSpecification.isValid(distributionSetFilter.getIsValid());
            specList.add(spec);
        }

        if (distributionSetFilter.getTypeId() != null) {
            spec = DistributionSetSpecification.byType(distributionSetFilter.getTypeId());
            specList.add(spec);
        }

        if (!ObjectUtils.isEmpty(distributionSetFilter.getSearchText())) {
            final String[] dsFilterNameAndVersionEntries = JpaManagementHelper
                    .getFilterNameAndVersionEntries(distributionSetFilter.getSearchText().trim());
            spec = DistributionSetSpecification.likeNameAndVersion(dsFilterNameAndVersionEntries[0],
                    dsFilterNameAndVersionEntries[1]);
            specList.add(spec);
        }

        if (hasTagsFilterActive(distributionSetFilter)) {
            spec = DistributionSetSpecification.hasTags(distributionSetFilter.getTagNames(),
                    distributionSetFilter.getSelectDSWithNoTag());
            specList.add(spec);
        }
        return specList;
    }

    private static boolean hasTagsFilterActive(final DistributionSetFilter distributionSetFilter) {
        final boolean isNoTagActive = Boolean.TRUE.equals(distributionSetFilter.getSelectDSWithNoTag());
        final boolean isAtLeastOneTagActive = !CollectionUtils.isEmpty(distributionSetFilter.getTagNames());

        return isNoTagActive || isAtLeastOneTagActive;
    }

    private static Collection<Long> notFound(final Collection<Long> distributionSetIds, final List<JpaDistributionSet> foundDistributionSets) {
        final Map<Long, JpaDistributionSet> foundDistributionSetMap = foundDistributionSets.stream()
                .collect(Collectors.toMap(JpaDistributionSet::getId, Function.identity()));
        return distributionSetIds.stream().filter(id -> !foundDistributionSetMap.containsKey(id)).toList();
    }

    private List<DistributionSet> updateTag(
            final Collection<Long> dsIds, final long dsTagId,
            final BiFunction<DistributionSetTag, JpaDistributionSet, DistributionSet> updater) {
        final DistributionSetTag tag = distributionSetTagManagement.get(dsTagId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, dsTagId));
        final List<JpaDistributionSet> allDs = dsIds.size() == 1 ?
                distributionSetRepository.findById(dsIds.iterator().next())
                        .map(List::of)
                        .orElseGet(Collections::emptyList) :
                distributionSetRepository.findAll(DistributionSetSpecification.byIdsFetch(dsIds));
        if (allDs.size() < dsIds.size()) {
            throw new EntityNotFoundException(DistributionSet.class, notFound(dsIds, allDs));
        }

        try {
            // apply update and collect modified targets
            return allDs.stream().map(distributionSet -> updater.apply(tag, distributionSet)).toList();
        } finally {
            // No reason to save the tag
            entityManager.detach(tag);
        }
    }

    private Map<String, String> getMap(final long id, final MapAttribute<JpaDistributionSet, String, String> mapAttribute) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);

        final Root<JpaDistributionSet> targetRoot = query.from(JpaDistributionSet.class);
        query.where(cb.equal(targetRoot.get(AbstractJpaBaseEntity_.ID), id));

        final MapJoin<JpaDistributionSet, String, String> mapJoin = targetRoot.join(mapAttribute);
        query.multiselect(mapJoin.key(), mapJoin.value());
        query.orderBy(cb.asc(mapJoin.key()));

        return entityManager
                .createQuery(query)
                .getResultList()
                .stream()
                .collect(Collectors.toMap(entry -> (String) entry[0], entry -> (String) entry[1], (v1, v2) -> v1, LinkedHashMap::new));
    }

    private JpaSoftwareModule findSoftwareModuleAndThrowExceptionIfNotFound(final Long softwareModuleId) {
        return softwareModuleRepository.findById(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));
    }

    private JpaDistributionSetCreate setDefaultTypeIfMissing(final JpaDistributionSetCreate create) {
        if (create.getType() == null) {
            create.type(systemManagement.getTenantMetadata().getDefaultDsType().getKey());
        }
        return create;
    }

    private List<Specification<JpaDistributionSet>> buildSpecsByComplete(final Boolean complete) {
        final List<Specification<JpaDistributionSet>> specifications = new ArrayList<>();
        specifications.add(DistributionSetSpecification.isNotDeleted());
        if (complete != null) {
            specifications.add(DistributionSetSpecification.isCompleted(complete));
        }
        return specifications;
    }

    private void assertMetaDataQuota(final Long dsId, final int requested) {
        final int limit = quotaManagement.getMaxMetaDataEntriesPerDistributionSet();
        QuotaHelper.assertAssignmentQuota(dsId, requested, limit, "Metadata", DistributionSet.class.getSimpleName(), null);
    }

    private void assertSoftwareModuleQuota(final Long id, final int requested) {
        QuotaHelper.assertAssignmentQuota(id, requested, quotaManagement.getMaxSoftwareModulesPerDistributionSet(),
                SoftwareModule.class, DistributionSet.class, softwareModuleRepository::countByAssignedToId);
    }

    private void assertDistributionSetIsNotAssignedToTargets(final Long distributionSet) {
        if (actionRepository.countByDistributionSetId(distributionSet) > 0) {
            throw new EntityReadOnlyException(String.format(
                    "Distribution set %s is already assigned to targets and cannot be changed", distributionSet));
        }
    }

    private List<JpaDistributionSet> getDistributionSets(final Collection<Long> ids) {
        final List<JpaDistributionSet> foundDs = distributionSetRepository.findAllById(ids);
        if (foundDs.size() != ids.size()) {
            throw new EntityNotFoundException(
                    DistributionSet.class, ids, foundDs.stream().map(JpaDistributionSet::getId).toList());
        }
        return foundDs;
    }

    private void lockSoftwareModules(final JpaDistributionSet distributionSet) {
        distributionSet.getModules().forEach(module -> {
            if (!module.isLocked()) {
                final JpaSoftwareModule jpaSoftwareModule = (JpaSoftwareModule) module;
                jpaSoftwareModule.lock();
                softwareModuleRepository.save(jpaSoftwareModule);
            }
        });
    }

    private JpaDistributionSet getById(final long id) {
        return distributionSetRepository
                .findById(id)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSet.class, id));
    }

    private void assertDistributionSetExists(final long id) {
        if (!distributionSetRepository.existsById(id)) {
            throw new EntityNotFoundException(DistributionSet.class, id);
        }
    }

    private void assertDsTagExists(final Long tagId) {
        if (!distributionSetTagRepository.existsById(tagId)) {
            throw new EntityNotFoundException(DistributionSetTag.class, tagId);
        }
    }
}