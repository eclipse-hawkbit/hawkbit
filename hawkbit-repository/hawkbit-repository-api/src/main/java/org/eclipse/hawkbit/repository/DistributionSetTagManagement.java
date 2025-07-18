/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.util.Optional;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.eclipse.hawkbit.im.authentication.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link DistributionSetTag}s.
 */
public interface DistributionSetTagManagement<T extends DistributionSetTag>
        extends RepositoryManagement<T, DistributionSetTagManagement.Create, DistributionSetTagManagement.Update> {

    /**
     * Find {@link DistributionSet} based on given name.
     *
     * @param name to look for.
     * @return {@link DistributionSet}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Optional<T> findByName(@NotEmpty String name);

    /**
     * Finds all {@link TargetTag} assigned to given {@link Target}.
     *
     * @param distributionSetId of the {@link DistributionSet}
     * @param pageable information for page size, offset and sort order.
     * @return page of the found {@link TargetTag}s
     * @throws EntityNotFoundException if {@link DistributionSet} with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Page<T> findByDistributionSet(long distributionSetId, @NotNull Pageable pageable);

    /**
     * Deletes {@link DistributionSetTag} by given
     * {@link DistributionSetTag#getName()}.
     *
     * @param tagName to be deleted
     * @throws EntityNotFoundException if tag with given name does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_DELETE_REPOSITORY)
    void delete(@NotEmpty String tagName);

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Create extends UpdateCreate {}

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Update extends UpdateCreate implements Identifiable<Long> {

        @NotNull
        private Long id;
    }

    @SuperBuilder
    @Getter
    class UpdateCreate {

        @ValidString
        @Size(min = 1, max = NamedEntity.NAME_MAX_SIZE)
        @NotNull(groups = Create.class)
        private String name;

        @ValidString
        @Size(max = NamedEntity.DESCRIPTION_MAX_SIZE)
        private String description;

        @ValidString
        @Size(max = Tag.COLOUR_MAX_SIZE)
        private String colour;
    }
}