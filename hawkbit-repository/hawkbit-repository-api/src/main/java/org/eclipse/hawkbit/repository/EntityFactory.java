/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * central {@link BaseEntity} generation service. Objects are created but not
 * persisted.
 *
 */
public interface EntityFactory {

    /**
     * Generates an empty {@link Action} without persisting it.
     * 
     * @return {@link Action} object
     */
    Action generateAction();

    /**
     * Generates an {@link ActionStatus} object without persisting it.
     * 
     * @param action
     *            the {@link ActionStatus} belongs to.
     * @param status
     *            as reflected by this {@link ActionStatus}.
     * @param occurredAt
     *            time in {@link TimeUnit#MILLISECONDS} GMT when the status
     *            change happened.
     * 
     * @return {@link ActionStatus} object
     */
    ActionStatus generateActionStatus(@NotNull Action action, @NotNull Status status, Long occurredAt);

    /**
     * Generates an {@link ActionStatus} object without persisting it.
     * 
     * @param status
     *            as reflected by this {@link ActionStatus}.
     * @param occurredAt
     *            time in {@link TimeUnit#MILLISECONDS} GMT when the status
     *            change happened.
     * @param messages
     *            optional comments
     * 
     * @return {@link ActionStatus} object
     */
    ActionStatus generateActionStatus(@NotNull Status status, Long occurredAt, final Collection<String> messages);

    /**
     * Generates an {@link ActionStatus} object without persisting it.
     * 
     * @param status
     *            as reflected by this {@link ActionStatus}.
     * @param occurredAt
     *            time in {@link TimeUnit#MILLISECONDS} GMT when the status
     *            change happened.
     * @param message
     *            optional comment
     * 
     * @return {@link ActionStatus} object
     */
    ActionStatus generateActionStatus(@NotNull Status status, Long occurredAt, final String message);

    /**
     * Generates an empty {@link Artifact} without persisting it.
     * 
     * @return {@link Artifact} object
     */
    Artifact generateArtifact();

    /**
     * Generates an {@link DistributionSet} without persisting it.
     * 
     * @param name
     *            {@link DistributionSet#getName()}
     * @param version
     *            {@link DistributionSet#getVersion()}
     * @param description
     *            {@link DistributionSet#getDescription()}
     * @param type
     *            {@link DistributionSet#getType()}
     * @param moduleList
     *            {@link DistributionSet#getModules()}
     * @param requiredMigrationStep
     *            {@link DistributionSet#isRequiredMigrationStep()}
     * 
     * @return {@link DistributionSet} object
     */
    DistributionSet generateDistributionSet(@NotNull String name, @NotNull String version, String description,
            DistributionSetType type, Collection<SoftwareModule> moduleList, boolean requiredMigrationStep);

    /**
     * Generates an empty {@link DistributionSetMetadata} element without
     * persisting it.
     * 
     * @return {@link DistributionSetMetadata} object
     */
    DistributionSetMetadata generateDistributionSetMetadata();

    /**
     * Generates an {@link DistributionSetMetadata} element without persisting
     * it.
     * 
     * @param distributionSet
     *            {@link DistributionSetMetadata#getDistributionSet()}
     * @param key
     *            {@link DistributionSetMetadata#getKey()}
     * @param value
     *            {@link DistributionSetMetadata#getValue()}
     * 
     * @return {@link DistributionSetMetadata} object
     */
    DistributionSetMetadata generateDistributionSetMetadata(@NotNull DistributionSet distributionSet,
            @NotNull String key, String value);

    /**
     * Generates an empty {@link DistributionSetTag} without persisting it.
     * 
     * @return {@link DistributionSetTag} object
     */
    DistributionSetTag generateDistributionSetTag();

    /**
     * Generates a {@link DistributionSetTag} without persisting it.
     * 
     * @param name
     *            of the tag
     * @return {@link DistributionSetTag} object
     */
    DistributionSetTag generateDistributionSetTag(@NotNull String name);

    /**
     * Generates a {@link DistributionSetTag} without persisting it.
     * 
     * @param name
     *            of the tag
     * @param description
     *            of the tag
     * @param colour
     *            of the tag
     * @return {@link DistributionSetTag} object
     */
    DistributionSetTag generateDistributionSetTag(@NotNull String name, String description, String colour);

    /**
     * Generates an empty {@link DistributionSetType} without persisting it.
     * 
     * @return {@link DistributionSetType} object
     */
    DistributionSetType generateDistributionSetType();

    /**
     * Generates a {@link DistributionSetType} without persisting it.
     * 
     * @param key
     *            {@link DistributionSetType#getKey()}
     * @param name
     *            {@link DistributionSetType#getName()}
     * @param description
     *            {@link DistributionSetType#getDescription()}
     * 
     * @return {@link DistributionSetType} object
     */
    DistributionSetType generateDistributionSetType(@NotNull String key, @NotNull String name, String description);

    /**
     * Generates a {@link DistributionSetType} without persisting it.
     * 
     * @param key
     *            {@link DistributionSetType#getKey()}
     * @param name
     *            {@link DistributionSetType#getName()}
     * @param description
     *            {@link DistributionSetType#getDescription()}
     * @param mandatory
     *            list of {@link SoftwareModuleType}s
     * @param optional
     *            list of {@link SoftwareModuleType}s
     * 
     * @return {@link DistributionSetType} object
     */
    DistributionSetType generateDistributionSetType(String key, String name, String description,
            Collection<SoftwareModuleType> mandatory, Collection<SoftwareModuleType> optional);

