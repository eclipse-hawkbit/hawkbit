/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.List;

import org.eclipse.hawkbit.repository.model.TargetTag;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link TargetTag} repository.
 *
 *
 *
 */
@Transactional(readOnly = true)
public interface TargetTagRepository extends BaseEntityRepository<TargetTag, Long> {

    /**
     * deletes the {@link TargetTag}s with the given tag names.
     * 
     * @param tagNames
     *            to be deleted
     * @return 1 if tag was deleted
     */
    @Modifying
    @Transactional
    Long deleteByName(final String tagName);

    /**
     * find {@link TargetTag} by its name.
     * 
     * @param tagName
     *            to filter on
     * @return the {@link TargetTag} if found, otherwise null
     */
    TargetTag findByNameEquals(final String tagName);

    /**
     * Returns all instances of the type.
     *
     * @return all entities
     */
    @Override
    List<TargetTag> findAll();

    @Override
    <S extends TargetTag> List<S> save(Iterable<S> entities);
}
