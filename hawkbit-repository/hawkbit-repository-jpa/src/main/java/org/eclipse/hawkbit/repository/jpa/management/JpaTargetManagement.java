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

import static org.eclipse.hawkbit.repository.jpa.JpaManagementHelper.combineWithAnd;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.validation.constraints.NotEmpty;

import org.eclipse.hawkbit.ql.jpa.QLSupport;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTagRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.qfields.TargetFields;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link TargetManagement}.
 */
@Transactional(readOnly = true)
@Validated
@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "target-management" }, matchIfMissing = true)
public class JpaTargetManagement
        extends AbstractJpaRepositoryManagement<JpaTarget, TargetManagement.Create, TargetManagement.Update, TargetRepository, TargetFields>
        implements TargetManagement<JpaTarget> {

    private final JpaDistributionSetManagement distributionSetManagement;
    private final QuotaManagement quotaManagement;
    private final TargetTypeRepository targetTypeRepository;
    private final TargetTagRepository targetTagRepository;

    @SuppressWarnings("java:S107")
    protected JpaTargetManagement(
            final TargetRepository jpaRepository, final EntityManager entityManager,
            final JpaDistributionSetManagement distributionSetManagement, final QuotaManagement quotaManagement,
            final TargetTypeRepository targetTypeRepository,
            final TargetTagRepository targetTagRepository) {
        super(jpaRepository, entityManager);
        this.distributionSetManagement = distributionSetManagement;
        this.quotaManagement = quotaManagement;
        this.targetTypeRepository = targetTypeRepository;
        this.targetTagRepository = targetTagRepository;
    }

    @Override
    public Map<String, String> getControllerAttributes(final String controllerId) {
        return getMap(controllerId, JpaTarget_.controllerAttributes);
    }

    @Override
    public boolean isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(
            final String controllerId, final long distributionSetId, final String targetFilterQuery) {
        QLSupport.getInstance().validate(targetFilterQuery, TargetFields.class, JpaTarget.class);
        final DistributionSet ds = distributionSetManagement.get(distributionSetId);
        final Long distSetTypeId = ds.getType().getId();
        final List<Specification<JpaTarget>> specList = List.of(
                QLSupport.getInstance().buildSpec(targetFilterQuery, TargetFields.class),
                TargetSpecifications.hasNotDistributionSetInActions(distributionSetId),
                TargetSpecifications.isCompatibleWithDistributionSetType(distSetTypeId),
                TargetSpecifications.hasControllerId(controllerId));

        final Specification<JpaTarget> combinedSpecification = Objects
                .requireNonNull(JpaManagementHelper.combineWithAnd(specList));
        return jpaRepository.exists(AccessController.Operation.UPDATE, combinedSpecification);
    }

    @Override
    public Target getByControllerId(final String controllerId) {
        return jpaRepository.findByControllerId(controllerId).map(Target.class::cast)
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
    }

    @Override
    public Optional<Target> findByControllerId(final String controllerId) {
        return jpaRepository.findByControllerId(controllerId).map(Target.class::cast);
    }

    @Override
    public List<Target> findByControllerId(final Collection<String> controllerIDs) {
        return Collections.unmodifiableList(jpaRepository.findAll(TargetSpecifications.byControllerIdWithAssignedDsInJoin(controllerIDs)));
    }

    @Override
    public Target getWithDetails(final String controllerId, final String detailsKey) {
        return jpaRepository.getWithDetailsByControllerId(controllerId, "Target." + detailsKey);
    }

    @Override
    public Slice<Target> findByTargetFilterQueryAndNonDSAndCompatibleAndUpdatable(
            final long distributionSetId, final String rsql, final Pageable pageable) {
        final DistributionSet jpaDistributionSet = distributionSetManagement.get(distributionSetId);
        final Long distSetTypeId = jpaDistributionSet.getType().getId();

        return jpaRepository
                .findAllWithoutCount(
                        AccessController.Operation.UPDATE,
                        combineWithAnd(List.of(
                                QLSupport.getInstance().buildSpec(rsql, TargetFields.class),
                                TargetSpecifications.hasNotDistributionSetInActions(distributionSetId),
                                TargetSpecifications.isCompatibleWithDistributionSetType(distSetTypeId))),
                        pageable)
                .map(Target.class::cast);
    }

    @Override
    public Page<Target> findByAssignedDistributionSet(final long distributionSetId, final Pageable pageable) {
        final DistributionSet validDistSet = distributionSetManagement.get(distributionSetId);

        return JpaManagementHelper.findAllWithCountBySpec(
                jpaRepository,
                List.of(TargetSpecifications.hasAssignedDistributionSet(validDistSet.getId())), pageable);
    }

    @Override
    public Page<Target> findByAssignedDistributionSetAndRsql(final long distributionSetId, final String rsql, final Pageable pageable) {
        final DistributionSet validDistSet = distributionSetManagement.get(distributionSetId);

        final List<Specification<JpaTarget>> specList = List.of(
                QLSupport.getInstance().buildSpec(rsql, TargetFields.class),
                TargetSpecifications.hasAssignedDistributionSet(validDistSet.getId()));

        return JpaManagementHelper.findAllWithCountBySpec(jpaRepository, specList, pageable);
    }

    @Override
    public Page<Target> findByInstalledDistributionSet(final long distributionSetId, final Pageable pageReq) {
        final DistributionSet validDistSet = distributionSetManagement.get(distributionSetId);

        return JpaManagementHelper.findAllWithCountBySpec(
                jpaRepository, List.of(TargetSpecifications.hasInstalledDistributionSet(validDistSet.getId())), pageReq);
    }

    @Override
    public Page<Target> findByInstalledDistributionSetAndRsql(final long distributionSetId, final String rsql, final Pageable pageable) {
        final DistributionSet validDistSet = distributionSetManagement.get(distributionSetId);

        final List<Specification<JpaTarget>> specList = List.of(
                QLSupport.getInstance().buildSpec(rsql, TargetFields.class),
                TargetSpecifications.hasInstalledDistributionSet(validDistSet.getId()));

        return JpaManagementHelper.findAllWithCountBySpec(jpaRepository, specList, pageable);
    }

    @Override
    public Page<Target> findByTag(final long tagId, final Pageable pageable) {
        throwEntityNotFoundExceptionIfTagDoesNotExist(tagId);
        return JpaManagementHelper.findAllWithCountBySpec(jpaRepository, List.of(TargetSpecifications.hasTag(tagId)), pageable);
    }

    @Override
    public Page<Target> findByRsqlAndTag(final String rsql, final long tagId, final Pageable pageable) {
        throwEntityNotFoundExceptionIfTagDoesNotExist(tagId);

        final List<Specification<JpaTarget>> specList = List.of(
                QLSupport.getInstance().buildSpec(rsql, TargetFields.class),
                TargetSpecifications.hasTag(tagId));

        return JpaManagementHelper.findAllWithCountBySpec(jpaRepository, specList, pageable);
    }

    @Override
    public long countByRsqlAndCompatible(final String rsql, final Long distributionSetIdTypeId) {
        final List<Specification<JpaTarget>> specList = List.of(
                QLSupport.getInstance().buildSpec(rsql, TargetFields.class),
                TargetSpecifications.isCompatibleWithDistributionSetType(distributionSetIdTypeId));
        return JpaManagementHelper.countBySpec(jpaRepository, specList);
    }

    @Override
    public long countByFailedInRollout(final String rolloutId, final Long dsTypeId) {
        final List<Specification<JpaTarget>> specList = List.of(TargetSpecifications.failedActionsForRollout(rolloutId));
        return JpaManagementHelper.countBySpec(jpaRepository, specList);
    }

    @Override
    public long countByRsqlAndNonDsAndCompatibleAndUpdatable(final long distributionSetId, final String rsql) {
        final DistributionSet jpaDistributionSet = distributionSetManagement.get(distributionSetId);
        final Long distSetTypeId = jpaDistributionSet.getType().getId();

        return jpaRepository.count(
                AccessController.Operation.UPDATE,
                combineWithAnd(List.of(
                        QLSupport.getInstance().buildSpec(rsql, TargetFields.class),
                        TargetSpecifications.hasNotDistributionSetInActions(distributionSetId),
                        TargetSpecifications.isCompatibleWithDistributionSetType(distSetTypeId))));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteByControllerId(final String controllerId) {
        jpaRepository.delete(jpaRepository.getByControllerId(controllerId));
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<Target> assignTag(
            final Collection<String> controllerIds, final long targetTagId, final Consumer<Collection<String>> notFoundHandler) {
        return assignTag0(controllerIds, targetTagId, notFoundHandler);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<Target> assignTag(final Collection<String> controllerIds, final long targetTagId) {
        return assignTag0(controllerIds, targetTagId, null);
    }

    private List<Target> assignTag0(
            final Collection<String> controllerIds, final long targetTagId, final Consumer<Collection<String>> notFoundHandler) {
        return updateTag(controllerIds, targetTagId, notFoundHandler, (tag, target) -> {
            if (target.getTags().contains(tag)) {
                return target;
            } else {
                target.addTag(tag);
                return jpaRepository.save(target);
            }
        });
    }

    @Override
    public Set<TargetTag> getTags(@NotEmpty String controllerId) {
        // the method has PreAuthorized by itself
        return ((JpaTarget) getWithDetails(controllerId, DETAILS_TAGS)).getTags();
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<Target> unassignTag(
            final Collection<String> controllerIds, final long targetTagId, final Consumer<Collection<String>> notFoundHandler) {
        return unassignTag0(controllerIds, targetTagId, notFoundHandler);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public List<Target> unassignTag(final Collection<String> controllerIds, final long targetTagId) {
        return unassignTag0(controllerIds, targetTagId, null);
    }

    private List<Target> unassignTag0(
            final Collection<String> controllerIds, final long targetTagId, final Consumer<Collection<String>> notFoundHandler) {
        return updateTag(controllerIds, targetTagId, notFoundHandler, (tag, target) -> {
            if (target.getTags().contains(tag)) {
                target.removeTag(tag);
                return jpaRepository.save(target);
            } else {
                return target;
            }
        });
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target assignType(final String controllerId, final Long targetTypeId) {
        final JpaTarget target = jpaRepository.getByControllerId(controllerId);

        jpaRepository.getAccessController().ifPresent(acm ->
                acm.assertOperationAllowed(AccessController.Operation.UPDATE, target));

        final JpaTargetType targetType = targetTypeRepository.getById(targetTypeId);
        target.setTargetType(targetType);
        return jpaRepository.save(target);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target unassignType(final String controllerId) {
        final JpaTarget target = jpaRepository.getByControllerId(controllerId);
        target.setTargetType(null);
        return jpaRepository.save(target);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void assignTargetGroupWithRsql(String group, String rsql) {
        final Specification<JpaTarget> rsqlSpecification = QLSupport.getInstance().buildSpec(rsql, TargetFields.class);

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaUpdate<JpaTarget> criteriaUpdateQuery = cb.createCriteriaUpdate(JpaTarget.class);
        final Root<JpaTarget> root = criteriaUpdateQuery.getRoot();
        criteriaUpdateQuery.set("group", group);
        // get predicate from rsql specification using a dummy query in order to execute batch update
        final Predicate predicate = rsqlSpecification.toPredicate(root, entityManager.getCriteriaBuilder().createQuery(JpaTarget.class), cb);
        criteriaUpdateQuery.where(predicate);

        entityManager.createQuery(criteriaUpdateQuery).executeUpdate();
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void assignTargetsWithGroup(String group, List<String> controllerIds) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaUpdate<JpaTarget> criteriaQuery = cb.createCriteriaUpdate(JpaTarget.class);
        Root<JpaTarget> root = criteriaQuery.from(JpaTarget.class);
        CriteriaBuilder.In<String> in = cb.in(root.get("controllerId"));
        controllerIds.forEach(in::value);

        entityManager.createQuery(criteriaQuery.set("group", group).where(in)).executeUpdate();
    }

    @Override
    public Page<Target> findTargetsByGroup(String group, final boolean withSubgroups, final Pageable pageable) {
        if (withSubgroups) {
            // search for eq(group) and like(group%)
            return JpaManagementHelper
                    .findAllWithCountBySpec(jpaRepository, List.of(TargetSpecifications.eqOrSubTargetGroup(group)), pageable);
        } else {
            return JpaManagementHelper
                    .findAllWithCountBySpec(jpaRepository, List.of(TargetSpecifications.eqTargetGroup(group)), pageable);
        }
    }

    @Override
    public List<String> findGroups() {
        return jpaRepository.findDistinctGroups();
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void createMetadata(final String controllerId, final String key, final String value) {
        final JpaTarget target = jpaRepository.getByControllerId(controllerId);

        // get the modifiable metadata map
        final Map<String, String> metadata = target.getMetadata();
        if (!metadata.containsKey(key)) {
            assertMetadataQuota(target.getId(), metadata.size() + 1);
        }
        metadata.put(key, value);

        jpaRepository.save(target);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void createMetadata(final String controllerId, final Map<String, String> md) {
        final JpaTarget target = jpaRepository.getByControllerId(controllerId);

        // get the modifiable metadata map
        final Map<String, String> metadata = target.getMetadata();
        md.keySet().forEach(key -> {
            if (metadata.containsKey(key)) {
                throw new EntityAlreadyExistsException("Metadata entry with key '" + key + "' already exists");
            }
        });
        metadata.putAll(md);

        assertMetadataQuota(target.getId(), metadata.size());
        jpaRepository.save(target);
    }

    @Override
    public Map<String, String> getMetadata(final String controllerId) {
        return getMap(controllerId, JpaTarget_.metadata);
    }

    @Override
    public String getMetadata(final String controllerId, final String key) {
        return getMap(controllerId, JpaTarget_.metadata).get(key);
    }

    @Override
    @Transactional
    @Retryable(retryFor = { ConcurrencyFailureException.class }, maxAttempts = Constants.TX_RT_MAX,
            backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteMetadata(final String controllerId, final String key) {
        final JpaTarget target = jpaRepository.getByControllerId(controllerId);

        // get the modifiable metadata map
        final Map<String, String> metadata = target.getMetadata();
        if (metadata.remove(key) == null) {
            throw new EntityNotFoundException("Target metadata", controllerId + ":" + key);
        }

        jpaRepository.save(target);
    }

    private Map<String, String> getMap(final String controllerId, final MapAttribute<JpaTarget, String, String> mapAttribute) {
        jpaRepository.getByControllerId(controllerId);

        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);

        final Root<JpaTarget> targetRoot = query.from(JpaTarget.class);
        query.where(cb.equal(targetRoot.get(JpaTarget_.controllerId), controllerId));

        final MapJoin<JpaTarget, String, String> mapJoin = targetRoot.join(mapAttribute);
        query.multiselect(mapJoin.key(), mapJoin.value());
        query.orderBy(cb.asc(mapJoin.key()));

        return entityManager
                .createQuery(query)
                .getResultList()
                .stream()
                .collect(Collectors.toMap(entry -> (String) entry[0], entry -> (String) entry[1], (v1, v2) -> v1, LinkedHashMap::new));
    }

    private static Collection<String> notFound(final Collection<String> controllerIds, final List<JpaTarget> foundTargets) {
        final Map<String, JpaTarget> foundTargetMap = foundTargets.stream()
                .collect(Collectors.toMap(Target::getControllerId, Function.identity()));
        return controllerIds.stream().filter(id -> !foundTargetMap.containsKey(id)).toList();
    }

    private void assertMetadataQuota(final Long targetId, final int requested) {
        final int limit = quotaManagement.getMaxMetaDataEntriesPerTarget();
        QuotaHelper.assertAssignmentQuota(targetId, requested, limit, "Metadata", Target.class.getSimpleName(), null);
    }

    private List<Target> updateTag(
            final Collection<String> controllerIds, final long targetTagId, final Consumer<Collection<String>> notFoundHandler,
            final BiFunction<JpaTargetTag, JpaTarget, Target> updater) {
        final JpaTargetTag tag = targetTagRepository.getById(targetTagId);
        final List<JpaTarget> targets = controllerIds.size() == 1 ?
                jpaRepository.findByControllerId(controllerIds.iterator().next())
                        .map(List::of)
                        .orElseGet(Collections::emptyList) :
                jpaRepository.findAll(TargetSpecifications.byControllerIdWithTagsInJoin(controllerIds));
        if (targets.size() < controllerIds.size()) {
            final Collection<String> notFound = notFound(controllerIds, targets);
            if (notFoundHandler == null) {
                throw new EntityNotFoundException(Target.class, notFound);
            } else {
                notFoundHandler.accept(notFound);
            }
        }

        jpaRepository.getAccessController()
                .ifPresent(acm -> acm.assertOperationAllowed(AccessController.Operation.UPDATE, targets));

        try {
            return targets.stream().map(target -> updater.apply(tag, target)).toList();
        } finally {
            // No reason to save the tag
            entityManager.detach(tag);
        }
    }

    private void throwEntityNotFoundExceptionIfTagDoesNotExist(final Long tagId) {
        if (!targetTagRepository.existsById(tagId)) {
            throw new EntityNotFoundException(TargetTag.class, tagId);
        }
    }
}