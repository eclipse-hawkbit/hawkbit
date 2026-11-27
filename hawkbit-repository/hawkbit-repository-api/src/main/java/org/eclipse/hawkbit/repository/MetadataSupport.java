/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Represents interface for managing meta-data of entities. The keys are always Strings, while the values are generic
 * <p/>
 * The single value {@code createMetadata(Long id, String key, MV value)} is declared by the sub-interfaces with the concrete value type.
 * A {@code @Valid} parameter of the unbounded {@code MV} (erasure {@link Object}) is a potential container for Hibernate Validator
 * (deprecated, HV000272), and the sub-interfaces / implementations must not add {@code @Valid} to an inherited method parameter.
 *
 * @param <MV> the type of the meta-data value
 */
@SuppressWarnings("java:S119") // java:S119 - better self explainable
public interface MetadataSupport<MV> {

    /**
     * Creates a list of entity meta-data entries.
     *
     * @param id the entity id which meta-data has to be created
     * @param metadata the meta-data entries to create
     * @throws EntityAlreadyExistsException in case one of the meta-data entry already exists for the specific key
     * @throws EntityNotFoundException if entity with given ID does not exist
     * @throws AssignmentQuotaExceededException if the maximum number of meta-data entries is exceeded for the addressed entity
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    void createMetadata(@NotNull Long id, @NotEmpty Map<String, @Valid ? extends MV> metadata);

    /**
     * Finds all meta-data by the given entity id and key.
     *
     * @param id the entity id to retrieve the meta-data from
     * @param key the meta-data key to retrieve
     * @return a paged result of all meta-data entries for a given entity id
     * @throws EntityNotFoundException if entity with given ID does not exist ot the
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    MV getMetadata(@NotNull Long id, @NotEmpty String key);

    /**
     * Finds all meta-data by the given entity id.
     *
     * @param id the entity id to retrieve the meta-data from
     * @return a paged result of all meta-data entries for a given entity id
     * @throws EntityNotFoundException if entity with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Map<String, MV> getMetadata(@NotNull Long id);

    /**
     * Deletes a entity meta-data entry.
     *
     * @param id where meta-data has to be deleted
     * @param key of the meta-data element
     * @throws EntityNotFoundException if entity with given ID does not exist or the key is not found
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    void deleteMetadata(@NotNull Long id, @NotEmpty String key);
}
