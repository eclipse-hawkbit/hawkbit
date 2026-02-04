/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mcp.server.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.mcp.server.config.HawkbitMcpProperties;
import org.eclipse.hawkbit.mcp.server.dto.ActionRequest;
import org.eclipse.hawkbit.mcp.server.dto.DistributionSetRequest;
import org.eclipse.hawkbit.mcp.server.dto.ListRequest;
import org.eclipse.hawkbit.mcp.server.dto.OperationResponse;
import org.eclipse.hawkbit.mcp.server.dto.PagedResponse;
import org.eclipse.hawkbit.mcp.server.dto.RolloutRequest;
import org.eclipse.hawkbit.mcp.server.dto.SoftwareModuleRequest;
import org.eclipse.hawkbit.mcp.server.dto.TargetFilterRequest;
import org.eclipse.hawkbit.mcp.server.dto.TargetRequest;
import org.eclipse.hawkbit.mgmt.json.model.PagedList;
import org.eclipse.hawkbit.mgmt.json.model.action.MgmtAction;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.rollout.MgmtRolloutResponseBody;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModule;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.json.model.targetfilter.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtActionRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRolloutRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetFilterQueryRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.eclipse.hawkbit.sdk.HawkbitClient;
import org.eclipse.hawkbit.sdk.Tenant;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.List;

