/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetMetadataFields;
import org.eclipse.hawkbit.repository.DistributionSetTypeFields;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TagManagement;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionDeletedEvent;
import org.eclipse.hawkbit.repository.eventbus.event.DistributionSetTagAssigmentResultEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.DsMetadataCompositeKey;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetTypeSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;

/**
 * JPA implementation of {@link DistributionSetManagement}.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
@Validated
public class JpaDistributionSetManagement implements DistributionSetManagement {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DistributionSetRepository distributionSetRepository;

    @Autowired
    private TagManagement tagManagement;

    @Autowired
    private SystemManagement systemManagement;

    @Autowired
    private DistributionSetTypeRepository distributionSetTypeRepository;

    @Autowired
    private DistributionSetMetadataRepository distributionSetMetadataRepository;

    @Autowired
    private TargetFilterQueryRepository targetFilterQueryRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private AfterTransactionCommitExecutor afterCommit;

    @Autowired
    private TenantAware tenantAware;

    @Autowired
    private VirtualPropertyReplacer virtualPropertyReplacer;

    @Autowired
    private SoftwareModuleRepository softwareModuleRepository;

    @Autowired
    private SoftwareModuleTypeRepository softwareModuleTypeRepository;

    @Override
    public DistributionSet findDistributionSetByIdWithDetails(final Long distid) {
        return distributionSetRepository.findOne(DistributionSetSpecification.byId(distid));
    }

    @Override
    public DistributionSet findDistributionSetById(final Long distid) {
        return distributionSetRepository.findOne(distid);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSetTagAssignmentResult toggleTagAssignment(final Collection<Long> dsIds, final String tagName) {

        final List<JpaDistributionSet> sets = findDistributionSetListWithDetails(dsIds);
        final DistributionSetTag myTag = tagManagement.findDistributionSetTag(tagName);

        DistributionSetTagAssignmentResult result;

        final List<JpaDistributionSet> toBeChangedDSs = sets.stream().filter(set -> set.addTag(myTag))
                .collect(Collectors.toList());

        // un-assignment case
        if (toBeChangedDSs.isEmpty()) {
            for (final JpaDistributionSet set : sets) {
                if (set.removeTag(myTag)) {
                    toBeChangedDSs.add(set);
                }
            }
            result = new DistributionSetTagAssignmentResult(dsIds.size() - toBeChangedDSs.size(), 0,
                    toBeChangedDSs.size(), Collections.emptyList(),
                    Collections.unmodifiableList(distributionSetRepository.save(toBeChangedDSs)), myTag,
                    tenantAware.getCurrentTenant());
        } else {
            result = new DistributionSetTagAssignmentResult(dsIds.size() - toBeChangedDSs.size(), toBeChangedDSs.size(),
                    0, Collections.unmodifiableList(distributionSetRepository.save(toBeChangedDSs)),
                    Collections.emptyList(), myTag, tenantAware.getCurrentTenant());
        }

        final DistributionSetTagAssignmentResult resultAssignment = result;
        afterCommit.afterCommit(() -> eventBus
                .post(new DistributionSetTagAssigmentResultEvent(resultAssignment, tenantAware.getCurrentTenant())));

        // no reason to persist the tag
        entityManager.detach(myTag);
        return result;
    }

    private List<JpaDistributionSet> findDistributionSetListWithDetails(final Collection<Long> distributionIdSet) {
        return distributionSetRepository.findAll(DistributionSetSpecification.byIds(distributionIdSet));
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSet updateDistributionSet(final Long setId, final String name, final String description,
            final String version, final Boolean requiredMigrationStep, final Long typeId) {

        final JpaDistributionSet set = findDistributionSetAndThrowExceptionIfNotFound(setId);

        if (name != null) {
            set.setName(name);
        }

        if (description != null) {
            set.setDescription(description);
        }

        if (version != null) {
            set.setVersion(version);
        }

        if (requiredMigrationStep != null && !requiredMigrationStep.equals(set.isRequiredMigrationStep())) {
            checkDistributionSetIsAllowedToModify(set);
            set.setRequiredMigrationStep(requiredMigrationStep);
        }

        if (typeId != null) {
            final DistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(typeId);
            if (type.getId().equals(set.getType().getId())) {
                checkDistributionSetIsAllowedToModify(set);
                set.setType(type);
            }
        }

        return distributionSetRepository.save(set);
    }

    private JpaDistributionSetType findDistributionSetTypeAndThrowExceptionIfNotFound(final Long setId) {
        final JpaDistributionSetType set = (JpaDistributionSetType) findDistributionSetTypeById(setId);

        if (set == null) {
            throw new EntityNotFoundException("Distribution set type cannot be updated as it does not exixt" + setId);
        }
        return set;
    }

    private JpaDistributionSet findDistributionSetAndThrowExceptionIfNotFound(final Long setId) {
        final JpaDistributionSet set = (JpaDistributionSet) findDistributionSetByIdWithDetails(setId);

        if (set == null) {
            throw new EntityNotFoundException("Distribution set cannot be updated as it does not exixt" + setId);
        }
        return set;
    }

    private JpaSoftwareModule findSoftwareModuleAndThrowExceptionIfNotFound(final Long moduleId) {
        final JpaSoftwareModule module = softwareModuleRepository.findOne(moduleId);

        if (module == null) {
            throw new EntityNotFoundException("Distribution set cannot be updated as it does not exixt" + moduleId);
        }
        return module;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteDistributionSet(final Long... distributionSetIDs) {
        final List<Long> assigned = distributionSetRepository
                .findAssignedToTargetDistributionSetsById(distributionSetIDs);
        assigned.addAll(distributionSetRepository.findAssignedToRolloutDistributionSetsById(distributionSetIDs));

        // soft delete assigned
        if (!assigned.isEmpty()) {
            final Long[] dsIds = assigned.toArray(new Long[assigned.size()]);
            distributionSetRepository.deleteDistributionSet(dsIds);
            targetFilterQueryRepository.unsetAutoAssignDistributionSet(dsIds);
        }

        // mark the rest as hard delete
        final List<Long> toHardDelete = Arrays.stream(distributionSetIDs).filter(setId -> !assigned.contains(setId))
                .collect(Collectors.toList());

        // hard delete the rest if exists
        if (!toHardDelete.isEmpty()) {
            // don't give the delete statement an empty list, JPA/Oracle cannot
            // handle the empty list
            distributionSetRepository.deleteByIdIn(toHardDelete);
        }

        Arrays.stream(distributionSetIDs)
                .forEach(dsId -> eventBus.post(new DistributionDeletedEvent(tenantAware.getCurrentTenant(), dsId)));
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSet createDistributionSet(final DistributionSet dSet) {
        prepareDsSave(dSet);
        if (dSet.getType() == null) {
            ((JpaDistributionSet) dSet).setType(systemManagement.getTenantMetadata().getDefaultDsType());
        }
        return distributionSetRepository.save((JpaDistributionSet) dSet);
    }

    private void prepareDsSave(final DistributionSet dSet) {
        if (dSet.getId() != null) {
            throw new EntityAlreadyExistsException("Parameter seems to be an existing, already persisted entity");
        }

        if (distributionSetRepository.countByNameAndVersion(dSet.getName(), dSet.getVersion()) > 0) {
            throw new EntityAlreadyExistsException("DistributionSet with that name and version already exists.");
        }

    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<DistributionSet> createDistributionSets(final Collection<DistributionSet> distributionSets) {
        for (final DistributionSet ds : distributionSets) {
            prepareDsSave(ds);
            if (ds.getType() == null) {
                ((JpaDistributionSet) ds).setType(systemManagement.getTenantMetadata().getDefaultDsType());
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Collection<JpaDistributionSet> toSave = (Collection) distributionSets;

        return Collections.unmodifiableList(distributionSetRepository.save(toSave));
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSet assignSoftwareModules(final Long setId, final Collection<Long> moduleIds) {
        final JpaDistributionSet set = findDistributionSetAndThrowExceptionIfNotFound(setId);
        final Collection<JpaSoftwareModule> modules = softwareModuleRepository.findByIdIn(moduleIds);

        if (modules.size() < moduleIds.size()) {
            throw new EntityNotFoundException("Not all given software modules where found.");
        }

        checkDistributionSetIsAllowedToModify(set);

        modules.forEach(set::addModule);

        return distributionSetRepository.save(set);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSet unassignSoftwareModule(final Long setId, final Long moduleId) {
        final JpaDistributionSet set = findDistributionSetAndThrowExceptionIfNotFound(setId);
        final JpaSoftwareModule module = findSoftwareModuleAndThrowExceptionIfNotFound(moduleId);

        checkDistributionSetIsAllowedToModify(set);

        set.removeModule(module);

        return distributionSetRepository.save(set);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSetType updateDistributionSetType(final Long dsTypeId, final String description,
            final String colour) {

        final JpaDistributionSetType persisted = findDistributionSetTypeAndThrowExceptionIfNotFound(dsTypeId);

        if (description != null) {
            persisted.setDescription(description);
        }

        if (colour != null) {
            persisted.setColour(colour);
        }

        return distributionSetTypeRepository.save(persisted);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSetType assignMandatorySoftwareModuleTypes(final Long dsTypeId,
            final Collection<Long> softwareModulesTypeIds) {
        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(dsTypeId);

        final Collection<JpaSoftwareModuleType> modules = softwareModuleTypeRepository
                .findByIdIn(softwareModulesTypeIds);

        if (modules.size() < softwareModulesTypeIds.size()) {
            throw new EntityNotFoundException("Not all given software module types where found.");
        }

        checkDistributionSetTypeSoftwareModuleTypesIsAllowedToModify(type);

        modules.forEach(type::addMandatoryModuleType);

        return distributionSetTypeRepository.save(type);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSetType assignOptionalSoftwareModuleTypes(final Long dsTypeId,
            final Collection<Long> softwareModulesTypeIds) {
        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(dsTypeId);

        final Collection<JpaSoftwareModuleType> modules = softwareModuleTypeRepository
                .findByIdIn(softwareModulesTypeIds);

        if (modules.size() < softwareModulesTypeIds.size()) {
            throw new EntityNotFoundException("Not all given software module types where found.");
        }

        checkDistributionSetTypeSoftwareModuleTypesIsAllowedToModify(type);

        modules.forEach(type::addOptionalModuleType);

        return distributionSetTypeRepository.save(type);
    }

    private void checkDistributionSetTypeSoftwareModuleTypesIsAllowedToModify(final JpaDistributionSetType type) {
        if (distributionSetRepository.countByType(type) > 0) {
            throw new EntityReadOnlyException(String.format(
                    "distribution set type %s is already assigned to distribution sets and cannot be changed",
                    type.getName()));
        }

    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSetType unassignSoftwareModuleType(final Long dsTypeId, final Long softwareModuleTypeId) {
        final JpaDistributionSetType type = findDistributionSetTypeAndThrowExceptionIfNotFound(dsTypeId);

        checkDistributionSetTypeSoftwareModuleTypesIsAllowedToModify(type);

        type.removeModuleType(softwareModuleTypeId);

        return distributionSetTypeRepository.save(type);
    }

    @Override
    public Page<DistributionSetType> findDistributionSetTypesAll(final String rsqlParam, final Pageable pageable) {
        final Specification<JpaDistributionSetType> spec = RSQLUtility.parse(rsqlParam, DistributionSetTypeFields.class,
                virtualPropertyReplacer);

        return convertDsTPage(distributionSetTypeRepository.findAll(spec, pageable));
    }

    private static Page<DistributionSetType> convertDsTPage(final Page<JpaDistributionSetType> findAll) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()));
    }

    @Override
    public Page<DistributionSetType> findDistributionSetTypesAll(final Pageable pageable) {
        return convertDsTPage(distributionSetTypeRepository.findByDeleted(pageable, false));
    }

    @Override
    public Page<DistributionSet> findDistributionSetsByFilters(final Pageable pageable,
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(
                distributionSetFilter);
        return convertDsPage(findByCriteriaAPI(pageable, specList), pageable);
    }

    private static Page<DistributionSet> convertDsPage(final Page<JpaDistributionSet> findAll,
            final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    /**
     *
     * @param distributionSetFilter
     *            had details of filters to be applied
     * @return a single DistributionSet which is either installed or assigned to
     *         a specific target or {@code null}.
     */
    private DistributionSet findDistributionSetsByFiltersAndInstalledOrAssignedTarget(
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = buildDistributionSetSpecifications(
                distributionSetFilter);
        if (specList == null || specList.isEmpty()) {
            return null;
        }
        return distributionSetRepository.findOne(SpecificationsBuilder.combineWithAnd(specList));
    }

    @Override
    public Page<DistributionSet> findDistributionSetsByDeletedAndOrCompleted(final Pageable pageReq,
            final Boolean deleted, final Boolean complete) {
        final List<Specification<JpaDistributionSet>> specList = new ArrayList<>(2);

        if (deleted != null) {
            final Specification<JpaDistributionSet> spec = DistributionSetSpecification.isDeleted(deleted);
            specList.add(spec);
        }

        if (complete != null) {
            final Specification<JpaDistributionSet> spec = DistributionSetSpecification.isCompleted(complete);
            specList.add(spec);
        }

        return convertDsPage(findByCriteriaAPI(pageReq, specList), pageReq);
    }

    @Override
    public Page<DistributionSet> findDistributionSetsAll(final String rsqlParam, final Pageable pageReq,
            final Boolean deleted) {

        final Specification<JpaDistributionSet> spec = RSQLUtility.parse(rsqlParam, DistributionSetFields.class,
                virtualPropertyReplacer);

        final List<Specification<JpaDistributionSet>> specList = new ArrayList<>(2);
        if (deleted != null) {
            specList.add(DistributionSetSpecification.isDeleted(deleted));
        }
        specList.add(spec);

        return convertDsPage(findByCriteriaAPI(pageReq, specList), pageReq);
    }

    @Override
    public Page<DistributionSet> findDistributionSetsAllOrderedByLinkTarget(final Pageable pageable,
            final DistributionSetFilterBuilder distributionSetFilterBuilder, final String assignedOrInstalled) {

        final DistributionSetFilter filterWithInstalledTargets = distributionSetFilterBuilder
                .setInstalledTargetId(assignedOrInstalled).setAssignedTargetId(null).build();
        final DistributionSet installedDS = findDistributionSetsByFiltersAndInstalledOrAssignedTarget(
                filterWithInstalledTargets);

        final DistributionSetFilter filterWithAssignedTargets = distributionSetFilterBuilder.setInstalledTargetId(null)
                .setAssignedTargetId(assignedOrInstalled).build();
        final DistributionSet assignedDS = findDistributionSetsByFiltersAndInstalledOrAssignedTarget(
                filterWithAssignedTargets);

        final DistributionSetFilter dsFilterWithNoTargetLinked = distributionSetFilterBuilder.setInstalledTargetId(null)
                .setAssignedTargetId(null).build();
        // first fine the distribution sets filtered by the given filter
        // parameters
        final Page<DistributionSet> findDistributionSetsByFilters = findDistributionSetsByFilters(pageable,
                dsFilterWithNoTargetLinked);

        final List<DistributionSet> resultSet = new ArrayList<>(findDistributionSetsByFilters.getContent());
        int orderIndex = 0;
        if (installedDS != null) {
            final boolean remove = resultSet.remove(installedDS);
            if (!remove) {
                resultSet.remove(resultSet.size() - 1);
            }
            resultSet.add(orderIndex, installedDS);
            orderIndex++;
        }
        if (assignedDS != null && !assignedDS.equals(installedDS)) {
            final boolean remove = resultSet.remove(assignedDS);
            if (!remove) {
                resultSet.remove(resultSet.size() - 1);
            }
            resultSet.add(orderIndex, assignedDS);
        }

        return new PageImpl<>(resultSet, pageable, findDistributionSetsByFilters.getTotalElements());
    }

    @Override
    public DistributionSet findDistributionSetByNameAndVersion(final String distributionName, final String version) {
        final Specification<JpaDistributionSet> spec = DistributionSetSpecification
                .equalsNameAndVersionIgnoreCase(distributionName, version);
        return distributionSetRepository.findOne(spec);

    }

    @Override
    public List<DistributionSet> findDistributionSetsAll(final Collection<Long> dist) {
        return Collections
                .unmodifiableList(distributionSetRepository.findAll(DistributionSetSpecification.byIds(dist)));
    }

    @Override
    public Long countDistributionSetsAll() {
        final Specification<JpaDistributionSet> spec = DistributionSetSpecification.isDeleted(Boolean.FALSE);

        return distributionSetRepository.count(SpecificationsBuilder.combineWithAnd(Lists.newArrayList(spec)));
    }

    @Override
    public Long countDistributionSetTypesAll() {
        return distributionSetTypeRepository.countByDeleted(false);
    }

    @Override
    public DistributionSetType findDistributionSetTypeByName(final String name) {
        return distributionSetTypeRepository.findOne(DistributionSetTypeSpecification.byName(name));
    }

    @Override
    public DistributionSetType findDistributionSetTypeById(final Long id) {
        return distributionSetTypeRepository.findOne(DistributionSetTypeSpecification.byId(id));
    }

    @Override
    public DistributionSetType findDistributionSetTypeByKey(final String key) {
        return distributionSetTypeRepository.findOne(DistributionSetTypeSpecification.byKey(key));
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSetType createDistributionSetType(final DistributionSetType type) {
        if (type.getId() != null) {
            throw new EntityAlreadyExistsException("Given type contains an Id!");
        }

        return distributionSetTypeRepository.save((JpaDistributionSetType) type);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteDistributionSetType(final DistributionSetType ty) {
        final JpaDistributionSetType type = (JpaDistributionSetType) ty;

        if (distributionSetRepository.countByType(type) > 0) {
            final JpaDistributionSetType toDelete = entityManager.merge(type);
            toDelete.setDeleted(true);
            distributionSetTypeRepository.save(toDelete);
        } else {
            distributionSetTypeRepository.delete(type.getId());
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public List<DistributionSetMetadata> createDistributionSetMetadata(final Long dsId, final Collection<MetaData> md) {

        md.forEach(meta -> checkAndThrowAlreadyIfDistributionSetMetadataExists(
                new DsMetadataCompositeKey(dsId, meta.getKey())));

        final JpaDistributionSet set = touch(dsId);

        return new ArrayList<>((Collection<? extends DistributionSetMetadata>) distributionSetMetadataRepository
                .save(md.stream().map(meta -> new JpaDistributionSetMetadata(meta.getKey(), set, meta.getValue()))
                        .collect(Collectors.toList())));
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public DistributionSetMetadata updateDistributionSetMetadata(final Long dsId, final MetaData md) {

        // check if exists otherwise throw entity not found exception
        final JpaDistributionSetMetadata toUpdate = (JpaDistributionSetMetadata) findDistributionSetMetadata(dsId,
                md.getKey());
        toUpdate.setValue(md.getValue());
        // touch it to update the lock revision because we are modifying the
        // DS indirectly
        touch(dsId);
        return distributionSetMetadataRepository.save(toUpdate);
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Modifying
    public void deleteDistributionSetMetadata(final Long distributionSet, final String key) {
        touch(distributionSet);
        distributionSetMetadataRepository.delete(new DsMetadataCompositeKey(distributionSet, key));
    }

    /**
     * Method to get the latest distribution set based on DS ID after the
     * metadata changes for that distribution set.
     *
     * @param distId
     *            Distribution set
     */
    private JpaDistributionSet touch(final Long distId) {
        final DistributionSet latestDistributionSet = findDistributionSetAndThrowExceptionIfNotFound(distId);

        // merge base distribution set so optLockRevision gets updated and audit
        // log written because
        // modifying metadata is modifying the base distribution set itself for
        // auditing purposes.
        final JpaDistributionSet result = entityManager.merge((JpaDistributionSet) latestDistributionSet);
        result.setLastModifiedAt(0L);

        return result;
    }

    @Override
    public Page<DistributionSetMetadata> findDistributionSetMetadataByDistributionSetId(final Long distributionSetId,
            final Pageable pageable) {
        return convertMdPage(distributionSetMetadataRepository
                .findAll((Specification<JpaDistributionSetMetadata>) (root, query, cb) -> cb.equal(
                        root.get(JpaDistributionSetMetadata_.distributionSet).get(JpaDistributionSet_.id),
                        distributionSetId), pageable),
                pageable);
    }

    @Override
    public List<DistributionSetMetadata> findDistributionSetMetadataByDistributionSetId(final Long distributionSetId) {
        return Collections.unmodifiableList(distributionSetMetadataRepository
                .findAll((Specification<JpaDistributionSetMetadata>) (root, query, cb) -> cb.equal(
                        root.get(JpaDistributionSetMetadata_.distributionSet).get(JpaDistributionSet_.id),
                        distributionSetId)));
    }

    @Override
    public Page<DistributionSetMetadata> findDistributionSetMetadataByDistributionSetId(final Long distributionSetId,
            final String rsqlParam, final Pageable pageable) {

        final Specification<JpaDistributionSetMetadata> spec = RSQLUtility.parse(rsqlParam,
                DistributionSetMetadataFields.class, virtualPropertyReplacer);

        return convertMdPage(
                distributionSetMetadataRepository
                        .findAll((Specification<JpaDistributionSetMetadata>) (root, query, cb) -> cb.and(
                                cb.equal(root.get(JpaDistributionSetMetadata_.distributionSet)
                                        .get(JpaDistributionSet_.id), distributionSetId),
                                spec.toPredicate(root, query, cb)), pageable),
                pageable);
    }

    private static Page<DistributionSetMetadata> convertMdPage(final Page<JpaDistributionSetMetadata> findAll,
            final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(findAll.getContent()), pageable, findAll.getTotalElements());
    }

    @Override
    public DistributionSetMetadata findDistributionSetMetadata(final Long distributionSet, final String key) {
        final DistributionSetMetadata findOne = distributionSetMetadataRepository
                .findOne(new DsMetadataCompositeKey(distributionSet, key));
        if (findOne == null) {
            throw new EntityNotFoundException("Metadata with key '" + key + "' does not exist");
        }
        return findOne;
    }

    @Override
    public DistributionSet findDistributionSetByAction(final Action action) {
        return distributionSetRepository.findByAction((JpaAction) action);
    }

    @Override
    public boolean isDistributionSetInUse(final DistributionSet distributionSet) {
        return actionRepository.countByDistributionSet((JpaDistributionSet) distributionSet) > 0;
    }

    private static List<Specification<JpaDistributionSet>> buildDistributionSetSpecifications(
            final DistributionSetFilter distributionSetFilter) {
        final List<Specification<JpaDistributionSet>> specList = new ArrayList<>(7);

        Specification<JpaDistributionSet> spec;

        if (null != distributionSetFilter.getIsComplete()) {
            spec = DistributionSetSpecification.isCompleted(distributionSetFilter.getIsComplete());
            specList.add(spec);
        }

        if (null != distributionSetFilter.getIsDeleted()) {
            spec = DistributionSetSpecification.isDeleted(distributionSetFilter.getIsDeleted());
            specList.add(spec);
        }

        if (distributionSetFilter.getType() != null) {
            spec = DistributionSetSpecification.byType(distributionSetFilter.getType());
            specList.add(spec);
        }

        if (!Strings.isNullOrEmpty(distributionSetFilter.getSearchText())) {
            spec = DistributionSetSpecification.likeNameOrDescriptionOrVersion(distributionSetFilter.getSearchText());
            specList.add(spec);
        }

        if (isDSWithNoTagSelected(distributionSetFilter) || isTagsSelected(distributionSetFilter)) {
            spec = DistributionSetSpecification.hasTags(distributionSetFilter.getTagNames(),
                    distributionSetFilter.getSelectDSWithNoTag());
            specList.add(spec);
        }
        if (distributionSetFilter.getInstalledTargetId() != null) {
            spec = DistributionSetSpecification.installedTarget(distributionSetFilter.getInstalledTargetId());
            specList.add(spec);
        }
        if (distributionSetFilter.getAssignedTargetId() != null) {
            spec = DistributionSetSpecification.assignedTarget(distributionSetFilter.getAssignedTargetId());
            specList.add(spec);
        }
        return specList;
    }

    private void checkDistributionSetIsAllowedToModify(final JpaDistributionSet distributionSet) {
        if (actionRepository.countByDistributionSet(distributionSet) > 0) {
            throw new EntityReadOnlyException(
                    String.format("distribution set %s:%s is already assigned to targets and cannot be changed",
                            distributionSet.getName(), distributionSet.getVersion()));
        }
    }

    private static Boolean isDSWithNoTagSelected(final DistributionSetFilter distributionSetFilter) {
        if (distributionSetFilter.getSelectDSWithNoTag() != null && distributionSetFilter.getSelectDSWithNoTag()) {
            return true;
        }
        return false;
    }

    private static Boolean isTagsSelected(final DistributionSetFilter distributionSetFilter) {
        if (distributionSetFilter.getTagNames() != null && !distributionSetFilter.getTagNames().isEmpty()) {
            return true;
        }
        return false;
    }

    /**
     * executes findAll with the given {@link DistributionSet}
     * {@link Specification}s.
     *
     * @param pageable
     *            paging parameter
     * @param specList
     *            list of @link {@link Specification}
     * @return the page with the found {@link DistributionSet}
     */
    private Page<JpaDistributionSet> findByCriteriaAPI(final Pageable pageable,
            final List<Specification<JpaDistributionSet>> specList) {

        if (specList == null || specList.isEmpty()) {
            return distributionSetRepository.findAll(pageable);
        }

        return distributionSetRepository.findAll(SpecificationsBuilder.combineWithAnd(specList), pageable);
    }

    private void checkAndThrowAlreadyIfDistributionSetMetadataExists(final DsMetadataCompositeKey metadataId) {
        if (distributionSetMetadataRepository.exists(metadataId)) {
            throw new EntityAlreadyExistsException(
                    "Metadata entry with key '" + metadataId.getKey() + "' already exists");
        }
    }

    private static void throwMetadataKeyAlreadyExists(final String metadataKey) {
        throw new EntityAlreadyExistsException("Metadata entry with key '" + metadataKey + "' already exists");
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<DistributionSet> assignTag(final Collection<Long> dsIds, final DistributionSetTag tag) {
        final List<JpaDistributionSet> allDs = findDistributionSetListWithDetails(dsIds);

        allDs.forEach(ds -> ds.addTag(tag));

        final List<DistributionSet> save = Collections.unmodifiableList(distributionSetRepository.save(allDs));

        afterCommit.afterCommit(() -> {

            final DistributionSetTagAssignmentResult result = new DistributionSetTagAssignmentResult(0, save.size(), 0,
                    save, Collections.emptyList(), tag, tenantAware.getCurrentTenant());
            eventBus.post(new DistributionSetTagAssigmentResultEvent(result, tenantAware.getCurrentTenant()));
        });

        return save;
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<DistributionSet> unAssignAllDistributionSetsByTag(final DistributionSetTag tag) {

        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Collection<JpaDistributionSet> distributionSets = (Collection) tag.getAssignedToDistributionSet();

        return Collections.unmodifiableList(unAssignTag(distributionSets, tag));
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSet unAssignTag(final Long dsId, final DistributionSetTag distributionSetTag) {
        final List<JpaDistributionSet> allDs = findDistributionSetListWithDetails(Arrays.asList(dsId));
        final List<JpaDistributionSet> unAssignTag = unAssignTag(allDs, distributionSetTag);
        return unAssignTag.isEmpty() ? null : unAssignTag.get(0);
    }

    private List<JpaDistributionSet> unAssignTag(final Collection<JpaDistributionSet> distributionSets,
            final DistributionSetTag tag) {
        distributionSets.forEach(ds -> ds.removeTag(tag));
        return distributionSetRepository.save(distributionSets);
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public List<DistributionSetType> createDistributionSetTypes(final Collection<DistributionSetType> types) {
        return types.stream().map(this::createDistributionSetType).collect(Collectors.toList());
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deleteDistributionSet(final DistributionSet set) {
        deleteDistributionSet(set.getId());
    }

    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public DistributionSetTagAssignmentResult toggleTagAssignment(final Collection<DistributionSet> sets,
            final DistributionSetTag tag) {
        return toggleTagAssignment(sets.stream().map(ds -> ds.getId()).collect(Collectors.toList()), tag.getName());
    }

    @Override
    public Long countDistributionSetsByType(final DistributionSetType type) {
        return distributionSetRepository.countByType((JpaDistributionSetType) type);
    }
}
