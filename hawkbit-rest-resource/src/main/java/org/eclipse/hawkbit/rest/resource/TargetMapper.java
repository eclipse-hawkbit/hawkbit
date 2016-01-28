/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.net.URI;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo.PollStatus;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.rest.resource.model.PollStatusRest;
import org.eclipse.hawkbit.rest.resource.model.action.ActionRest;
import org.eclipse.hawkbit.rest.resource.model.action.ActionStatusRest;
import org.eclipse.hawkbit.rest.resource.model.action.ActionStatussRest;
import org.eclipse.hawkbit.rest.resource.model.action.ActionsRest;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRequestBody;
import org.eclipse.hawkbit.rest.resource.model.target.TargetRest;
import org.eclipse.hawkbit.rest.resource.model.target.TargetsRest;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
final public class TargetMapper {

    private TargetMapper() {
        // Utility class
    }

    /**
     * Add links to a target response.
     * 
     * @param response
     *            the target response
     */
    public static void addTargetLinks(final TargetRest response) {
        response.add(linkTo(methodOn(TargetResource.class).getAssignedDistributionSet(response.getControllerId()))
                .withRel(RestConstants.TARGET_V1_ASSIGNED_DISTRIBUTION_SET));
        response.add(linkTo(methodOn(TargetResource.class).getInstalledDistributionSet(response.getControllerId()))
                .withRel(RestConstants.TARGET_V1_INSTALLED_DISTRIBUTION_SET));
        response.add(linkTo(methodOn(TargetResource.class).getAttributes(response.getControllerId()))
                .withRel(RestConstants.TARGET_V1_ATTRIBUTES));
        response.add(linkTo(methodOn(TargetResource.class).getActionHistory(response.getControllerId(), 0,
                RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE,
                ActionFields.ID.getFieldName() + ":" + SortDirection.DESC, null))
                        .withRel(RestConstants.TARGET_V1_ACTIONS));
    }

    /**
     * Add the pollstatus to a target response.
     * 
     * @param target
     *            the target
     * @param targetRest
     *            the response
     */
    public static void addPollStatus(final Target target, final TargetRest targetRest) {
        final PollStatus pollStatus = target.getTargetInfo().getPollStatus();
        if (pollStatus != null) {
            final PollStatusRest pollStatusRest = new PollStatusRest();
            pollStatusRest.setLastRequestAt(
                    Date.from(pollStatus.getLastPollDate().atZone(ZoneId.systemDefault()).toInstant()).getTime());
            pollStatusRest.setNextExpectedRequestAt(
                    Date.from(pollStatus.getNextPollDate().atZone(ZoneId.systemDefault()).toInstant()).getTime());
            pollStatusRest.setOverdue(pollStatus.isOverdue());
            targetRest.setPollStatus(pollStatusRest);
        }
    }

    /**
     * Create a response which includes links and pollstatus for all targets.
     * 
     * @param targets
     *            the targets
     * @return the response
     */
    public static TargetsRest toResponseWithLinksAndPollStatus(final Iterable<Target> targets) {
        final TargetsRest mappedList = new TargetsRest();
        if (targets != null) {
            for (final Target target : targets) {
                final TargetRest response = toResponse(target);
                addPollStatus(target, response);
                addTargetLinks(response);
                mappedList.add(response);
            }
        }
        return mappedList;
    }

    /**
     * Create a response for targets.
     * 
     * @param targets
     *            list of targets
     * @return the response
     */
    public static TargetsRest toResponse(final Iterable<Target> targets) {
        final TargetsRest mappedList = new TargetsRest();
        if (targets != null) {
            for (final Target target : targets) {
                final TargetRest response = toResponse(target);
                mappedList.add(response);
            }
        }
        return mappedList;
    }

