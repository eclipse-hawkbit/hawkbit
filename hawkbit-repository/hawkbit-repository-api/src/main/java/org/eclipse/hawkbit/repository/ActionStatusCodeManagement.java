/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.ActionStatusCode;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for managing {@link ActionStatusCode}s.
 */
public interface ActionStatusCodeManagement<T extends ActionStatusCode> extends PermissionSupport {

    @Override
    default String permissionGroup() {
        return SpPermission.TARGET;
    }

    /**
     * Creates a new action status code.
     *
     * @param create the create request
     * @return the created action status code
     * @throws org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException if code already exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_CREATE_REPOSITORY)
    T create(@NotNull @Valid Create create);

    /**
     * Updates an action status code.
     *
     * @param update the update request
     * @return the updated action status code
     * @throws org.eclipse.hawkbit.repository.exception.EntityNotFoundException if not found
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    T update(@NotNull @Valid Update update);

    /**
     * Deletes an action status code from the catalogue. Action statuses and actions referencing the code by value are not
     * touched - if the code is still referenced it is reported as undocumented again.
     *
     * @param id the id
     * @throws org.eclipse.hawkbit.repository.exception.EntityNotFoundException if not found
     */
    @PreAuthorize(SpringEvalExpressions.HAS_DELETE_REPOSITORY)
    void delete(long id);

    /**
     * Gets an action status code by id.
     *
     * @param id the id
     * @return the action status code
     * @throws org.eclipse.hawkbit.repository.exception.EntityNotFoundException if not found
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    T get(long id);

    /**
     * Finds an action status code by id.
     *
     * @param id the id
     * @return the action status code if found
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Optional<T> find(long id);

    /**
     * Finds action status codes by RSQL query.
     *
     * @param rsql the RSQL query, or null for all
     * @param pageable the page request
     * @return the page of action status codes
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Page<T> findByRsql(String rsql, @NotNull Pageable pageable);

    /**
     * Finds distinct non-null action status codes that have been reported by devices
     * but do not have a corresponding catalogue entry, ordered by code ascending.
     * The sort in the incoming pageable is ignored.
     *
     * @param pageable the page request (sort is ignored)
     * @return page of undocumented codes
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Page<Integer> findUndocumentedCodes(@NotNull Pageable pageable);

    @SuperBuilder
    @Getter
    @EqualsAndHashCode
    @ToString
    final class Create {

        @NotNull
        private Integer code;

        @ValidString
        @Size(min = 1, max = NamedEntity.NAME_MAX_SIZE)
        @NotNull
        private String name;

        @ValidString
        @Size(max = NamedEntity.DESCRIPTION_MAX_SIZE)
        private String description;

        @NotNull
        @Builder.Default
        private ActionStatusCode.Severity severity = ActionStatusCode.Severity.INFO;
    }

    @SuperBuilder
    @Getter
    @EqualsAndHashCode
    @ToString
    final class Update implements Identifiable<Long> {

        @NotNull
        private Long id;

        @ValidString
        @Size(min = 1, max = NamedEntity.NAME_MAX_SIZE)
        private String name;

        @ValidString
        @Size(max = NamedEntity.DESCRIPTION_MAX_SIZE)
        private String description;

        private ActionStatusCode.Severity severity;
    }
}
