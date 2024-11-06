/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtAssignedTargetRequestBody;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTag;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.json.model.tag.MgmtTargetTagAssigmentResult;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.util.PagingUtility;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling for tag CRUD operations.
 */
@Slf4j
@RestController
public class MgmtTargetTagResource implements MgmtTargetTagRestApi {

    private final TargetTagManagement tagManagement;

    private final TargetManagement targetManagement;

    private final EntityFactory entityFactory;

    private final TenantConfigHelper tenantConfigHelper;

    MgmtTargetTagResource(final TargetTagManagement tagManagement, final TargetManagement targetManagement,
            final EntityFactory entityFactory, final SystemSecurityContext securityContext,
            final TenantConfigurationManagement configurationManagement) {
        this.tagManagement = tagManagement;
        this.targetManagement = targetManagement;
        this.entityFactory = entityFactory;
        this.tenantConfigHelper = TenantConfigHelper.usingContext(securityContext, configurationManagement);
    }

    @Override
    public ResponseEntity<PagedList<MgmtTag>> getTargetTags(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTagSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        Page<TargetTag> findTargetsAll;
        if (rsqlParam == null) {
            findTargetsAll = this.tagManagement.findAll(pageable);

        } else {
            findTargetsAll = this.tagManagement.findByRsql(pageable, rsqlParam);

        }

        final List<MgmtTag> rest = MgmtTagMapper.toResponse(findTargetsAll.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, findTargetsAll.getTotalElements()));
    }

    @Override
    public ResponseEntity<MgmtTag> getTargetTag(@PathVariable("targetTagId") final Long targetTagId) {
        final TargetTag tag = findTargetTagById(targetTagId);

        final MgmtTag response = MgmtTagMapper.toResponse(tag);
        MgmtTagMapper.addLinks(tag, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<MgmtTag>> createTargetTags(@RequestBody final List<MgmtTagRequestBodyPut> tags) {
        log.debug("creating {} target tags", tags.size());
        final List<TargetTag> createdTargetTags = this.tagManagement
                .create(MgmtTagMapper.mapTagFromRequest(entityFactory, tags));
        return new ResponseEntity<>(MgmtTagMapper.toResponse(createdTargetTags), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<MgmtTag> updateTargetTag(@PathVariable("targetTagId") final Long targetTagId,
            @RequestBody final MgmtTagRequestBodyPut restTargetTagRest) {
        log.debug("update {} target tag", restTargetTagRest);

        final TargetTag updateTargetTag = tagManagement
                .update(entityFactory.tag().update(targetTagId).name(restTargetTagRest.getName())
                        .description(restTargetTagRest.getDescription()).colour(restTargetTagRest.getColour()));

        log.debug("target tag updated");

        final MgmtTag response = MgmtTagMapper.toResponse(updateTargetTag);
        MgmtTagMapper.addLinks(updateTargetTag, response);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> deleteTargetTag(@PathVariable("targetTagId") final Long targetTagId) {
        log.debug("Delete {} target tag", targetTagId);
        final TargetTag targetTag = findTargetTagById(targetTagId);

        this.tagManagement.delete(targetTag.getName());

        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(@PathVariable("targetTagId") final Long targetTagId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<Target> findTargetsAll;
        if (rsqlParam == null) {
            findTargetsAll = targetManagement.findByTag(pageable, targetTagId);
        } else {
            findTargetsAll = targetManagement.findByRsqlAndTag(pageable, rsqlParam, targetTagId);
        }

        final List<MgmtTarget> rest = MgmtTargetMapper.toResponse(findTargetsAll.getContent(), tenantConfigHelper);
        return ResponseEntity.ok(new PagedList<>(rest, findTargetsAll.getTotalElements()));
    }

    @Override
    public ResponseEntity<Void> assignTarget(final Long targetTagId, final String controllerId) {
        log.debug("Assign target {} for target tag {}", controllerId, targetTagId);
        this.targetManagement.assignTag(List.of(controllerId), targetTagId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> assignTargets(final Long targetTagId, final OnNotFoundPolicy onNotFoundPolicy,
            final List<String> controllerIds) {
        log.debug("Assign {} targets for target tag {}", controllerIds.size(), targetTagId);
        if (onNotFoundPolicy == OnNotFoundPolicy.FAIL) {
            this.targetManagement.assignTag(controllerIds, targetTagId);
        } else {
            final AtomicReference<Collection<String>> notFound = new AtomicReference<>();
            this.targetManagement.assignTag(controllerIds, targetTagId, notFound::set);
            if (notFound.get() != null) {
                // has not found
                if (onNotFoundPolicy == OnNotFoundPolicy.ON_WHAT_FOUND_AND_FAIL) {
                    throw new EntityNotFoundException(Target.class, notFound.get());
                } // else - success
            }
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> unassignTarget(final Long targetTagId,
            @PathVariable("controllerId") final String controllerId) {
        log.debug("Unassign target {} for target tag {}", controllerId, targetTagId);
        this.targetManagement.unassignTag(controllerId, targetTagId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Void> unassignTargets(final Long targetTagId, final OnNotFoundPolicy onNotFoundPolicy,
            final List<String> controllerIds) {
        log.debug("Unassign {} targets for target tag {}", controllerIds.size(), targetTagId);
        if (onNotFoundPolicy == OnNotFoundPolicy.FAIL) {
            this.targetManagement.unassignTag(controllerIds, targetTagId);
        } else {
            final AtomicReference<Collection<String>> notFound = new AtomicReference<>();
            this.targetManagement.unassignTag(controllerIds, targetTagId, notFound::set);
            if (notFound.get() != null) {
                // has not found
                if (onNotFoundPolicy == OnNotFoundPolicy.ON_WHAT_FOUND_AND_FAIL) {
                    throw new EntityNotFoundException(Target.class, notFound.get());
                } // else - success
            }
        }
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MgmtTargetTagAssigmentResult> toggleTagAssignment(
            @PathVariable("targetTagId") final Long targetTagId,
            @RequestBody final List<MgmtAssignedTargetRequestBody> assignedTargetRequestBodies) {
        log.debug("Toggle Target assignment {} for target tag {}", assignedTargetRequestBodies.size(), targetTagId);

        final TargetTag targetTag = findTargetTagById(targetTagId);
        final TargetTagAssignmentResult assigmentResult = this.targetManagement
                .toggleTagAssignment(findTargetControllerIds(assignedTargetRequestBodies), targetTag.getName());

        final MgmtTargetTagAssigmentResult tagAssigmentResultRest = new MgmtTargetTagAssigmentResult();
        tagAssigmentResultRest.setAssignedTargets(
                MgmtTargetMapper.toResponse(assigmentResult.getAssignedEntity(), tenantConfigHelper));
        tagAssigmentResultRest.setUnassignedTargets(
                MgmtTargetMapper.toResponse(assigmentResult.getUnassignedEntity(), tenantConfigHelper));
        return ResponseEntity.ok(tagAssigmentResultRest);
    }

    @Override
    public ResponseEntity<List<MgmtTarget>> assignTargetsByRequestBody(@PathVariable("targetTagId") final Long targetTagId,
            @RequestBody final List<MgmtAssignedTargetRequestBody> assignedTargetRequestBodies) {
        log.debug("Assign targets {} for target tag {}", assignedTargetRequestBodies, targetTagId);
        final List<Target> assignedTarget = this.targetManagement
                .assignTag(findTargetControllerIds(assignedTargetRequestBodies), targetTagId);
        return ResponseEntity.ok(MgmtTargetMapper.toResponse(assignedTarget, tenantConfigHelper));
    }

    private TargetTag findTargetTagById(final Long targetTagId) {
        return tagManagement.get(targetTagId)
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, targetTagId));
    }

    private List<String> findTargetControllerIds(
            final List<MgmtAssignedTargetRequestBody> assignedTargetRequestBodies) {
        return assignedTargetRequestBodies.stream().map(MgmtAssignedTargetRequestBody::getControllerId)
                .collect(Collectors.toList());
    }
}