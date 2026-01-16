/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
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
import org.eclipse.hawkbit.mcp.server.config.HawkBitMcpProperties;
import org.eclipse.hawkbit.mcp.server.dto.ActionOperation;
import org.eclipse.hawkbit.mcp.server.dto.ListRequest;
import org.eclipse.hawkbit.mcp.server.dto.ManageActionRequest;
import org.eclipse.hawkbit.mcp.server.dto.ManageDistributionSetRequest;
import org.eclipse.hawkbit.mcp.server.dto.ManageRolloutRequest;
import org.eclipse.hawkbit.mcp.server.dto.ManageSoftwareModuleRequest;
import org.eclipse.hawkbit.mcp.server.dto.ManageTargetFilterRequest;
import org.eclipse.hawkbit.mcp.server.dto.ManageTargetRequest;
import org.eclipse.hawkbit.mcp.server.dto.Operation;
import org.eclipse.hawkbit.mcp.server.dto.OperationResponse;
import org.eclipse.hawkbit.mcp.server.dto.PagedResponse;
import org.eclipse.hawkbit.mcp.server.dto.RolloutOperation;
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
public class HawkBitMcpToolProvider {

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
    private final HawkBitMcpProperties properties;

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
                           "Operations: CREATE (new target with controllerId, name, description), " +
                           "UPDATE (modify existing target by controllerId), " +
                           "DELETE (remove target by controllerId)")
    public OperationResponse<Object> manageTarget(final ManageTargetRequest request) {
        validateOperation(request.operation(), "targets");
        log.debug("Managing target: operation={}, controllerId={}", request.operation(), request.controllerId());

        final MgmtTargetRestApi api = hawkbitClient.mgmtService(MgmtTargetRestApi.class, dummyTenant);

        return switch (request.operation()) {
            case CREATE -> {
                if (request.body() == null) {
                    yield OperationResponse.failure(OP_CREATE, "Request body is required for CREATE operation");
                }
                final ResponseEntity<List<MgmtTarget>> response = api.createTargets(List.of(request.body()));
                final List<MgmtTarget> created = response.getBody();
                yield OperationResponse.success(OP_CREATE, "Target created successfully",
                        created != null && !created.isEmpty() ? created.get(0) : null);
            }
            case UPDATE -> {
                if (request.controllerId() == null || request.controllerId().isBlank()) {
                    yield OperationResponse.failure(OP_UPDATE, "controllerId is required for UPDATE operation");
                }
                if (request.body() == null) {
                    yield OperationResponse.failure(OP_UPDATE, "Request body is required for UPDATE operation");
                }
                final ResponseEntity<MgmtTarget> response = api.updateTarget(request.controllerId(), request.body());
                yield OperationResponse.success(OP_UPDATE, "Target updated successfully", response.getBody());
            }
            case DELETE -> {
                if (request.controllerId() == null || request.controllerId().isBlank()) {
                    yield OperationResponse.failure(OP_DELETE, "controllerId is required for DELETE operation");
                }
                api.deleteTarget(request.controllerId());
                yield OperationResponse.success(OP_DELETE, "Target deleted successfully");
            }
        };
    }

    @McpTool(name = "manage_rollout",
             description = "Create, update, delete, and control rollouts for software deployment. " +
                           "Operations: CREATE (new rollout), UPDATE (modify rollout), DELETE (remove rollout), " +
                           "START (begin rollout), PAUSE, STOP, RESUME, APPROVE, DENY, RETRY, TRIGGER_NEXT_GROUP")
    public OperationResponse<Object> manageRollout(final ManageRolloutRequest request) {
        validateRolloutOperation(request.operation());
        log.debug("Managing rollout: operation={}, rolloutId={}", request.operation(), request.rolloutId());

        final MgmtRolloutRestApi api = hawkbitClient.mgmtService(MgmtRolloutRestApi.class, dummyTenant);

        return switch (request.operation()) {
            case CREATE -> {
                if (request.createBody() == null) {
                    yield OperationResponse.failure(OP_CREATE, "createBody is required for CREATE operation");
                }
                final ResponseEntity<MgmtRolloutResponseBody> response = api.create(request.createBody());
                yield OperationResponse.success(OP_CREATE, "Rollout created successfully", response.getBody());
            }
            case UPDATE -> {
                if (request.rolloutId() == null) {
                    yield OperationResponse.failure(OP_UPDATE, "rolloutId is required for UPDATE operation");
                }
                if (request.updateBody() == null) {
                    yield OperationResponse.failure(OP_UPDATE, "updateBody is required for UPDATE operation");
                }
                final ResponseEntity<MgmtRolloutResponseBody> response = api.update(request.rolloutId(), request.updateBody());
                yield OperationResponse.success(OP_UPDATE, "Rollout updated successfully", response.getBody());
            }
            case DELETE -> {
                if (request.rolloutId() == null) {
                    yield OperationResponse.failure(OP_DELETE, "rolloutId is required for DELETE operation");
                }
                api.delete(request.rolloutId());
                yield OperationResponse.success(OP_DELETE, "Rollout deleted successfully");
            }
            case START -> {
                if (request.rolloutId() == null) {
                    yield OperationResponse.failure(OP_START, "rolloutId is required for START operation");
                }
                api.start(request.rolloutId());
                yield OperationResponse.success(OP_START, "Rollout started successfully");
            }
            case PAUSE -> {
                if (request.rolloutId() == null) {
                    yield OperationResponse.failure(OP_PAUSE, "rolloutId is required for PAUSE operation");
                }
                api.pause(request.rolloutId());
                yield OperationResponse.success(OP_PAUSE, "Rollout paused successfully");
            }
            case STOP -> {
                if (request.rolloutId() == null) {
                    yield OperationResponse.failure(OP_STOP, "rolloutId is required for STOP operation");
                }
                api.stop(request.rolloutId());
                yield OperationResponse.success(OP_STOP, "Rollout stopped successfully");
            }
            case RESUME -> {
                if (request.rolloutId() == null) {
                    yield OperationResponse.failure(OP_RESUME, "rolloutId is required for RESUME operation");
                }
                api.resume(request.rolloutId());
                yield OperationResponse.success(OP_RESUME, "Rollout resumed successfully");
            }
            case APPROVE -> {
                if (request.rolloutId() == null) {
                    yield OperationResponse.failure(OP_APPROVE, "rolloutId is required for APPROVE operation");
                }
                api.approve(request.rolloutId(), request.remark());
                yield OperationResponse.success(OP_APPROVE, "Rollout approved successfully");
            }
            case DENY -> {
                if (request.rolloutId() == null) {
                    yield OperationResponse.failure(OP_DENY, "rolloutId is required for DENY operation");
                }
                api.deny(request.rolloutId(), request.remark());
                yield OperationResponse.success(OP_DENY, "Rollout denied successfully");
            }
            case RETRY -> {
                if (request.rolloutId() == null) {
                    yield OperationResponse.failure(OP_RETRY, "rolloutId is required for RETRY operation");
                }
                final ResponseEntity<MgmtRolloutResponseBody> response = api.retryRollout(request.rolloutId());
                yield OperationResponse.success(OP_RETRY, "Rollout retry created successfully", response.getBody());
            }
            case TRIGGER_NEXT_GROUP -> {
                if (request.rolloutId() == null) {
                    yield OperationResponse.failure(OP_TRIGGER_NEXT_GROUP, "rolloutId is required for TRIGGER_NEXT_GROUP operation");
                }
                api.triggerNextGroup(request.rolloutId());
                yield OperationResponse.success(OP_TRIGGER_NEXT_GROUP, "Next rollout group triggered successfully");
            }
        };
    }

    @McpTool(name = "manage_distribution_set",
             description = "Create, update, or delete distribution sets (software packages). " +
                           "Operations: CREATE (new distribution set with name, version, type), " +
                           "UPDATE (modify existing distribution set), DELETE (remove distribution set)")
    public OperationResponse<Object> manageDistributionSet(final ManageDistributionSetRequest request) {
        validateOperation(request.operation(), "distributionSets");
        log.debug("Managing distribution set: operation={}, distributionSetId={}", request.operation(), request.distributionSetId());

        final MgmtDistributionSetRestApi api = hawkbitClient.mgmtService(MgmtDistributionSetRestApi.class, dummyTenant);

        return switch (request.operation()) {
            case CREATE -> {
                if (request.createBody() == null) {
                    yield OperationResponse.failure(OP_CREATE, "createBody is required for CREATE operation");
                }
                final ResponseEntity<List<MgmtDistributionSet>> response = api.createDistributionSets(List.of(request.createBody()));
                final List<MgmtDistributionSet> created = response.getBody();
                yield OperationResponse.success(OP_CREATE, "Distribution set created successfully",
                        created != null && !created.isEmpty() ? created.get(0) : null);
            }
            case UPDATE -> {
                if (request.distributionSetId() == null) {
                    yield OperationResponse.failure(OP_UPDATE, "distributionSetId is required for UPDATE operation");
                }
                if (request.updateBody() == null) {
                    yield OperationResponse.failure(OP_UPDATE, "updateBody is required for UPDATE operation");
                }
                final ResponseEntity<MgmtDistributionSet> response = api.updateDistributionSet(request.distributionSetId(), request.updateBody());
                yield OperationResponse.success(OP_UPDATE, "Distribution set updated successfully", response.getBody());
            }
            case DELETE -> {
                if (request.distributionSetId() == null) {
                    yield OperationResponse.failure(OP_DELETE, "distributionSetId is required for DELETE operation");
                }
                api.deleteDistributionSet(request.distributionSetId());
                yield OperationResponse.success(OP_DELETE, "Distribution set deleted successfully");
            }
        };
    }

    @McpTool(name = "manage_action",
             description = "Delete deployment actions. Actions are created indirectly via distribution set assignment. " +
                           "Operations: DELETE (single action by ID), DELETE_BATCH (multiple actions by RSQL filter or list of IDs)")
    public OperationResponse<Object> manageAction(final ManageActionRequest request) {
        validateActionOperation(request.operation());
        log.debug("Managing action: operation={}, actionId={}", request.operation(), request.actionId());

        final MgmtActionRestApi api = hawkbitClient.mgmtService(MgmtActionRestApi.class, dummyTenant);

        return switch (request.operation()) {
            case DELETE -> {
                if (request.actionId() == null) {
                    yield OperationResponse.failure(OP_DELETE, "actionId is required for DELETE operation");
                }
                api.deleteAction(request.actionId());
                yield OperationResponse.success(OP_DELETE, "Action deleted successfully");
            }
            case DELETE_BATCH -> {
                if ((request.actionIds() == null || request.actionIds().isEmpty()) &&
                    (request.rsql() == null || request.rsql().isBlank())) {
                    yield OperationResponse.failure(OP_DELETE_BATCH, "Either actionIds or rsql is required for DELETE_BATCH operation");
                }
                api.deleteActions(request.rsql(), request.actionIds());
                yield OperationResponse.success(OP_DELETE_BATCH, "Actions deleted successfully");
            }
        };
    }

    @McpTool(name = "manage_software_module",
             description = "Create, update, or delete software modules. " +
                           "Operations: CREATE (new software module with name, version, type), " +
                           "UPDATE (modify existing software module), DELETE (remove software module)")
    public OperationResponse<Object> manageSoftwareModule(final ManageSoftwareModuleRequest request) {
        validateOperation(request.operation(), "softwareModules");
        log.debug("Managing software module: operation={}, softwareModuleId={}", request.operation(), request.softwareModuleId());

        final MgmtSoftwareModuleRestApi api = hawkbitClient.mgmtService(MgmtSoftwareModuleRestApi.class, dummyTenant);

        return switch (request.operation()) {
            case CREATE -> {
                if (request.createBody() == null) {
                    yield OperationResponse.failure(OP_CREATE, "createBody is required for CREATE operation");
                }
                final ResponseEntity<List<MgmtSoftwareModule>> response = api.createSoftwareModules(List.of(request.createBody()));
                final List<MgmtSoftwareModule> created = response.getBody();
                yield OperationResponse.success(OP_CREATE, "Software module created successfully",
                        created != null && !created.isEmpty() ? created.get(0) : null);
            }
            case UPDATE -> {
                if (request.softwareModuleId() == null) {
                    yield OperationResponse.failure(OP_UPDATE, "softwareModuleId is required for UPDATE operation");
                }
                if (request.updateBody() == null) {
                    yield OperationResponse.failure(OP_UPDATE, "updateBody is required for UPDATE operation");
                }
                final ResponseEntity<MgmtSoftwareModule> response = api.updateSoftwareModule(request.softwareModuleId(), request.updateBody());
                yield OperationResponse.success(OP_UPDATE, "Software module updated successfully", response.getBody());
            }
            case DELETE -> {
                if (request.softwareModuleId() == null) {
                    yield OperationResponse.failure(OP_DELETE, "softwareModuleId is required for DELETE operation");
                }
                api.deleteSoftwareModule(request.softwareModuleId());
                yield OperationResponse.success(OP_DELETE, "Software module deleted successfully");
            }
        };
    }

    @McpTool(name = "manage_target_filter",
             description = "Create, update, or delete target filter queries. " +
                           "Operations: CREATE (new target filter with name and RSQL query), " +
                           "UPDATE (modify existing target filter), DELETE (remove target filter)")
    public OperationResponse<Object> manageTargetFilter(final ManageTargetFilterRequest request) {
        validateOperation(request.operation(), "targetFilters");
        log.debug("Managing target filter: operation={}, filterId={}", request.operation(), request.filterId());

        final MgmtTargetFilterQueryRestApi api = hawkbitClient.mgmtService(MgmtTargetFilterQueryRestApi.class, dummyTenant);

        return switch (request.operation()) {
            case CREATE -> {
                if (request.body() == null) {
                    yield OperationResponse.failure(OP_CREATE, "Request body is required for CREATE operation");
                }
                final ResponseEntity<MgmtTargetFilterQuery> response = api.createFilter(request.body());
                yield OperationResponse.success(OP_CREATE, "Target filter created successfully", response.getBody());
            }
            case UPDATE -> {
                if (request.filterId() == null) {
                    yield OperationResponse.failure(OP_UPDATE, "filterId is required for UPDATE operation");
                }
                if (request.body() == null) {
                    yield OperationResponse.failure(OP_UPDATE, "Request body is required for UPDATE operation");
                }
                final ResponseEntity<MgmtTargetFilterQuery> response = api.updateFilter(request.filterId(), request.body());
                yield OperationResponse.success(OP_UPDATE, "Target filter updated successfully", response.getBody());
            }
            case DELETE -> {
                if (request.filterId() == null) {
                    yield OperationResponse.failure(OP_DELETE, "filterId is required for DELETE operation");
                }
                api.deleteFilter(request.filterId());
                yield OperationResponse.success(OP_DELETE, "Target filter deleted successfully");
            }
        };
    }


    private void validateOperation(final Operation operation, final String entity) {
        final String opName = operation.name().toLowerCase();
        if (!isOperationEnabled(opName, entity)) {
            throw new IllegalArgumentException(
                    "Operation " + operation + " is not enabled for " + entity +
                    ". Check hawkbit.mcp.operations configuration.");
        }
    }

    private void validateRolloutOperation(final RolloutOperation operation) {
        final String opName = operation.name().toLowerCase().replace("_", "-");
        final HawkBitMcpProperties.RolloutConfig config = properties.getOperations().getRollouts();
        final Boolean entitySetting = config.getOperationEnabled(opName);

        // For standard CRUD ops, check global fallback
        if (entitySetting == null) {
            if (!properties.getOperations().isGlobalOperationEnabled(opName)) {
                throw new IllegalArgumentException(
                        "Operation " + operation + " is not enabled for rollouts. " +
                        "Check hawkbit.mcp.operations configuration.");
            }
            return;
        }

        if (!entitySetting) {
            throw new IllegalArgumentException(
                    "Operation " + operation + " is not enabled for rollouts. " +
                    "Check hawkbit.mcp.operations configuration.");
        }
    }

    private void validateActionOperation(final ActionOperation operation) {
        final String opName = operation.name().toLowerCase().replace("_", "-");
        final HawkBitMcpProperties.ActionConfig config = properties.getOperations().getActions();
        final Boolean entitySetting = config.getOperationEnabled(opName);

        if (entitySetting == null) {
            if (opName.equals("delete") && !properties.getOperations().isGlobalOperationEnabled("delete")) {
                throw new IllegalArgumentException(
                        "Operation " + operation + " is not enabled for actions. " +
                        "Check hawkbit.mcp.operations configuration.");
            }
            return;
        }

        if (!entitySetting) {
            throw new IllegalArgumentException(
                    "Operation " + operation + " is not enabled for actions. " +
                    "Check hawkbit.mcp.operations configuration.");
        }
    }

    private boolean isOperationEnabled(final String operation, final String entity) {
        final HawkBitMcpProperties.Operations ops = properties.getOperations();
        final HawkBitMcpProperties.EntityConfig entityConfig = getEntityConfig(entity);

        final Boolean entitySetting = entityConfig != null ? entityConfig.getOperationEnabled(operation) : null;
        if (entitySetting != null) {
            return entitySetting;
        }

        return ops.isGlobalOperationEnabled(operation);
    }

    private HawkBitMcpProperties.EntityConfig getEntityConfig(final String entity) {
        final HawkBitMcpProperties.Operations ops = properties.getOperations();
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
