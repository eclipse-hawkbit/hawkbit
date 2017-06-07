/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.net.URI;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.MgmtPollStatus;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtAction;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtActionStatus;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetRequestBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.ActionStatusFields;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.TargetCreate;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.PollStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.data.ResponseList;
import org.eclipse.hawkbit.rest.data.SortDirection;
import org.eclipse.hawkbit.util.IpUtil;
import org.springframework.data.domain.PageRequest;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
public final class MgmtTargetMapper {

    private MgmtTargetMapper() {
        // Utility class
    }

    /**
     * Add links to a target response.
     *
     * @param response
     *            the target response
     */
    public static void addTargetLinks(final MgmtTarget response) {
        response.add(linkTo(methodOn(MgmtTargetRestApi.class).getAssignedDistributionSet(response.getControllerId()))
                .withRel(MgmtRestConstants.TARGET_V1_ASSIGNED_DISTRIBUTION_SET));
        response.add(linkTo(methodOn(MgmtTargetRestApi.class).getInstalledDistributionSet(response.getControllerId()))
                .withRel(MgmtRestConstants.TARGET_V1_INSTALLED_DISTRIBUTION_SET));
        response.add(linkTo(methodOn(MgmtTargetRestApi.class).getAttributes(response.getControllerId()))
                .withRel(MgmtRestConstants.TARGET_V1_ATTRIBUTES));
        response.add(linkTo(methodOn(MgmtTargetRestApi.class).getActionHistory(response.getControllerId(), 0,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE,
                ActionFields.ID.getFieldName() + ":" + SortDirection.DESC, null))
                        .withRel(MgmtRestConstants.TARGET_V1_ACTIONS).expand());
    }

    static void addPollStatus(final Target target, final MgmtTarget targetRest) {
        final PollStatus pollStatus = target.getPollStatus();
        if (pollStatus != null) {
            final MgmtPollStatus pollStatusRest = new MgmtPollStatus();
            pollStatusRest.setLastRequestAt(
                    Date.from(pollStatus.getLastPollDate().atZone(ZoneId.systemDefault()).toInstant()).getTime());
            pollStatusRest.setNextExpectedRequestAt(
                    Date.from(pollStatus.getNextPollDate().atZone(ZoneId.systemDefault()).toInstant()).getTime());
            pollStatusRest.setOverdue(pollStatus.isOverdue());
            targetRest.setPollStatus(pollStatusRest);
        }
    }

    /**
     * Create a response for targets.
     *
     * @param targets
     *            list of targets
     * @return the response
     */
    public static List<MgmtTarget> toResponse(final Collection<Target> targets) {
        if (targets == null) {
            return Collections.emptyList();
        }

        return new ResponseList<>(targets.stream().map(MgmtTargetMapper::toResponse).collect(Collectors.toList()));
    }

    /**
     * Create a response for target.
     *
     * @param target
     *            the target
     * @return the response
     */
    public static MgmtTarget toResponse(final Target target) {
        if (target == null) {
            return null;
        }
        final MgmtTarget targetRest = new MgmtTarget();
        targetRest.setControllerId(target.getControllerId());
        targetRest.setDescription(target.getDescription());
        targetRest.setName(target.getName());
        targetRest.setUpdateStatus(target.getUpdateStatus().name().toLowerCase());

        final URI address = target.getAddress();
        if (address != null) {
            if (IpUtil.isIpAddresKnown(address)) {
                targetRest.setIpAddress(address.getHost());
            }
            targetRest.setAddress(address.toString());
        }

        targetRest.setCreatedBy(target.getCreatedBy());
        targetRest.setLastModifiedBy(target.getLastModifiedBy());

        targetRest.setCreatedAt(target.getCreatedAt());
        targetRest.setLastModifiedAt(target.getLastModifiedAt());

        targetRest.setSecurityToken(target.getSecurityToken());

        // last target query is the last controller request date
        final Long lastTargetQuery = target.getLastTargetQuery();
        final Long installationDate = target.getInstallationDate();

        if (lastTargetQuery != null) {
            targetRest.setLastControllerRequestAt(lastTargetQuery);
        }
        if (installationDate != null) {
            targetRest.setInstalledAt(installationDate);
        }

        targetRest.add(linkTo(methodOn(MgmtTargetRestApi.class).getTarget(target.getControllerId())).withSelfRel());

        return targetRest;
    }

