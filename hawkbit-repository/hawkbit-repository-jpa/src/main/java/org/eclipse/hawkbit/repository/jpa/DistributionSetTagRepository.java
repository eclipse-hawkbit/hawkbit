/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link TargetTag} repository.
 *
 */
@Transactional(readOnly = true)
public interface DistributionSetTagRepository
        extends BaseEntityRepository<JpaDistributionSetTag, Long>, JpaSpecificationExecutor<JpaDistributionSetTag> {
    /**
     * deletes the {@link DistributionSet} with the given name.
     * 
     * @param tagName
     *            to be deleted
     * @return 1 if tag was deleted
     */
    @Modifying
    @Transactional
    Long deleteByName(final String tagName);

    /**
     * find {@link DistributionSetTag} by its name.
     * 
     * @param tagName
     *            to filter on
     * @return the {@link DistributionSetTag} if found, otherwise null
     */
    Optional<DistributionSetTag> findByNameEquals(final String tagName);

    /**
     * Returns all instances of the type.
     *
     * @return all entities
     */
    @Override
    List<JpaDistributionSetTag> findAll();
}
