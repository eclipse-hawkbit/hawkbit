/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.deprecated;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import jakarta.persistence.EntityManager;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.rest.resource.MgmtDistributionSetMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.MgmtTargetMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.deprecated.json.model.MgmtAssignedDistributionSetRequestBody;
import org.eclipse.hawkbit.mgmt.rest.resource.deprecated.json.model.MgmtAssignedTargetRequestBody;
import org.eclipse.hawkbit.mgmt.rest.resource.deprecated.json.model.MgmtDistributionSetTagAssigmentResult;
import org.eclipse.hawkbit.mgmt.rest.resource.deprecated.json.model.MgmtTargetTagAssigmentResult;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTagRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.DistributionSetSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for {@link DistributionSetTag} CRUD operations.
 */
@Slf4j
@RestController
@SuppressWarnings("java:S5738") // will be removed at some point
public class DeprecatedMgmtResource implements DeprecatedMgmtRestApi {

    // logger that logs usage of deprecated API
    private static final Logger DEPRECATED_USAGE_LOGGER = LoggerFactory.getLogger("DEPRECATED_USAGE");

    private final DistributionSetRepository distributionSetRepository;
    private final DistributionSetTagManagement distributionSetTagManagement;
    private final DistributionSetManagement distributionSetManagement;
    private final TargetRepository targetRepository;
    private final TargetTagRepository targetTagRepository;
    private final TargetManagement targetManagement;
    private final TargetTagManagement targetTagManagement;
    private final PlatformTransactionManager txManager;
    private final EntityManager entityManager;
    private final TenantConfigHelper tenantConfigHelper;

    @SuppressWarnings("squid:S107")
    DeprecatedMgmtResource(
            final DistributionSetRepository distributionSetRepository, final DistributionSetTagManagement distributionSetTagManagement,
            final DistributionSetManagement distributionSetManagement,
            final TargetRepository targetRepository, final TargetTagRepository targetTagRepository, final TargetManagement targetManagement,
            final TargetTagManagement targetTagManagement,
            final PlatformTransactionManager txManager, final EntityManager entityManager,
            final SystemSecurityContext securityContext, final TenantConfigurationManagement configurationManagement) {
        this.distributionSetRepository = distributionSetRepository;
        this.distributionSetTagManagement = distributionSetTagManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.targetRepository = targetRepository;
        this.targetTagRepository = targetTagRepository;
        this.targetManagement = targetManagement;
        this.targetTagManagement = targetTagManagement;
        this.txManager = txManager;
        this.entityManager = entityManager;
        tenantConfigHelper = TenantConfigHelper.usingContext(securityContext, configurationManagement);
    }

    @Override
    public ResponseEntity<MgmtDistributionSetTagAssigmentResult> toggleDistributionSetTagAssignment(
            final Long distributionsetTagId,
            final List<MgmtAssignedDistributionSetRequestBody> assignedDSRequestBodies) {
        DEPRECATED_USAGE_LOGGER.debug("[DEPRECATED] Deprecated POST /rest/v1/distributionsettags/{distributionsetTagId}/assigned/toggleTagAssignment called");
        log.debug("Toggle distribution set assignment {} for ds tag {}", assignedDSRequestBodies.size(), distributionsetTagId);

        final DistributionSetTag tag = findDistributionTagById(distributionsetTagId);

        final DistributionSetTagAssignmentResult assigmentResult =
                DeploymentHelper.runInNewTransaction(
                        txManager,
                        "toggleDistributionSetTagAssignment",
                        status -> toggleDistributionSetTagAssignment(findDistributionSetIds(assignedDSRequestBodies), tag.getName()));

        final MgmtDistributionSetTagAssigmentResult tagAssigmentResultRest = new MgmtDistributionSetTagAssigmentResult();
        tagAssigmentResultRest.setAssignedDistributionSets(
                MgmtDistributionSetMapper.toResponseDistributionSets(assigmentResult.getAssignedEntity()));
        tagAssigmentResultRest.setUnassignedDistributionSets(
                MgmtDistributionSetMapper.toResponseDistributionSets(assigmentResult.getUnassignedEntity()));

        log.debug("Toggled assignedDS {} and unassignedDS{}", assigmentResult.getAssigned(), assigmentResult.getUnassigned());

        return ResponseEntity.ok(tagAssigmentResultRest);
    }