    static List<TargetCreate> fromRequest(final EntityFactory entityFactory,
            final Collection<MgmtTargetRequestBody> targetsRest) {
        if (targetsRest == null) {
            return Collections.emptyList();
        }

        return targetsRest.stream().map(targetRest -> fromRequest(entityFactory, targetRest))
                .collect(Collectors.toList());
    }

    private static TargetCreate fromRequest(final EntityFactory entityFactory, final MgmtTargetRequestBody targetRest) {
        return entityFactory.target().create().controllerId(targetRest.getControllerId()).name(targetRest.getName())
                .description(targetRest.getDescription()).securityToken(targetRest.getSecurityToken())
                .address(targetRest.getAddress());
    }

    static List<MgmtActionStatus> toActionStatusRestResponse(final Collection<ActionStatus> actionStatus,
            final DeploymentManagement deploymentManagement) {
        if (actionStatus == null) {
            return Collections.emptyList();
        }

        return actionStatus.stream().map(status -> toResponse(status,
                deploymentManagement.findMessagesByActionStatusId(
                        new PageRequest(0, MgmtRestConstants.REQUEST_PARAMETER_PAGING_MAX_LIMIT), status.getId())
                        .getContent()))
                .collect(Collectors.toList());
    }

    static MgmtAction toResponse(final String targetId, final Action action) {
        final MgmtAction result = new MgmtAction();

        result.setActionId(action.getId());
        result.setType(getType(action));
        if (ActionType.TIMEFORCED.equals(action.getActionType())) {
            result.setForceTime(action.getForcedTime());
        }
        result.setForceType(MgmtRestModelMapper.convertActionType(action.getActionType()));

        if (action.isActive()) {
            result.setStatus(MgmtAction.ACTION_PENDING);
        } else {
            result.setStatus(MgmtAction.ACTION_FINISHED);
        }

        MgmtRestModelMapper.mapBaseToBase(result, action);

        result.add(linkTo(methodOn(MgmtTargetRestApi.class).getAction(targetId, action.getId())).withSelfRel());

        return result;
    }

    static MgmtAction toResponseWithLinks(final String controllerId, final Action action) {
        final MgmtAction result = toResponse(controllerId, action);

        if (action.isCancelingOrCanceled()) {
            result.add(linkTo(methodOn(MgmtTargetRestApi.class).getAction(controllerId, action.getId()))
                    .withRel(MgmtRestConstants.TARGET_V1_CANCELED_ACTION));
        } else {
            result.add(linkTo(
                    methodOn(MgmtDistributionSetRestApi.class).getDistributionSet(action.getDistributionSet().getId()))
                            .withRel("distributionset"));
        }

        result.add(linkTo(methodOn(MgmtTargetRestApi.class).getActionStatusList(controllerId, action.getId(), 0,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE,
                ActionStatusFields.ID.getFieldName() + ":" + SortDirection.DESC))
                        .withRel(MgmtRestConstants.TARGET_V1_ACTION_STATUS));

        return result;
    }

    static List<MgmtAction> toResponse(final String targetId, final Collection<Action> actions) {
        if (actions == null) {
            return Collections.emptyList();
        }

        return actions.stream().map(action -> toResponse(targetId, action)).collect(Collectors.toList());
    }

    private static String getType(final Action action) {
        if (!action.isCancelingOrCanceled()) {
            return MgmtAction.ACTION_UPDATE;
        } else if (action.isCancelingOrCanceled()) {
            return MgmtAction.ACTION_CANCEL;
        }

        return null;
    }

    private static MgmtActionStatus toResponse(final ActionStatus actionStatus, final List<String> messages) {
        final MgmtActionStatus result = new MgmtActionStatus();

        result.setMessages(messages);
        result.setReportedAt(actionStatus.getCreatedAt());
        result.setStatusId(actionStatus.getId());
        result.setType(actionStatus.getStatus().name().toLowerCase());

        return result;
    }

}