    /**
     * Create a response for target.
     * 
     * @param target
     *            the target
     * @return the response
     */
    public static TargetRest toResponse(final Target target) {
        if (target == null) {
            return null;
        }
        final TargetRest targetRest = new TargetRest();
        targetRest.setControllerId(target.getControllerId());
        targetRest.setDescription(target.getDescription());
        targetRest.setName(target.getName());
        targetRest.setUpdateStatus(getUpdateStatusName(target.getTargetInfo().getUpdateStatus()));

        final URI address = target.getTargetInfo().getAddress();
        if (address != null) {
            targetRest.setIpAddress(address.getHost());
            targetRest.setAddress(address.toString());
        }

        targetRest.setCreatedBy(target.getCreatedBy());
        targetRest.setLastModifiedBy(target.getLastModifiedBy());

        targetRest.setCreatedAt(target.getCreatedAt());
        targetRest.setLastModifiedAt(target.getLastModifiedAt());

        targetRest.setSecurityToken(target.getSecurityToken());

        // last target query is the last controller request date
        final Long lastTargetQuery = target.getTargetInfo().getLastTargetQuery();
        final Long installationDate = target.getTargetInfo().getInstallationDate();

        if (lastTargetQuery != null) {
            targetRest.setLastControllerRequestAt(lastTargetQuery);
        }
        if (installationDate != null) {
            targetRest.setInstalledAt(installationDate);
        }

        targetRest.add(linkTo(methodOn(TargetResource.class).getTarget(target.getControllerId())).withRel("self"));

        return targetRest;
    }

    static List<Target> fromRequest(final Iterable<TargetRequestBody> targetsRest) {
        final List<Target> mappedList = new ArrayList<>();
        for (final TargetRequestBody targetRest : targetsRest) {
            mappedList.add(fromRequest(targetRest));
        }
        return mappedList;
    }

    static Target fromRequest(final TargetRequestBody targetRest) {
        final Target target = new Target(targetRest.getControllerId());
        target.setDescription(targetRest.getDescription());
        target.setName(targetRest.getName());
        return target;
    }

    static ActionStatussRest toActionStatusRestResponse(final Action action, final List<ActionStatus> actionStatus) {
        final ActionStatussRest mappedList = new ActionStatussRest();

        if (actionStatus != null) {
            for (final ActionStatus status : actionStatus) {
                final ActionStatusRest response = toResponse(action, status);
                mappedList.add(response);
            }
        }

        return mappedList;
    }

    static ActionRest toResponse(final String targetId, final Action action, final boolean isActive) {
        final ActionRest result = new ActionRest();

        result.setActionId(action.getId());
        result.setType(getType(action));

        if (isActive) {
            result.setStatus(ActionRest.ACTION_PENDING);
        } else {
            result.setStatus(ActionRest.ACTION_FINISHED);
        }

        RestModelMapper.mapBaseToBase(result, action);

        result.add(linkTo(methodOn(TargetResource.class).getAction(targetId, action.getId())).withRel("self"));

        return result;
    }

    static ActionsRest toResponse(final String targetId, final List<Action> actions) {
        final ActionsRest mappedList = new ActionsRest();

        for (final Action action : actions) {
            final ActionRest response = toResponse(targetId, action, action.isActive());
            mappedList.add(response);
        }
        return mappedList;
    }

    private static String getNameOfActionStatusType(final Action.Status type) {
        String result;

        switch (type) {
        case CANCELED:
            result = ActionStatusRest.AS_CANCELED;
            break;
        case ERROR:
            result = ActionStatusRest.AS_ERROR;
            break;
        case FINISHED:
            result = ActionStatusRest.AS_FINISHED;
            break;
        case RETRIEVED:
            result = ActionStatusRest.AS_RETRIEVED;
            break;
        case RUNNING:
            result = ActionStatusRest.AS_RUNNING;
            break;
        case WARNING:
            result = ActionStatusRest.AS_WARNING;
            break;
        default:
            return type.name().toLowerCase();

        }

        return result;

    }

    private static String getType(final Action action) {
        if (!action.isCancelingOrCanceled()) {
            return ActionRest.ACTION_UPDATE;
        } else if (action.isCancelingOrCanceled()) {
            return ActionRest.ACTION_CANCEL;
        }

        return null;
    }

    private static String getUpdateStatusName(final TargetUpdateStatus updatestatus) {
        String result;

        switch (updatestatus) {
        case ERROR:
            result = "error";
            break;
        case IN_SYNC:
            result = "in_sync";
            break;
        case PENDING:
            result = "pending";
            break;
        case REGISTERED:
            result = "registered";
            break;
        case UNKNOWN:
            result = "unknown";
            break;
        default:
            return updatestatus.name().toLowerCase();
        }

        return result;
    }

    private static ActionStatusRest toResponse(final Action action, final ActionStatus actionStatus) {
        final ActionStatusRest result = new ActionStatusRest();

        result.setMessages(actionStatus.getMessages());
        result.setReportedAt(action.getCreatedAt());
        result.setStatusId(action.getId());
        result.setType(getNameOfActionStatusType(actionStatus.getStatus()));

        return result;
    }

}