    @Override
    public ResponseEntity<List<MgmtDistributionSet>> assignDistributionSetsByRequestBody(
            final Long distributionsetTagId,
            final List<MgmtAssignedDistributionSetRequestBody> assignedDSRequestBodies) {
        DEPRECATED_USAGE_LOGGER.debug("[DEPRECATED] Deprecated POST /rest/v1/distributionsettags/{distributionsetTagId}/assigned called");
        log.debug("Assign DistributionSet {} for ds tag {}", assignedDSRequestBodies.size(), distributionsetTagId);
        final List<DistributionSet> assignedDs = this.distributionSetManagement
                .assignTag(findDistributionSetIds(assignedDSRequestBodies), distributionsetTagId);
        log.debug("Assigned DistributionSet {}", assignedDs.size());
        return ResponseEntity.ok(MgmtDistributionSetMapper.toResponseDistributionSets(assignedDs));
    }

    @Override
    public ResponseEntity<MgmtTargetTagAssigmentResult> toggleTargetTagAssignment(
            final Long targetTagId, final List<MgmtAssignedTargetRequestBody> assignedTargetRequestBodies) {
        DEPRECATED_USAGE_LOGGER.debug("[DEPRECATED] Deprecated POST /rest/v1/targettags/{targetTagId}/assigned/toggleTagAssignment called");
        log.debug("Toggle Target assignment {} for target tag {}", assignedTargetRequestBodies.size(), targetTagId);

        final TargetTag targetTag = targetTagManagement.get(targetTagId)
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, targetTagId));
        final TargetTagAssignmentResult assigmentResult =
                DeploymentHelper.runInNewTransaction(
                        txManager,
                        "toggleDistributionSetTagAssignment",
                        status -> toggleTargetTagAssignment(findTargetControllerIds(assignedTargetRequestBodies), targetTag.getName()));

        final MgmtTargetTagAssigmentResult tagAssigmentResultRest = new MgmtTargetTagAssigmentResult();
        tagAssigmentResultRest.setAssignedTargets(
                MgmtTargetMapper.toResponse(assigmentResult.getAssignedEntity(), tenantConfigHelper));
        tagAssigmentResultRest.setUnassignedTargets(
                MgmtTargetMapper.toResponse(assigmentResult.getUnassignedEntity(), tenantConfigHelper));
        return ResponseEntity.ok(tagAssigmentResultRest);
    }

    @Override
    public ResponseEntity<List<MgmtTarget>> assignTargetsByRequestBody(
            final Long targetTagId, final List<MgmtAssignedTargetRequestBody> assignedTargetRequestBodies) {
        DEPRECATED_USAGE_LOGGER.debug("[DEPRECATED] Deprecated POST /rest/v1/targettags/{targetTagId}/assigned called");
        log.debug("Assign targets {} for target tag {}", assignedTargetRequestBodies, targetTagId);
        final List<Target> assignedTarget = targetManagement
                .assignTag(findTargetControllerIds(assignedTargetRequestBodies), targetTagId);
        return ResponseEntity.ok(MgmtTargetMapper.toResponse(assignedTarget, tenantConfigHelper));
    }

    private DistributionSetTagAssignmentResult toggleDistributionSetTagAssignment(final Collection<Long> ids, final String tagName) {
        return updateTag(
                ids,
                () -> distributionSetTagManagement
                        .findByName(tagName)
                        .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, tagName)),
                (allDs, distributionSetTag) -> {
                    final List<JpaDistributionSet> toBeChangedDSs = allDs.stream().filter(set -> set.addTag(distributionSetTag)).toList();

                    final DistributionSetTagAssignmentResult result;
                    // un-assignment case
                    if (toBeChangedDSs.isEmpty()) {
                        final List<JpaDistributionSet> toRemoveSize = new ArrayList<>();
                        for (final JpaDistributionSet set : allDs) {
                            if (set.removeTag(distributionSetTag)) {
                                toRemoveSize.add(set);
                            }
                        }
                        result = new DistributionSetTagAssignmentResult(ids.size() - toRemoveSize.size(),
                                Collections.emptyList(),
                                toRemoveSize.stream().map(distributionSetRepository::save).map(DistributionSet.class::cast).toList(),
                                distributionSetTag);
                    } else {
                        result = new DistributionSetTagAssignmentResult(ids.size() - toBeChangedDSs.size(),
                                toBeChangedDSs.stream().map(distributionSetRepository::save).map(DistributionSet.class::cast).toList(),
                                Collections.emptyList(), distributionSetTag);
                    }
                    return result;
                });
    }

    private <T> T updateTag(
            final Collection<Long> dsIds, final Supplier<DistributionSetTag> tagSupplier,
            final BiFunction<List<JpaDistributionSet>, DistributionSetTag, T> updater) {
        final List<JpaDistributionSet> allDs = dsIds.size() == 1 ?
                distributionSetRepository.findById(dsIds.iterator().next()).map(List::of).orElseGet(Collections::emptyList) :
                distributionSetRepository.findAll(DistributionSetSpecification.byIdsFetch(dsIds));
        if (allDs.size() < dsIds.size()) {
            throw new EntityNotFoundException(DistributionSet.class, dsIds, allDs.stream().map(DistributionSet::getId).toList());
        }

        final DistributionSetTag distributionSetTag = tagSupplier.get();
        try {
            return updater.apply(allDs, distributionSetTag);
        } finally {
            // No reason to save the tag
            entityManager.detach(distributionSetTag);
        }
    }

    private TargetTagAssignmentResult toggleTargetTagAssignment(final Collection<String> controllerIds, final String tagName) {
        final TargetTag tag = targetTagRepository
                .findByNameEquals(tagName)
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, tagName));
        final List<JpaTarget> allTargets = targetRepository
                .findAll(TargetSpecifications.byControllerIdWithTagsInJoin(controllerIds));
        if (allTargets.size() < controllerIds.size()) {
            throw new EntityNotFoundException(Target.class, controllerIds,
                    allTargets.stream().map(Target::getControllerId).toList());
        }

        final List<JpaTarget> alreadyAssignedTargets = targetRepository.findAll(
                TargetSpecifications.hasTagName(tagName).and(TargetSpecifications.hasControllerIdIn(controllerIds)));

        // all are already assigned -> unassign
        if (alreadyAssignedTargets.size() == allTargets.size()) {

            alreadyAssignedTargets.forEach(target -> target.removeTag(tag));
            return new TargetTagAssignmentResult(0, Collections.emptyList(),
                    Collections.unmodifiableList(alreadyAssignedTargets), tag);
        }

        allTargets.removeAll(alreadyAssignedTargets);
        // some or none are assigned -> assign
        allTargets.forEach(target -> target.addTag(tag));
        final TargetTagAssignmentResult result = new TargetTagAssignmentResult(alreadyAssignedTargets.size(),
                targetRepository.saveAll(allTargets), Collections.emptyList(), tag);

        // no reason to persist the tag
        entityManager.detach(tag);
        return result;
    }

    private static List<Long> findDistributionSetIds(
            final List<MgmtAssignedDistributionSetRequestBody> assignedDistributionSetRequestBodies) {
        return assignedDistributionSetRequestBodies.stream()
                .map(MgmtAssignedDistributionSetRequestBody::getDistributionSetId).toList();
    }

    private DistributionSetTag findDistributionTagById(final Long distributionsetTagId) {
        return distributionSetTagManagement.get(distributionsetTagId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, distributionsetTagId));
    }

    private List<String> findTargetControllerIds(
            final List<MgmtAssignedTargetRequestBody> assignedTargetRequestBodies) {
        return assignedTargetRequestBodies.stream().map(MgmtAssignedTargetRequestBody::getControllerId).toList();
    }
}