    /**
     * Generates an empty {@link Rollout} without persisting it.
     * 
     * @return {@link Rollout} object
     */
    Rollout generateRollout(String name, String description, DistributionSet set, String targetFilterQuery,
            ActionType actionType, long forcedTime);

    Rollout generateRollout(String name, String description, DistributionSet set, String targetFilterQuery);

    /**
     * Generates an empty {@link RolloutGroup} without persisting it.
     * 
     * @return {@link RolloutGroup} object
     */
    RolloutGroup generateRolloutGroup();

    /**
     * Generates a {@link SoftwareModule} without persisting it.
     *
     * @param type
     *            of the {@link SoftwareModule}
     * @param name
     *            abstract name of the {@link SoftwareModule}
     * @param version
     *            of the {@link SoftwareModule}
     * @param description
     *            of the {@link SoftwareModule}
     * @param vendor
     *            of the {@link SoftwareModule}
     * 
     * @return {@link SoftwareModule} object
     */
    SoftwareModule generateSoftwareModule(@NotNull SoftwareModuleType type, @NotNull String name,
            @NotNull String version, String description, String vendor);

    /**
     * Generates an empty {@link SoftwareModuleMetadata} pair without persisting
     * it.
     * 
     * @return {@link SoftwareModuleMetadata} object
     */
    SoftwareModuleMetadata generateSoftwareModuleMetadata();

    /**
     * Generates a {@link SoftwareModuleMetadata} pair without persisting it.
     * 
     * @param softwareModule
     *            {@link SoftwareModuleMetadata#getSoftwareModule()}
     * @param key
     *            {@link SoftwareModuleMetadata#getKey()}
     * @param value
     *            {@link SoftwareModuleMetadata#getValue()}
     * 
     * @return {@link SoftwareModuleMetadata} object
     */
    SoftwareModuleMetadata generateSoftwareModuleMetadata(@NotNull SoftwareModule softwareModule, @NotNull String key,
            String value);

    /**
     * Generates a {@link SoftwareModuleType} without persisting it.
     * 
     * @param key
     *            {@link SoftwareModuleType#getKey()}
     * @param name
     *            {@link SoftwareModuleType#getName()}
     * @param description
     *            {@link SoftwareModuleType#getDescription()}
     * @param colour
     *            {@link SoftwareModuleType#getColour()}
     * @param maxAssignments
     *            {@link SoftwareModuleType#getMaxAssignments()}
     * 
     * @return {@link SoftwareModuleType} object
     */
    SoftwareModuleType generateSoftwareModuleType(@NotNull String key, @NotNull String name, String description,
            String colour, int maxAssignments);

    /**
     * Generates an empty {@link Target} without persisting it.
     * {@link Target#getSecurityToken()} is generated. {@link Target#getName()}
     * is equal to {@link Target#getControllerId()}.
     * 
     * @param controllerID
     *            of the {@link Target}
     * 
     * @return {@link Target} object
     */
    Target generateTarget(@NotEmpty String controllerID);

    /**
     * Generates an empty {@link Target} without persisting it.
     * 
     * @param controllerID
     *            of the {@link Target}
     * @param name
     *            of the {@link Target} (optional, i.e. {@link Target#getName()}
     *            is equal to {@link Target#getControllerId()}.)
     * @param description
     *            of the {@link Target} (optional)
     * @param securityToken
     *            of the {@link Target} for authentication if enabled on tenant.
     *            Generates one if empty or <code>null</code>.
     * 
     * @return {@link Target} object
     */
    Target generateTarget(@NotEmpty String controllerID, String name, String description, String securityToken);

    /**
     * Generates an empty {@link TargetFilterQuery} without persisting it.
     * 
     * @return {@link TargetFilterQuery} object
     */
    TargetFilterQuery generateTargetFilterQuery();

    /**
     * Generates an {@link TargetFilterQuery} without persisting it.
     *
     * @param name
     *            name for the filter
     * @param query
     *            query of the filter
     * @return {@link TargetFilterQuery} object
     */
    TargetFilterQuery generateTargetFilterQuery(String name, String query);

    /**
     * Generates an {@link TargetFilterQuery} without persisting it.
     *
     * @param name
     *            name for the filter
     * @param query
     *            query of the filter
     * @param autoAssignDS
     *            auto assign distribution set
     * @return {@link TargetFilterQuery} object
     */
    TargetFilterQuery generateTargetFilterQuery(String name, String query, DistributionSet autoAssignDS);

    /**
     * Generates an empty {@link TargetTag} without persisting it.
     * 
     * @return {@link TargetTag} object
     */
    TargetTag generateTargetTag();

    /**
     * Generates a {@link TargetTag} without persisting it.
     * 
     * @param name
     *            of the tag
     * @return {@link TargetTag} object
     */
    TargetTag generateTargetTag(@NotNull String name);

    /**
     * Generates a {@link TargetTag} without persisting it.
     * 
     * @param name
     *            of the tag
     * @param description
     *            of the tag
     * @param colour
     *            of the tag
     * @return {@link TargetTag} object
     */
    TargetTag generateTargetTag(@NotNull String name, String description, String colour);

}
