/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.RolloutManagement.GroupCreate;
import org.eclipse.hawkbit.repository.TargetManagement.Create;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.CollectionUtils;

/**
 * Builder class for building certain json strings.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
class JsonBuilder {

    static String targets(final List<Create> creates, final boolean withToken) throws JSONException {
        final StringBuilder builder = new StringBuilder();

        builder.append("[");
        int i = 0;
        for (final Create create : creates) {
            final String address = create.getAddress() != null ? create.getAddress() : null;
            final String targetType = create.getTargetType() != null ? create.getTargetType().getId().toString() : null;
            final String token = withToken ? create.getSecurityToken() : null;

            builder.append(new JSONObject().put("controllerId", create.getControllerId())
                    .put("description", create.getDescription()).put("name", create.getName()).put("createdAt", "0")
                    .put("updatedAt", "0").put("createdBy", "systemtest").put("updatedBy", "systemtest")
                    .put("address", address).put("securityToken", token).put("targetType", targetType).toString());

            if (++i < creates.size()) {
                builder.append(",");
            }
        }

        builder.append("]");

        return builder.toString();
    }

    static String targets(final List<Create> creates, final boolean withToken, final long targetTypeId) throws JSONException {
        final StringBuilder builder = new StringBuilder();

        builder.append("[");
        int i = 0;
        for (final Create create : creates) {
            final String address = create.getAddress() != null ? create.getAddress() : null;
            final String token = withToken ? create.getSecurityToken() : null;

            builder.append(new JSONObject().put("controllerId", create.getControllerId())
                    .put("description", create.getDescription()).put("name", create.getName()).put("createdAt", "0")
                    .put("updatedAt", "0").put("createdBy", "fghdfkjghdfkjh").put("updatedBy", "fghdfkjghdfkjh")
                    .put("address", address).put("securityToken", token).put("targetType", targetTypeId).toString());

            if (++i < creates.size()) {
                builder.append(",");
            }
        }

        builder.append("]");

        return builder.toString();
    }

    static String rollout(
            final String name, final String description, final int groupSize,
            final long distributionSetId, final String targetFilterQuery, final RolloutGroupConditions conditions) {
        return rollout(name, description, groupSize, distributionSetId, targetFilterQuery, conditions, null, null, null,
                null, null, null);
    }

    static String rolloutWithGroups(final String name, final String description, final Integer groupSize,
            final long distributionSetId, final String targetFilterQuery, final RolloutGroupConditions conditions,
            final List<GroupCreate> groups) {
        return rolloutWithGroups(name, description, groupSize, distributionSetId, targetFilterQuery, conditions, groups,
                null, null, null);
    }

    static String rolloutWithGroups(final String name, final String description, final Integer groupSize,
            final long distributionSetId, final String targetFilterQuery, final RolloutGroupConditions conditions,
            final List<GroupCreate> groups, final String type, final Integer weight,
            final Boolean confirmationRequired) {
        final List<String> rolloutGroupsJson = groups.stream().map(JsonBuilder::rolloutGroup).toList();
        return rollout(
                name, description, groupSize, distributionSetId, targetFilterQuery, conditions,
                rolloutGroupsJson, type, weight, System.currentTimeMillis(), null, confirmationRequired);
    }

    static String rollout(final String name, final String description, final Integer groupSize,
            final long distributionSetId, final String targetFilterQuery, final RolloutGroupConditions conditions,
            final List<String> groupJsonList, final String type, final Integer weight, final Long startAt, final Long forceTime,
            final Boolean confirmationRequired) {
        return rollout(name, description, groupSize, distributionSetId, targetFilterQuery, conditions, groupJsonList, type,
                weight, startAt, forceTime, confirmationRequired, false, null, 0);
    }

    static String rollout(final String name, final String description, final Integer groupSize,
            final long distributionSetId, final String targetFilterQuery, final RolloutGroupConditions conditions,
            final List<String> groupJsonList, final String type, final Integer weight, final Long startAt, final Long forceTime,
            final Boolean confirmationRequired, final boolean isDynamic, final String dynamicGroupSuffix, final int dynamicGroupTargetsCount) {
        final JSONObject json = new JSONObject();

        try {
            json.put("name", name);
            json.put("description", description);
            json.put("amountGroups", groupSize);
            json.put("distributionSetId", distributionSetId);
            json.put("targetFilterQuery", targetFilterQuery);

            if (type != null) {
                json.put("type", type);
            }

            if (weight != null) {
                json.put("weight", weight);
            }

            if (startAt != null) {
                json.put("startAt", startAt);
            }

            if (forceTime != null) {
                json.put("forcetime", forceTime);
            }

            if (confirmationRequired != null) {
                json.put("confirmationRequired", confirmationRequired);
            }

            if (conditions != null) {
                final JSONObject successCondition = new JSONObject();

                json.put("successCondition", successCondition);

                successCondition.put("condition", conditions.getSuccessCondition().toString());
                successCondition.put("expression", conditions.getSuccessConditionExp());

                final JSONObject successAction = new JSONObject();
                json.put("successAction", successAction);
                successAction.put("action", conditions.getSuccessAction().toString());
                successAction.put("expression", conditions.getSuccessActionExp());

                final JSONObject errorCondition = new JSONObject();
                json.put("errorCondition", errorCondition);
                errorCondition.put("condition", conditions.getErrorCondition().toString());
                errorCondition.put("expression", conditions.getErrorConditionExp());

                final JSONObject errorAction = new JSONObject();
                json.put("errorAction", errorAction);
                errorAction.put("action", conditions.getErrorAction().toString());
                errorAction.put("expression", conditions.getErrorActionExp());
            }

            if (isDynamic) {
                json.put("dynamic", isDynamic);

                final JSONObject dynamicGroupTemplate = new JSONObject();
                json.put("dynamicGroupTemplate", dynamicGroupTemplate);
                dynamicGroupTemplate.put("nameSuffix",
                        (dynamicGroupSuffix == null || dynamicGroupSuffix.isEmpty()) ? "-dynamic" : dynamicGroupSuffix);
                dynamicGroupTemplate.put("targetCount", dynamicGroupTargetsCount < 0 ? 1 : dynamicGroupTargetsCount);
            }

            if (!CollectionUtils.isEmpty(groupJsonList)) {
                final JSONArray jsonGroups = new JSONArray();
                for (final String groupJson : groupJsonList) {
                    jsonGroups.put(new JSONObject(groupJson));
                }
                json.put("groups", jsonGroups);
            }

        } catch (final JSONException e) {
            log.error("JSONException (skip)", e);
        }

        return json.toString();
    }

    static String rolloutGroup(final GroupCreate create) {
        return rolloutGroup(create, null);
    }

    static String rolloutGroup(final GroupCreate create, final RolloutGroup rolloutGroup) {
        final RolloutGroupConditions conditions = getConditions(rolloutGroup);
        return rolloutGroup(create.getName(), create.getDescription(), create.getTargetFilterQuery(),
                create.getTargetPercentage(), create.isConfirmationRequired(), conditions);

    }

    static String rolloutGroup(final String name, final String description, final String targetFilterQuery,
            final Float targetPercentage, final Boolean confirmationRequired,
            final RolloutGroupConditions rolloutGroupConditions) {
        final JSONObject jsonGroup = new JSONObject();
        try {
            jsonGroup.put("name", name);
            jsonGroup.put("description", description);
            jsonGroup.put("targetFilterQuery", targetFilterQuery);
            if (targetPercentage == null) {
                jsonGroup.put("targetPercentage", 100F);
            } else {
                jsonGroup.put("targetPercentage", targetPercentage);
            }

            if (confirmationRequired != null) {
                jsonGroup.put("confirmationRequired", confirmationRequired);
            }

            if (rolloutGroupConditions.getSuccessCondition() != null) {
                final JSONObject successCondition = new JSONObject();
                jsonGroup.put("successCondition", successCondition);
                successCondition.put("condition", rolloutGroupConditions.getSuccessCondition().toString());
                successCondition.put("expression", rolloutGroupConditions.getSuccessConditionExp());
            }
            if (rolloutGroupConditions.getSuccessAction() != null) {
                final JSONObject successAction = new JSONObject();
                jsonGroup.put("successAction", successAction);
                successAction.put("action", rolloutGroupConditions.getSuccessAction().toString());
                successAction.put("expression", rolloutGroupConditions.getSuccessActionExp());
            }
            if (rolloutGroupConditions.getErrorCondition() != null) {
                final JSONObject errorCondition = new JSONObject();
                jsonGroup.put("errorCondition", errorCondition);
                errorCondition.put("condition", rolloutGroupConditions.getErrorCondition().toString());
                errorCondition.put("expression", rolloutGroupConditions.getErrorConditionExp());
            }
            if (rolloutGroupConditions.getErrorAction() != null) {
                final JSONObject errorAction = new JSONObject();
                jsonGroup.put("errorAction", errorAction);
                errorAction.put("action", rolloutGroupConditions.getErrorAction().toString());
                errorAction.put("expression", rolloutGroupConditions.getErrorActionExp());
            }

        } catch (final JSONException e) {
            log.error("JSONException (skip)", e);
            fail("Cannot parse JSON for rollout group.");
        }

        return jsonGroup.toString();
    }

    static JSONObject configData(final Map<String, String> attributes) throws JSONException {
        return configData(attributes, null);
    }

    static JSONObject configData(final Map<String, String> attributes, final String mode) throws JSONException {
        final JSONObject data = new JSONObject();
        attributes.forEach((key, value) -> {
            try {
                data.put(key, value);
            } catch (final JSONException e) {
                log.error("JSONException (skip)", e);
            }
        });

        final JSONObject json = new JSONObject().put("data", data);
        if (mode != null) {
            json.put("mode", mode);
        }
        return json;
    }

    private static RolloutGroupConditions getConditions(final RolloutGroup rolloutGroup) {
        if (rolloutGroup == null) {
            return new RolloutGroupConditionBuilder().withDefaults().build();
        } else {
            return new RolloutGroupConditionBuilder()
                    .errorCondition(rolloutGroup.getErrorCondition(), rolloutGroup.getErrorConditionExp())
                    .errorAction(rolloutGroup.getErrorAction(), rolloutGroup.getErrorActionExp())
                    .successAction(rolloutGroup.getSuccessAction(), rolloutGroup.getSuccessActionExp())
                    .successCondition(rolloutGroup.getSuccessCondition(), rolloutGroup.getSuccessConditionExp()).build();
        }
    }
}