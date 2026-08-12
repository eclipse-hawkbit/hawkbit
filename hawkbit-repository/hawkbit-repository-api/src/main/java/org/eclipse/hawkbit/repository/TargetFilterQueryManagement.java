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

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Management service for {@link TargetFilterQuery}s.
 */
public interface TargetFilterQueryManagement<T extends TargetFilterQuery>
        extends RepositoryManagement<T, TargetFilterQueryManagement.UpdateCreate, TargetFilterQueryManagement.Update> {

    @Override
    default String permissionGroup() {
        return SpPermission.TARGET;
    }

    /**
     * Retrieves the {@link TargetFilterQuery} with the given name. Names are unique per tenant, so at most one is returned.
     *
     * @param name the name of the target filter query
     * @return the matching {@link TargetFilterQuery} or an empty {@link Optional} if none exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Optional<TargetFilterQuery> findByName(@NotNull String name);

    /**
     * Finds the auto assignment with the same name and query as the target filter query if it exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Optional<AutoAssignment> findLinkedAutoAssignment(final long id);

    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    AutoAssignment createLinkedAutoAssignment(final long id, final AutoAssignmentManagement.Create create);

    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    void deleteLinkedAutoAssignment(final long id);

    /**
     * Verifies the provided filter syntax.
     *
     * @param query to verify
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *         given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    void verifyTargetFilterQuerySyntax(@NotNull String query);

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
        @NotNull(groups = DistributionSetTagManagement.Create.class)
        private String name;

        @ValidString
        @Size(min = 1, max = TargetFilterQuery.QUERY_MAX_SIZE)
        private String query;
    }
}