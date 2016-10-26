/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Artifact;
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
import org.springframework.validation.annotation.Validated;

/**
 * JPA Implementation of {@link EntityFactory}.
 *
 */
@Validated
public class JpaEntityFactory implements EntityFactory {

    @Override
    public DistributionSetType generateDistributionSetType() {
        return new JpaDistributionSetType();
    }

    @Override
    public DistributionSetMetadata generateDistributionSetMetadata() {
        return new JpaDistributionSetMetadata();
    }

    @Override
    public DistributionSetMetadata generateDistributionSetMetadata(final DistributionSet distributionSet,
            final String key, final String value) {
        return new JpaDistributionSetMetadata(key, distributionSet, value);
    }

    @Override
    public DistributionSetType generateDistributionSetType(final String key, final String name,
            final String description) {
        return new JpaDistributionSetType(key, name, description);
    }

    @Override
    public DistributionSet generateDistributionSet(final String name, final String version, final String description,
            final DistributionSetType type, final Collection<SoftwareModule> moduleList,
            final boolean requiredMigrationstep) {
        return new JpaDistributionSet(name, version, description, type, moduleList, requiredMigrationstep);
    }

    @Override
    public Target generateTarget(final String controllerId, final String name, final String description,
            final String securityToken) {
        JpaTarget target;

        if (StringUtils.isEmpty(securityToken)) {
            target = new JpaTarget(controllerId);
        } else {
            target = new JpaTarget(controllerId, securityToken);
        }

        if (!StringUtils.isEmpty(name)) {
            target.setName(name);
        }

        target.setDescription(description);

        return target;
    }

    @Override
    public Target generateTarget(final String controllerID) {
        return generateTarget(controllerID, null, null, null);
    }

    @Override
    public TargetTag generateTargetTag() {
        return new JpaTargetTag();
    }

    @Override
    public DistributionSetTag generateDistributionSetTag() {
        return new JpaDistributionSetTag();
    }

    @Override
    public TargetTag generateTargetTag(final String name, final String description, final String colour) {
        return new JpaTargetTag(name, description, colour);
    }

    @Override
    public DistributionSetTag generateDistributionSetTag(final String name, final String description,
            final String colour) {
        return new JpaDistributionSetTag(name, description, colour);
    }

    @Override
    public TargetTag generateTargetTag(final String name) {
        return new JpaTargetTag(name);
    }

    @Override
    public DistributionSetTag generateDistributionSetTag(final String name) {
        return new JpaDistributionSetTag(name);
    }

    @Override
    public TargetFilterQuery generateTargetFilterQuery() {
        return new JpaTargetFilterQuery();
    }

    @Override
    public TargetFilterQuery generateTargetFilterQuery(final String name, final String query) {
        return new JpaTargetFilterQuery(name, query);
    }

    @Override
    public TargetFilterQuery generateTargetFilterQuery(final String name, final String query,
            final DistributionSet autoAssignDS) {
        return new JpaTargetFilterQuery(name, query, (JpaDistributionSet) autoAssignDS);
    }

    @Override
    public SoftwareModule generateSoftwareModule(final SoftwareModuleType type, final String name, final String version,
            final String description, final String vendor) {

        return new JpaSoftwareModule(type, name, version, description, vendor);
    }

    @Override
    public SoftwareModuleMetadata generateSoftwareModuleMetadata() {
        return new JpaSoftwareModuleMetadata();
    }

    @Override
    public SoftwareModuleMetadata generateSoftwareModuleMetadata(final SoftwareModule softwareModule, final String key,
            final String value) {
        return new JpaSoftwareModuleMetadata(key, softwareModule, value);
    }

    @Override
    public SoftwareModuleType generateSoftwareModuleType(final String key, final String name, final String description,
            final String colour, final int maxAssignments) {
        return new JpaSoftwareModuleType(key, name, description, maxAssignments, colour);
    }

    @Override
    public RolloutGroup generateRolloutGroup() {
        return new JpaRolloutGroup();
    }

    @Override
    public Action generateAction() {
        return new JpaAction();
    }

    @Override
    public ActionStatus generateActionStatus(final Status status, final Long occurredAt, final String message) {
        return new JpaActionStatus(status, occurredAt, message);
    }

    @Override
    public ActionStatus generateActionStatus(final Status status, final Long occurredAt,
            final Collection<String> messages) {

        final JpaActionStatus result = new JpaActionStatus(status, occurredAt, null);
        messages.forEach(result::addMessage);

        return result;
    }

    @Override
    public ActionStatus generateActionStatus(final Action action, final Status status, final Long occurredAt) {
        return new JpaActionStatus(action, status, occurredAt);
    }

    @Override
    public Artifact generateArtifact() {
        return new JpaArtifact();
    }

    @Override
    public DistributionSetType generateDistributionSetType(final String key, final String name,
            final String description, final Collection<SoftwareModuleType> mandatory,
            final Collection<SoftwareModuleType> optional) {
        final JpaDistributionSetType result = (JpaDistributionSetType) generateDistributionSetType(key, name,
                description);
        mandatory.forEach(result::addMandatoryModuleType);
        optional.forEach(result::addOptionalModuleType);

        return result;
    }

    @Override
    public Rollout generateRollout(final String name, final String description, final DistributionSet set,
            final String filterQuery, final ActionType actionType, final long forcedTime) {

        final JpaRollout rollout = new JpaRollout();

        rollout.setName(name);
        rollout.setDescription(description);
        rollout.setDistributionSet(set);
        rollout.setTargetFilterQuery(filterQuery);
        rollout.setActionType(actionType);
        rollout.setForcedTime(forcedTime);

        return rollout;
    }

    @Override
    public Rollout generateRollout(final String name, final String description, final DistributionSet set,
            final String filterQuery) {

        final JpaRollout rollout = new JpaRollout();

        rollout.setName(name);
        rollout.setDescription(description);
        rollout.setDistributionSet(set);
        rollout.setTargetFilterQuery(filterQuery);

        return rollout;
    }

}
