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
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaLocalArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
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
    public DistributionSet generateDistributionSet() {
        return new JpaDistributionSet();
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
            final DistributionSetType type, final Collection<SoftwareModule> moduleList) {
        return new JpaDistributionSet(name, version, description, type, moduleList);
    }

    @Override
    public Target generateTarget(final String controllerId) {
        return new JpaTarget(controllerId);
    }

    @Override
    public Target generateTarget(final String controllerId, final String securityToken) {
        if (StringUtils.isEmpty(securityToken)) {
            return new JpaTarget(controllerId);
        }
        return new JpaTarget(controllerId, securityToken);
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
    public SoftwareModuleType generateSoftwareModuleType() {
        return new JpaSoftwareModuleType();
    }

    @Override
    public SoftwareModule generateSoftwareModule() {
        return new JpaSoftwareModule();
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
            final int maxAssignments) {
        return new JpaSoftwareModuleType(key, name, description, maxAssignments);
    }

    @Override
    public Rollout generateRollout() {
        return new JpaRollout();
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
    public ActionStatus generateActionStatus() {
        return new JpaActionStatus();
    }

    @Override
    public ActionStatus generateActionStatus(final Action action, final Status status, final Long occurredAt,
            final String message) {
        return new JpaActionStatus((JpaAction) action, status, occurredAt, message);
    }

    @Override
    public ActionStatus generateActionStatus(final Action action, final Status status, final Long occurredAt,
            final Collection<String> messages) {

        final ActionStatus result = new JpaActionStatus((JpaAction) action, status, occurredAt, null);
        messages.forEach(result::addMessage);

        return result;
    }

    @Override
    public ActionStatus generateActionStatus(final Action action, final Status status, final Long occurredAt) {
        return new JpaActionStatus(action, status, occurredAt);
    }

    @Override
    public LocalArtifact generateLocalArtifact() {
        return new JpaLocalArtifact();
    }

}
