/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import org.eclipse.hawkbit.repository.builder.TargetTypeBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaActionStatusBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaRolloutGroupBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaSoftwareModuleTypeBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTagBuilder;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetMetadata;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * JPA Implementation of {@link EntityFactory}.
 */
@Validated
public class JpaEntityFactory implements EntityFactory {

    @Autowired
    private DistributionSetBuilder distributionSetBuilder;

    @Autowired
    private TargetBuilder targetBuilder;

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

    @Autowired
    private TargetTypeBuilder targetTypeBuilder;

    @Override
    public ActionStatusBuilder actionStatus() {
        return new JpaActionStatusBuilder();
    }

    @Override
    public DistributionSetBuilder distributionSet() {
        return distributionSetBuilder;
    }

    @Override
    public MetaData generateDsMetadata(final String key, final String value) {
        return new JpaDistributionSetMetadata(key, StringUtils.trimWhitespace(value));
    }

    @Override
    public MetaData generateTargetMetadata(final String key, final String value) {
        return new JpaTargetMetadata(key, StringUtils.trimWhitespace(value));
    }

    @Override
    public SoftwareModuleMetadataBuilder softwareModuleMetadata() {
        return softwareModuleMetadataBuilder;
    }

    @Override
    public TagBuilder tag() {
        return new JpaTagBuilder();
    }

    @Override
    public RolloutGroupBuilder rolloutGroup() {
        return new JpaRolloutGroupBuilder();
    }

    @Override
    public DistributionSetTypeBuilder distributionSetType() {
        return distributionSetTypeBuilder;
    }

    @Override
    public RolloutBuilder rollout() {
        return rolloutBuilder;
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
    public TargetBuilder target() {
        return targetBuilder;
    }

    @Override
    public TargetTypeBuilder targetType() {
        return targetTypeBuilder;
    }

    @Override
    public TargetFilterQueryBuilder targetFilterQuery() {
        return targetFilterQueryBuilder;
    }

}
