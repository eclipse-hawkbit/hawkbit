/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.ActionStatusBuilder;
import org.eclipse.hawkbit.repository.builder.DistributionSetBuilder;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeBuilder;
import org.eclipse.hawkbit.repository.builder.RolloutBuilder;
import org.eclipse.hawkbit.repository.builder.RolloutGroupBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeBuilder;
import org.eclipse.hawkbit.repository.builder.TagBuilder;
import org.eclipse.hawkbit.repository.builder.TargetBuilder;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaActionStatusBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaRolloutGroupBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleTypeBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTagBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTargetBuilder;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;

/**
 * JPA Implementation of {@link EntityFactory}.
 *
 */
@Validated
public class JpaEntityFactory implements EntityFactory {

    @Autowired
    private DistributionSetBuilder distributionSetBuilder;

    @Autowired
    private DistributionSetTypeBuilder distributionSetTypeBuilder;

    @Autowired
    private SoftwareModuleBuilder softwareModuleBuilder;

    @Autowired
    private RolloutBuilder rolloutBuilder;

    @Autowired
    private TargetFilterQueryBuilder targetFilterQueryBuilder;

    @Autowired
    private SoftwareModuleMetadataBuilder softwareModuleMetadataBuilder;

    @Override
    public MetaData generateMetadata(final String key, final String value) {
        return new JpaDistributionSetMetadata(key, value);
    }

    @Override
    public DistributionSetTypeBuilder distributionSetType() {
        return distributionSetTypeBuilder;
    }

    @Override
    public DistributionSetBuilder distributionSet() {
        return distributionSetBuilder;
    }

    @Override
    public TargetBuilder target() {
        return new JpaTargetBuilder();
    }

    @Override
    public TagBuilder tag() {
        return new JpaTagBuilder();
    }

    @Override
    public TargetFilterQueryBuilder targetFilterQuery() {
        return targetFilterQueryBuilder;
    }

    @Override
    public SoftwareModuleBuilder softwareModule() {
        return softwareModuleBuilder;
    }

    @Override
    public SoftwareModuleTypeBuilder softwareModuleType() {
        return new JpaSoftwareModuleTypeBuilder();
    }

    @Override
    public ActionStatusBuilder actionStatus() {
        return new JpaActionStatusBuilder();
    }

    @Override
    public RolloutBuilder rollout() {
        return rolloutBuilder;
    }

    @Override
    public RolloutGroupBuilder rolloutGroup() {
        return new JpaRolloutGroupBuilder();
    }

    @Override
    public SoftwareModuleMetadataBuilder softwareModuleMetadata() {
        return softwareModuleMetadataBuilder;
    }

}
