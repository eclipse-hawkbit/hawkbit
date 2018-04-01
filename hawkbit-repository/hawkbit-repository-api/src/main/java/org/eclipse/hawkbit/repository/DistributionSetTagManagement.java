/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Optional;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link DistributionSetTag}s.
 *
 */
public interface DistributionSetTagManagement extends RepositoryManagement<DistributionSetTag, TagCreate, TagUpdate> {

    /**
     * Deletes {@link DistributionSetTag} by given
     * {@link DistributionSetTag#getName()}.
     *
     * @param tagName
     *            to be deleted
     * 
     * @throws EntityNotFoundException
     *             if tag with given name does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void delete(@NotEmpty String tagName);

    /**
     * Find {@link DistributionSet} based on given name.
     *
     * @param name
     *            to look for.
     * @return {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<DistributionSetTag> getByName(@NotEmpty String name);

    /**
     * Finds all {@link TargetTag} assigned to given {@link Target}.
     * 
     * @param pageable
     *            information for page size, offset and sort order.
     *
     * @param setId
     *            of the {@link DistributionSet}
     * @return page of the found {@link TargetTag}s
     * 
     * @throws EntityNotFoundException
     *             if {@link DistributionSet} with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<DistributionSetTag> findByDistributionSet(@NotNull Pageable pageable, long setId);

}
