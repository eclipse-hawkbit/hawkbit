/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.builder.ActionStatusBuilder;
import org.eclipse.hawkbit.repository.builder.DistributionSetBuilder;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeBuilder;
import org.eclipse.hawkbit.repository.builder.RolloutBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeBuilder;
import org.eclipse.hawkbit.repository.builder.TagBuilder;
import org.eclipse.hawkbit.repository.builder.TargetBuilder;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryBuilder;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * central {@link BaseEntity} generation service. Objects are created but not
 * persisted.
 *
 */
public interface EntityFactory {

    /**
     * @return {@link ActionStatusBuilder} object
     */
    ActionStatusBuilder actionStatus();

    /**
     * @return {@link DistributionSetBuilder} object
     */
    DistributionSetBuilder distributionSet();

    /**
     * Generates an {@link MetaData} element without persisting it.
     * 
     * @param key
     *            {@link MetaData#getKey()}
     * @param value
     *            {@link MetaData#getValue()}
     * 
     * @return {@link MetaData} object
     */
    MetaData generateMetadata(@NotEmpty String key, @NotNull String value);

    /**
     * @return {@link TagBuilder} object
     */
    TagBuilder tag();

    /**
     * @return {@link DistributionSetTypeBuilder} object
     */
    DistributionSetTypeBuilder distributionSetType();

    /**
     * @return {@link RolloutBuilder} object
     */
    RolloutBuilder rollout();

    /**
     * @return {@link SoftwareModuleBuilder} object
     */
    SoftwareModuleBuilder softwareModule();

    /**
     * @return {@link SoftwareModuleTypeBuilder} object
     */
    SoftwareModuleTypeBuilder softwareModuleType();

    /**
     * @return {@link TargetBuilder} object
     */
    TargetBuilder target();

    /**
     * @return {@link TargetFilterQueryBuilder} object
     */
    TargetFilterQueryBuilder targetFilterQuery();

}
