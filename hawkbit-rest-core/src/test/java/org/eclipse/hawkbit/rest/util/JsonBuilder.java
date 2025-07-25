/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.util;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditionBuilder;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.CollectionUtils;

/**
 * Builder class for building certain json strings.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class JsonBuilder {

    public static String ids(final Collection<Long> ids) throws JSONException {
        final JSONArray list = new JSONArray();
        for (final Long smID : ids) {
            list.put(new JSONObject().put("id", smID));
        }
        return list.toString();
    }

    public static <T> String toArray(final Collection<T> ids) {
        final JSONArray list = new JSONArray();
        for (final T smID : ids) {
            list.put(smID);
        }
        return list.toString();
    }

    public static String tags(final List<Tag> tags) throws JSONException {
        final StringBuilder builder = new StringBuilder();

        builder.append("[");
        int i = 0;
        for (final Tag tag : tags) {
            createTagLine(builder, tag);

            if (++i < tags.size()) {
                builder.append(",");
            }
        }
        builder.append("]");

        return builder.toString();
    }

    public static String tag(final Tag tag) throws JSONException {
        final StringBuilder builder = new StringBuilder();
        createTagLine(builder, tag);
        return builder.toString();
    }

    public static String softwareModules(final List<SoftwareModuleManagement.Create> modules) throws JSONException {
        final StringBuilder builder = new StringBuilder();

        builder.append("[");
        int i = 0;
        for (final SoftwareModuleManagement.Create module : modules) {
            builder.append(new JSONObject().put("name", module.getName()).put("description", module.getDescription())
                    .put("type", module.getType().getKey()).put("id", Long.MAX_VALUE).put("vendor", module.getVendor())
                    .put("version", module.getVersion()).put("createdAt", "0").put("updatedAt", "0")
                    .put("createdBy", "fghdfkjghdfkjh").put("updatedBy", "fghdfkjghdfkjh")
                    .put("encrypted", module.isEncrypted()).toString());

            if (++i < modules.size()) {
                builder.append(",");
            }
        }
        builder.append("]");

        return builder.toString();
    }

    public static String softwareModuleTypes(final List<SoftwareModuleTypeManagement.Create> types) throws JSONException {
        final StringBuilder builder = new StringBuilder();

        builder.append("[");
        int i = 0;
        for (final SoftwareModuleTypeManagement.Create module : types) {
            builder.append(new JSONObject().put("name", module.getName()).put("description", module.getDescription())
                    .put("colour", module.getColour()).put("id", Long.MAX_VALUE).put("key", module.getKey())
                    .put("maxAssignments", module.getMaxAssignments()).put("createdAt", "0").put("updatedAt", "0")
                    .put("createdBy", "fghdfkjghdfkjh").put("updatedBy", "fghdfkjghdfkjh").toString());

            if (++i < types.size()) {
                builder.append(",");
            }
        }
        builder.append("]");

        return builder.toString();
    }

    public static String softwareModuleTypeCreates(final List<SoftwareModuleTypeManagement.Create> creates) throws JSONException {
        final StringBuilder builder = new StringBuilder();

        builder.append("[");
        int i = 0;
        for (final SoftwareModuleTypeManagement.Create module : creates) {
            builder.append(new JSONObject().put("name", module.getName()).put("description", module.getDescription())
                    .put("colour", module.getColour()).put("id", Long.MAX_VALUE).put("key", module.getKey())
                    .put("maxAssignments", module.getMaxAssignments()).put("createdAt", "0").put("updatedAt", "0")
                    .put("createdBy", "fghdfkjghdfkjh").put("updatedBy", "fghdfkjghdfkjh").toString());

            if (++i < creates.size()) {
                builder.append(",");
            }
        }
        builder.append("]");

        return builder.toString();
    }

    public static String distributionSetTypes(final List<DistributionSetTypeManagement.Create> types) throws JSONException {
        final JSONArray result = new JSONArray();

        for (final DistributionSetTypeManagement.Create type : types) {
            final JSONArray osmTypes = new JSONArray();
            type.getOptionalModuleTypes().forEach(smt -> {
                try {
                    osmTypes.put(new JSONObject().put("id", smt.getId()));
                } catch (final JSONException e1) {
                    log.error("JSONException (skip)", e1);
                }
            });

            final JSONArray msmTypes = new JSONArray();
            type.getMandatoryModuleTypes().forEach(smt -> {
                try {
                    msmTypes.put(new JSONObject().put("id", smt.getId()));
                } catch (final JSONException e) {
                    log.error("JSONException (skip)", e);
                }
            });

            result.put(new JSONObject().put("name", type.getName()).put("description", type.getDescription())
                    .put("colour", type.getColour()).put("id", Long.MAX_VALUE).put("key", type.getKey())
                    .put("createdAt", "0").put("updatedAt", "0").put("createdBy", "fghdfkjghdfkjh")
                    .put("optionalmodules", osmTypes).put("mandatorymodules", msmTypes)
                    .put("updatedBy", "fghdfkjghdfkjh"));

        }

        return result.toString();
    }

    public static String distributionSets(final List<DistributionSetManagement.Create> sets) {
        final JSONArray setsJson = new JSONArray();

        sets.forEach(set -> {
            try {
                setsJson.put(distributionSet(set));
            } catch (final JSONException e) {
                log.error("JSONException (skip)", e);
            }
        });

        return setsJson.toString();
    }

    public static JSONObject distributionSet(final DistributionSetManagement.Create set) throws JSONException {
        final List<JSONObject> modules = set.getModules().stream().map(module -> {
            try {
                return new JSONObject().put("id", module.getId());
            } catch (final JSONException e) {
                log.error("JSONException (skip)", e);
                return null;
            }
        }).toList();

        return new JSONObject().put("name", set.getName()).put("description", set.getDescription())
                .put("type", set.getType() == null ? null : set.getType().getKey()).put("id", Long.MAX_VALUE)
                .put("version", set.getVersion()).put("createdAt", "0").put("updatedAt", "0")
                .put("createdBy", "fghdfkjghdfkjh").put("updatedBy", "fghdfkjghdfkjh")
                .put("requiredMigrationStep", set.getRequiredMigrationStep()).put("modules", new JSONArray(modules));
    }

    public static String targets(final List<Target> targets, final boolean withToken) throws JSONException {
        final StringBuilder builder = new StringBuilder();

        builder.append("[");
        int i = 0;
        for (final Target target : targets) {
            final String address = target.getAddress() != null ? target.getAddress().toString() : null;
            final String targetType = target.getTargetType() != null ? target.getTargetType().getId().toString() : null;
            final String token = withToken ? target.getSecurityToken() : null;

            builder.append(new JSONObject().put("controllerId", target.getControllerId())
                    .put("description", target.getDescription()).put("name", target.getName()).put("createdAt", "0")
                    .put("updatedAt", "0").put("createdBy", "systemtest").put("updatedBy", "systemtest")
                    .put("address", address).put("securityToken", token).put("targetType", targetType).toString());

            if (++i < targets.size()) {
                builder.append(",");
            }
        }

        builder.append("]");

        return builder.toString();
    }

    public static String targets(final List<Target> targets, final boolean withToken, final long targetTypeId) throws JSONException {
        final StringBuilder builder = new StringBuilder();

        builder.append("[");
        int i = 0;
        for (final Target target : targets) {
            final String address = target.getAddress() != null ? target.getAddress().toString() : null;
            final String token = withToken ? target.getSecurityToken() : null;

            builder.append(new JSONObject().put("controllerId", target.getControllerId())
                    .put("description", target.getDescription()).put("name", target.getName()).put("createdAt", "0")
                    .put("updatedAt", "0").put("createdBy", "fghdfkjghdfkjh").put("updatedBy", "fghdfkjghdfkjh")
                    .put("address", address).put("securityToken", token).put("targetType", targetTypeId).toString());

            if (++i < targets.size()) {
                builder.append(",");
            }
        }

        builder.append("]");

        return builder.toString();
    }

    public static String targetTypes(final List<TargetType> types) throws JSONException {
        final JSONArray result = new JSONArray();

        for (final TargetType type : types) {

            final JSONArray dsTypes = new JSONArray();
            type.getCompatibleDistributionSetTypes().forEach(dsType -> {
                try {
                    dsTypes.put(new JSONObject().put("id", dsType.getId()));
                } catch (final JSONException e1) {
                    log.error("JSONException (skip)", e1);
                }
            });

            result.put(new JSONObject().put("name", type.getName()).put("description", type.getDescription())
                    .put("id", Long.MAX_VALUE).put("colour", type.getColour()).put("createdAt", "0")
                    .put("updatedAt", "0").put("createdBy", "fghdfkjghdfkjh").put("updatedBy", "fghdfkjghdfkjh")
                    .put("distributionsets", dsTypes));

        }

        return result.toString();
    }

    public static String rollout(final String name, final String description, final int groupSize,
            final long distributionSetId, final String targetFilterQuery, final RolloutGroupConditions conditions) {
        return rollout(name, description, groupSize, distributionSetId, targetFilterQuery, conditions, null, null, null,
                null, null, null);
    }

    public static String rolloutWithGroups(final String name, final String description, final Integer groupSize,
            final long distributionSetId, final String targetFilterQuery, final RolloutGroupConditions conditions,
            final List<RolloutGroup> groups) {
        return rolloutWithGroups(name, description, groupSize, distributionSetId, targetFilterQuery, conditions, groups,
                null, null, null);
    }

    public static String rolloutWithGroups(final String name, final String description, final Integer groupSize,
            final long distributionSetId, final String targetFilterQuery, final RolloutGroupConditions conditions,
            final List<RolloutGroup> groups, final String type, final Integer weight,
            final Boolean confirmationRequired) {
        final List<String> rolloutGroupsJson = groups.stream().map(JsonBuilder::rolloutGroup).toList();
        return rollout(
                name, description, groupSize, distributionSetId, targetFilterQuery, conditions,
                rolloutGroupsJson, type, weight, System.currentTimeMillis(), null, confirmationRequired);
    }

    public static String rollout(final String name, final String description, final Integer groupSize,
            final long distributionSetId, final String targetFilterQuery, final RolloutGroupConditions conditions,
            final List<String> groupJsonList, final String type, final Integer weight, final Long startAt, final Long forceTime,
            final Boolean confirmationRequired) {
        return rollout(name, description, groupSize, distributionSetId, targetFilterQuery, conditions, groupJsonList, type,
                weight, startAt, forceTime, confirmationRequired, false, null, 0);
    }

    public static String rollout(final String name, final String description, final Integer groupSize,
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

    public static String rolloutGroup(final RolloutGroup rolloutGroup) {
        final RolloutGroupConditions conditions = getConditions(rolloutGroup);
        return rolloutGroup(rolloutGroup.getName(), rolloutGroup.getDescription(), rolloutGroup.getTargetFilterQuery(),
                rolloutGroup.getTargetPercentage(), rolloutGroup.isConfirmationRequired(), conditions);

    }

    public static String rolloutGroup(final String name, final String description, final String targetFilterQuery,
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

    public static JSONObject configData(final Map<String, String> attributes) throws JSONException {
        return configData(attributes, null);
    }

    public static JSONObject configData(final Map<String, String> attributes, final String mode) throws JSONException {
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

    private static void createTagLine(final StringBuilder builder, final Tag tag) throws JSONException {
        builder.append(new JSONObject().put("name", tag.getName()).put("description", tag.getDescription())
                .put("colour", tag.getColour()).put("createdAt", "0").put("updatedAt", "0")
                .put("createdBy", "fghdfkjghdfkjh").put("updatedBy", "fghdfkjghdfkjh").toString());
    }

    private static RolloutGroupConditions getConditions(final RolloutGroup rolloutGroup) {
        return new RolloutGroupConditionBuilder()
                .errorCondition(rolloutGroup.getErrorCondition(), rolloutGroup.getErrorConditionExp())
                .errorAction(rolloutGroup.getErrorAction(), rolloutGroup.getErrorActionExp())
                .successAction(rolloutGroup.getSuccessAction(), rolloutGroup.getSuccessActionExp())
                .successCondition(rolloutGroup.getSuccessCondition(), rolloutGroup.getSuccessConditionExp()).build();
    }
}