/**
 * MCP tools for hawkBit using the SDK.
 * <p>
 * Provides tools for managing targets, rollouts, distribution sets, actions,
 * software modules, and target filter queries via the hawkBit REST API.
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class HawkbitMcpToolProvider {

    private static final String OP_CREATE = "CREATE";
    private static final String OP_UPDATE = "UPDATE";
    private static final String OP_DELETE = "DELETE";
    private static final String OP_DELETE_BATCH = "DELETE_BATCH";
    private static final String OP_START = "START";
    private static final String OP_PAUSE = "PAUSE";
    private static final String OP_STOP = "STOP";
    private static final String OP_RESUME = "RESUME";
    private static final String OP_APPROVE = "APPROVE";
    private static final String OP_DENY = "DENY";
    private static final String OP_RETRY = "RETRY";
    private static final String OP_TRIGGER_NEXT_GROUP = "TRIGGER_NEXT_GROUP";

    private final HawkbitClient hawkbitClient;
    private final Tenant dummyTenant;
    private final HawkbitMcpProperties properties;

    private <T> PagedResponse<T> toPagedResponse(final PagedList<T> pagedList, final ListRequest request) {
        if (pagedList == null) {
            return PagedResponse.of(
                    Collections.emptyList(),
                    0L,
                    request.getOffsetOrDefault(),
                    request.getLimitOrDefault());
        }
        return PagedResponse.of(
                pagedList.getContent(),
                pagedList.getTotal(),
                request.getOffsetOrDefault(),
                request.getLimitOrDefault());
    }

    @McpTool(name = "list_targets",
          description = "Retrieves a paged list of targets (devices) with optional RSQL filtering. " +
                        "Targets represent devices that can receive software updates.")
    public PagedResponse<MgmtTarget> listTargets(final ListRequest request) {
        log.debug("Listing targets with rsql={}, offset={}, limit={}",
                request.rsql(), request.getOffsetOrDefault(), request.getLimitOrDefault());

        MgmtTargetRestApi targetApi = hawkbitClient.mgmtService(MgmtTargetRestApi.class, dummyTenant);
        ResponseEntity<PagedList<MgmtTarget>> response = targetApi.getTargets(
                request.getRsqlOrNull(),
                request.getOffsetOrDefault(),
                request.getLimitOrDefault(),
                null);

        return toPagedResponse(response.getBody(), request);
    }

    @McpTool(name = "list_rollouts",
          description = "Retrieves a paged list of rollouts with optional RSQL filtering. " +
                        "Rollouts are used to deploy software to groups of targets.")
    public PagedResponse<MgmtRolloutResponseBody> listRollouts(final ListRequest request) {
        log.debug("Listing rollouts with rsql={}, offset={}, limit={}",
                request.rsql(), request.getOffsetOrDefault(), request.getLimitOrDefault());

        MgmtRolloutRestApi rolloutApi = hawkbitClient.mgmtService(MgmtRolloutRestApi.class, dummyTenant);
        ResponseEntity<PagedList<MgmtRolloutResponseBody>> response = rolloutApi.getRollouts(
                request.getRsqlOrNull(),
                request.getOffsetOrDefault(),
                request.getLimitOrDefault(),
                null,
                null);

        return toPagedResponse(response.getBody(), request);
    }

    @McpTool(name = "list_distribution_sets",
          description = "Retrieves a paged list of distribution sets with optional RSQL filtering. " +
                        "Distribution sets are software packages that can be deployed to targets.")
    public PagedResponse<MgmtDistributionSet> listDistributionSets(final ListRequest request) {
        log.debug("Listing distribution sets with rsql={}, offset={}, limit={}",
                request.rsql(), request.getOffsetOrDefault(), request.getLimitOrDefault());

        MgmtDistributionSetRestApi dsApi = hawkbitClient.mgmtService(MgmtDistributionSetRestApi.class, dummyTenant);
        ResponseEntity<PagedList<MgmtDistributionSet>> response = dsApi.getDistributionSets(
                request.getRsqlOrNull(),
                request.getOffsetOrDefault(),
                request.getLimitOrDefault(),
                null);

        return toPagedResponse(response.getBody(), request);
    }

    @McpTool(name = "list_actions",
          description = "Retrieves a paged list of actions with optional RSQL filtering. " +
                        "Actions represent deployment operations assigned to targets.")
    public PagedResponse<MgmtAction> listActions(final ListRequest request) {
        log.debug("Listing actions with rsql={}, offset={}, limit={}",
                request.rsql(), request.getOffsetOrDefault(), request.getLimitOrDefault());

        MgmtActionRestApi actionApi = hawkbitClient.mgmtService(MgmtActionRestApi.class, dummyTenant);
        ResponseEntity<PagedList<MgmtAction>> response = actionApi.getActions(
                request.getRsqlOrNull(),
                request.getOffsetOrDefault(),
                request.getLimitOrDefault(),
                null,
                null);

        return toPagedResponse(response.getBody(), request);
    }

    @McpTool(name = "list_software_modules",
          description = "Retrieves a paged list of software modules with optional RSQL filtering. " +
                        "Software modules are individual software components within distribution sets.")
    public PagedResponse<MgmtSoftwareModule> listSoftwareModules(final ListRequest request) {
        log.debug("Listing software modules with rsql={}, offset={}, limit={}",
                request.rsql(), request.getOffsetOrDefault(), request.getLimitOrDefault());

        MgmtSoftwareModuleRestApi smApi = hawkbitClient.mgmtService(MgmtSoftwareModuleRestApi.class, dummyTenant);
        ResponseEntity<PagedList<MgmtSoftwareModule>> response = smApi.getSoftwareModules(
                request.getRsqlOrNull(),
                request.getOffsetOrDefault(),
                request.getLimitOrDefault(),
                null);

        return toPagedResponse(response.getBody(), request);
    }

    @McpTool(name = "list_target_filters",
          description = "Retrieves a paged list of target filter queries with optional RSQL filtering. " +
                        "Target filters define RSQL queries to group targets for rollouts or auto-assignment.")
    public PagedResponse<MgmtTargetFilterQuery> listTargetFilters(final ListRequest request) {
        log.debug("Listing target filters with rsql={}, offset={}, limit={}",
                request.rsql(), request.getOffsetOrDefault(), request.getLimitOrDefault());

        MgmtTargetFilterQueryRestApi filterApi = hawkbitClient.mgmtService(MgmtTargetFilterQueryRestApi.class, dummyTenant);
        ResponseEntity<PagedList<MgmtTargetFilterQuery>> response = filterApi.getFilters(
                request.getRsqlOrNull(),
                request.getOffsetOrDefault(),
                request.getLimitOrDefault(),
                null,
                null);

        return toPagedResponse(response.getBody(), request);
    }

    @McpTool(name = "manage_target",
             description = "Create, update, or delete targets (devices). " +
                           "Operations: CREATE (new target with controllerId, name, description. When creating a target without a specific target type, set \"targetType\": null), " +
                           "UPDATE (modify existing target by controllerId), " +
                           "DELETE (remove target by controllerId). " +
                           "Use 'type' field to select operation: " +
                           "{\"type\":\"Create\",\"body\":{\"controllerId\":\"id\",\"name\":\"name\"}}, " +
                           "{\"type\":\"Update\",\"controllerId\":\"id\",\"body\":{...}}, " +
                           "{\"type\":\"Delete\",\"controllerId\":\"id\"}")
    public OperationResponse<Object> manageTarget(final TargetRequest request) {
        log.debug("Managing target: request={}", request.getClass().getSimpleName());

        final MgmtTargetRestApi api = hawkbitClient.mgmtService(MgmtTargetRestApi.class, dummyTenant);

        if (request instanceof TargetRequest.Create r) {
            validateOperation("create", "targets");
            if (r.body() == null) {
                return OperationResponse.failure(OP_CREATE, "Request body is required for CREATE operation");
            }
            final ResponseEntity<List<MgmtTarget>> response = api.createTargets(List.of(r.body()));
            final List<MgmtTarget> created = response.getBody();
            return OperationResponse.success(OP_CREATE, "Target created successfully",
                    created != null && !created.isEmpty() ? created.get(0) : null);
        } else if (request instanceof TargetRequest.Update r) {
            validateOperation("update", "targets");
            if (r.controllerId() == null || r.controllerId().isBlank()) {
                return OperationResponse.failure(OP_UPDATE, "controllerId is required for UPDATE operation");
            }
            if (r.body() == null) {
                return OperationResponse.failure(OP_UPDATE, "Request body is required for UPDATE operation");
            }
            final ResponseEntity<MgmtTarget> response = api.updateTarget(r.controllerId(), r.body());
            return OperationResponse.success(OP_UPDATE, "Target updated successfully", response.getBody());
        } else if (request instanceof TargetRequest.Delete r) {
            validateOperation("delete", "targets");
            if (r.controllerId() == null || r.controllerId().isBlank()) {
                return OperationResponse.failure(OP_DELETE, "controllerId is required for DELETE operation");
            }
            api.deleteTarget(r.controllerId());
            return OperationResponse.success(OP_DELETE, "Target deleted successfully");
        }
        throw new IllegalArgumentException("Unknown request type: " + request.getClass().getSimpleName());
    }

    @McpTool(name = "manage_rollout",
             description = "Create, update, delete, and control rollouts for software deployment. " +
                           "Use 'type' field to select operation. " +
                           "Types: Create, Update, Delete, Start, Pause, Stop, Resume, Approve, Deny, Retry, TriggerNextGroup. " +
                           "For Create: use rollout 'type' values: 'soft', 'forced', 'timeforced', 'downloadonly' (lowercase). " +
                           "If 'groups' list is provided, omit 'amountGroups' (they are mutually exclusive). " +
                           "Examples: {\"type\":\"Create\",\"body\":{...}}, " +
                           "{\"type\":\"Start\",\"rolloutId\":123}, " +
                           "{\"type\":\"Approve\",\"rolloutId\":123,\"remark\":\"approved\"}")
    public OperationResponse<Object> manageRollout(final RolloutRequest request) {
        log.debug("Managing rollout: request={}", request.getClass().getSimpleName());

        final MgmtRolloutRestApi api = hawkbitClient.mgmtService(MgmtRolloutRestApi.class, dummyTenant);

        if (request instanceof RolloutRequest.Create r) {
            validateRolloutOperation("create");
            if (r.body() == null) {
                return OperationResponse.failure(OP_CREATE, "body is required for CREATE operation");
            }
            final ResponseEntity<MgmtRolloutResponseBody> response = api.create(r.body());
            return OperationResponse.success(OP_CREATE, "Rollout created successfully", response.getBody());
        } else if (request instanceof RolloutRequest.Update r) {
            validateRolloutOperation("update");
            if (r.rolloutId() == null) {
                return OperationResponse.failure(OP_UPDATE, "rolloutId is required for UPDATE operation");
            }
            if (r.body() == null) {
                return OperationResponse.failure(OP_UPDATE, "body is required for UPDATE operation");
            }
            final ResponseEntity<MgmtRolloutResponseBody> response = api.update(r.rolloutId(), r.body());
            return OperationResponse.success(OP_UPDATE, "Rollout updated successfully", response.getBody());
        } else if (request instanceof RolloutRequest.Delete r) {
            validateRolloutOperation("delete");
            if (r.rolloutId() == null) {
                return OperationResponse.failure(OP_DELETE, "rolloutId is required for DELETE operation");
            }
            api.delete(r.rolloutId());
            return OperationResponse.success(OP_DELETE, "Rollout deleted successfully");
        } else if (request instanceof RolloutRequest.Start r) {
            validateRolloutOperation("start");
            if (r.rolloutId() == null) {
                return OperationResponse.failure(OP_START, "rolloutId is required for START operation");
            }
            api.start(r.rolloutId());
            return OperationResponse.success(OP_START, "Rollout started successfully");
        } else if (request instanceof RolloutRequest.Pause r) {
            validateRolloutOperation("pause");
            if (r.rolloutId() == null) {
                return OperationResponse.failure(OP_PAUSE, "rolloutId is required for PAUSE operation");
            }
            api.pause(r.rolloutId());
            return OperationResponse.success(OP_PAUSE, "Rollout paused successfully");
        } else if (request instanceof RolloutRequest.Stop r) {
            validateRolloutOperation("stop");
            if (r.rolloutId() == null) {
                return OperationResponse.failure(OP_STOP, "rolloutId is required for STOP operation");
            }
            api.stop(r.rolloutId());
            return OperationResponse.success(OP_STOP, "Rollout stopped successfully");
        } else if (request instanceof RolloutRequest.Resume r) {
            validateRolloutOperation("resume");
            if (r.rolloutId() == null) {
                return OperationResponse.failure(OP_RESUME, "rolloutId is required for RESUME operation");
            }
            api.resume(r.rolloutId());
            return OperationResponse.success(OP_RESUME, "Rollout resumed successfully");
        } else if (request instanceof RolloutRequest.Approve r) {
            validateRolloutOperation("approve");
            if (r.rolloutId() == null) {
                return OperationResponse.failure(OP_APPROVE, "rolloutId is required for APPROVE operation");
            }
            api.approve(r.rolloutId(), r.remark());
            return OperationResponse.success(OP_APPROVE, "Rollout approved successfully");
        } else if (request instanceof RolloutRequest.Deny r) {
            validateRolloutOperation("deny");
            if (r.rolloutId() == null) {
                return OperationResponse.failure(OP_DENY, "rolloutId is required for DENY operation");
            }
            api.deny(r.rolloutId(), r.remark());
            return OperationResponse.success(OP_DENY, "Rollout denied successfully");
        } else if (request instanceof RolloutRequest.Retry r) {
            validateRolloutOperation("retry");
            if (r.rolloutId() == null) {
                return OperationResponse.failure(OP_RETRY, "rolloutId is required for RETRY operation");
            }
            final ResponseEntity<MgmtRolloutResponseBody> response = api.retryRollout(r.rolloutId());
            return OperationResponse.success(OP_RETRY, "Rollout retry created successfully", response.getBody());
        } else if (request instanceof RolloutRequest.TriggerNextGroup r) {
            validateRolloutOperation("trigger-next-group");
            if (r.rolloutId() == null) {
                return OperationResponse.failure(OP_TRIGGER_NEXT_GROUP, "rolloutId is required for TRIGGER_NEXT_GROUP operation");
            }
            api.triggerNextGroup(r.rolloutId());
            return OperationResponse.success(OP_TRIGGER_NEXT_GROUP, "Next rollout group triggered successfully");
        }
        throw new IllegalArgumentException("Unknown request type: " + request.getClass().getSimpleName());
    }

    @McpTool(name = "manage_distribution_set",
             description = "Create, update, or delete distribution sets (software packages). " +
                           "Use 'type' field to select operation: " +
                           "{\"type\":\"Create\",\"body\":{\"name\":\"n\",\"version\":\"v\",\"type\":\"t\"}}, " +
                           "{\"type\":\"Update\",\"distributionSetId\":123,\"body\":{...}}, " +
                           "{\"type\":\"Delete\",\"distributionSetId\":123}")
    public OperationResponse<Object> manageDistributionSet(final DistributionSetRequest request) {
        log.debug("Managing distribution set: request={}", request.getClass().getSimpleName());

        final MgmtDistributionSetRestApi api = hawkbitClient.mgmtService(MgmtDistributionSetRestApi.class, dummyTenant);

        if (request instanceof DistributionSetRequest.Create r) {
            validateOperation("create", "distributionSets");
            if (r.body() == null) {
                return OperationResponse.failure(OP_CREATE, "body is required for CREATE operation");
            }
            final ResponseEntity<List<MgmtDistributionSet>> response = api.createDistributionSets(List.of(r.body()));
            final List<MgmtDistributionSet> created = response.getBody();
            return OperationResponse.success(OP_CREATE, "Distribution set created successfully",
                    created != null && !created.isEmpty() ? created.get(0) : null);
        } else if (request instanceof DistributionSetRequest.Update r) {
            validateOperation("update", "distributionSets");
            if (r.distributionSetId() == null) {
                return OperationResponse.failure(OP_UPDATE, "distributionSetId is required for UPDATE operation");
            }
            if (r.body() == null) {
                return OperationResponse.failure(OP_UPDATE, "body is required for UPDATE operation");
            }
            final ResponseEntity<MgmtDistributionSet> response = api.updateDistributionSet(r.distributionSetId(), r.body());
            return OperationResponse.success(OP_UPDATE, "Distribution set updated successfully", response.getBody());
        } else if (request instanceof DistributionSetRequest.Delete r) {
            validateOperation("delete", "distributionSets");
            if (r.distributionSetId() == null) {
                return OperationResponse.failure(OP_DELETE, "distributionSetId is required for DELETE operation");
            }
            api.deleteDistributionSet(r.distributionSetId());
            return OperationResponse.success(OP_DELETE, "Distribution set deleted successfully");
        }
        throw new IllegalArgumentException("Unknown request type: " + request.getClass().getSimpleName());
    }

    @McpTool(name = "manage_action",
             description = "Delete deployment actions. Actions are created indirectly via distribution set assignment. " +
                           "Use 'type' field to select operation: " +
                           "{\"type\":\"Delete\",\"actionIds\":[123]}, " +
                           "{\"type\":\"DeleteBatch\",\"actionIds\":[1,2,3],\"rsql\":\"\"}")
    public OperationResponse<Object> manageAction(final ActionRequest request) {
        log.debug("Managing action: request={}", request.getClass().getSimpleName());

        final MgmtActionRestApi api = hawkbitClient.mgmtService(MgmtActionRestApi.class, dummyTenant);

        if (request instanceof ActionRequest.Delete r) {
            validateActionOperation("delete");
            if (r.actionId() == null) {
                return OperationResponse.failure(OP_DELETE, "actionId is required for DELETE operation");
            }
            api.deleteAction(r.actionId());
            return OperationResponse.success(OP_DELETE, "Action deleted successfully");
        } else if (request instanceof ActionRequest.DeleteBatch r) {
            validateActionOperation("delete-batch");
            if ((r.actionIds() == null || r.actionIds().isEmpty()) &&
                (r.rsql() == null || r.rsql().isBlank())) {
                return OperationResponse.failure(OP_DELETE_BATCH, "Either actionIds or rsql is required for DELETE_BATCH operation");
            }
            api.deleteActions(r.rsql(), r.actionIds());
            return OperationResponse.success(OP_DELETE_BATCH, "Actions deleted successfully");
        }
        throw new IllegalArgumentException("Unknown request type: " + request.getClass().getSimpleName());
    }

    @McpTool(name = "manage_software_module",
             description = "Create, update, or delete software modules. " +
                           "Use 'type' field to select operation: " +
                           "{\"type\":\"Create\",\"body\":{\"name\":\"n\",\"version\":\"v\",\"type\":\"t\"}}, " +
                           "{\"type\":\"Update\",\"softwareModuleId\":123,\"body\":{...}}, " +
                           "{\"type\":\"Delete\",\"softwareModuleId\":123}")
    public OperationResponse<Object> manageSoftwareModule(final SoftwareModuleRequest request) {
        log.debug("Managing software module: request={}", request.getClass().getSimpleName());

        final MgmtSoftwareModuleRestApi api = hawkbitClient.mgmtService(MgmtSoftwareModuleRestApi.class, dummyTenant);

        if (request instanceof SoftwareModuleRequest.Create r) {
            validateOperation("create", "softwareModules");
            if (r.body() == null) {
                return OperationResponse.failure(OP_CREATE, "body is required for CREATE operation");
            }
            final ResponseEntity<List<MgmtSoftwareModule>> response = api.createSoftwareModules(List.of(r.body()));
            final List<MgmtSoftwareModule> created = response.getBody();
            return OperationResponse.success(OP_CREATE, "Software module created successfully",
                    created != null && !created.isEmpty() ? created.get(0) : null);
        } else if (request instanceof SoftwareModuleRequest.Update r) {
            validateOperation("update", "softwareModules");
            if (r.softwareModuleId() == null) {
                return OperationResponse.failure(OP_UPDATE, "softwareModuleId is required for UPDATE operation");
            }
            if (r.body() == null) {
                return OperationResponse.failure(OP_UPDATE, "body is required for UPDATE operation");
            }
            final ResponseEntity<MgmtSoftwareModule> response = api.updateSoftwareModule(r.softwareModuleId(), r.body());
            return OperationResponse.success(OP_UPDATE, "Software module updated successfully", response.getBody());
        } else if (request instanceof SoftwareModuleRequest.Delete r) {
            validateOperation("delete", "softwareModules");
            if (r.softwareModuleId() == null) {
                return OperationResponse.failure(OP_DELETE, "softwareModuleId is required for DELETE operation");
            }
            api.deleteSoftwareModule(r.softwareModuleId());
            return OperationResponse.success(OP_DELETE, "Software module deleted successfully");
        }
        throw new IllegalArgumentException("Unknown request type: " + request.getClass().getSimpleName());
    }

    @McpTool(name = "manage_target_filter",
             description = "Create, update, or delete target filter queries. " +
                           "Use 'type' field to select operation: " +
                           "{\"type\":\"Create\",\"body\":{\"name\":\"n\",\"query\":\"name==*\"}}, " +
                           "{\"type\":\"Update\",\"filterId\":123,\"body\":{...}}, " +
                           "{\"type\":\"Delete\",\"filterId\":123}")
    public OperationResponse<Object> manageTargetFilter(final TargetFilterRequest request) {
        log.debug("Managing target filter: request={}", request.getClass().getSimpleName());

        final MgmtTargetFilterQueryRestApi api = hawkbitClient.mgmtService(MgmtTargetFilterQueryRestApi.class, dummyTenant);

        if (request instanceof TargetFilterRequest.Create r) {
            validateOperation("create", "targetFilters");
            if (r.body() == null) {
                return OperationResponse.failure(OP_CREATE, "body is required for CREATE operation");
            }
            final ResponseEntity<MgmtTargetFilterQuery> response = api.createFilter(r.body());
            return OperationResponse.success(OP_CREATE, "Target filter created successfully", response.getBody());
        } else if (request instanceof TargetFilterRequest.Update r) {
            validateOperation("update", "targetFilters");
            if (r.filterId() == null) {
                return OperationResponse.failure(OP_UPDATE, "filterId is required for UPDATE operation");
            }
            if (r.body() == null) {
                return OperationResponse.failure(OP_UPDATE, "body is required for UPDATE operation");
            }
            final ResponseEntity<MgmtTargetFilterQuery> response = api.updateFilter(r.filterId(), r.body());
            return OperationResponse.success(OP_UPDATE, "Target filter updated successfully", response.getBody());
        } else if (request instanceof TargetFilterRequest.Delete r) {
            validateOperation("delete", "targetFilters");
            if (r.filterId() == null) {
                return OperationResponse.failure(OP_DELETE, "filterId is required for DELETE operation");
            }
            api.deleteFilter(r.filterId());
            return OperationResponse.success(OP_DELETE, "Target filter deleted successfully");
        }
        throw new IllegalArgumentException("Unknown request type: " + request.getClass().getSimpleName());
    }


    private void validateOperation(final String operation, final String entity) {
        if (!isOperationEnabled(operation, entity)) {
            throw new IllegalArgumentException(
                    "Operation " + operation.toUpperCase() + " is not enabled for " + entity +
                    ". Check hawkbit.mcp.operations configuration.");
        }
    }

    private void validateRolloutOperation(final String operation) {
        final HawkbitMcpProperties.RolloutConfig config = properties.getOperations().getRollouts();
        final Boolean entitySetting = config.getOperationEnabled(operation);

        // For standard CRUD ops, check global fallback
        if (entitySetting == null) {
            if (!properties.getOperations().isGlobalOperationEnabled(operation)) {
                throw new IllegalArgumentException(
                        "Operation " + operation.toUpperCase() + " is not enabled for rollouts. " +
                        "Check hawkbit.mcp.operations configuration.");
            }
            return;
        }

        if (!entitySetting) {
            throw new IllegalArgumentException(
                    "Operation " + operation.toUpperCase() + " is not enabled for rollouts. " +
                    "Check hawkbit.mcp.operations configuration.");
        }
    }

    private void validateActionOperation(final String operation) {
        final HawkbitMcpProperties.ActionConfig config = properties.getOperations().getActions();
        final Boolean entitySetting = config.getOperationEnabled(operation);

        if (entitySetting == null) {
            if (operation.equals("delete") && !properties.getOperations().isGlobalOperationEnabled("delete")) {
                throw new IllegalArgumentException(
                        "Operation " + operation.toUpperCase() + " is not enabled for actions. " +
                        "Check hawkbit.mcp.operations configuration.");
            }
            return;
        }

        if (!entitySetting) {
            throw new IllegalArgumentException(
                    "Operation " + operation.toUpperCase() + " is not enabled for actions. " +
                    "Check hawkbit.mcp.operations configuration.");
        }
    }

    private boolean isOperationEnabled(final String operation, final String entity) {
        final HawkbitMcpProperties.Operations ops = properties.getOperations();
        final HawkbitMcpProperties.EntityConfig entityConfig = getEntityConfig(entity);

        final Boolean entitySetting = entityConfig != null ? entityConfig.getOperationEnabled(operation) : null;
        if (entitySetting != null) {
            return entitySetting;
        }

        return ops.isGlobalOperationEnabled(operation);
    }

    private HawkbitMcpProperties.EntityConfig getEntityConfig(final String entity) {
        final HawkbitMcpProperties.Operations ops = properties.getOperations();
        return switch (entity.toLowerCase()) {
            case "targets" -> ops.getTargets();
            case "rollouts" -> ops.getRollouts();
            case "distributionsets" -> ops.getDistributionSets();
            case "softwaremodules" -> ops.getSoftwareModules();
            case "targetfilters" -> ops.getTargetFilters();
            default -> null;
        };
    }
}
