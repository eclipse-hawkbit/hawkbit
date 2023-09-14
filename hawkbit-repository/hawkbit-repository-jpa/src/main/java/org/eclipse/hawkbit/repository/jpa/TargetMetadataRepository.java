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

import org.eclipse.hawkbit.repository.jpa.model.JpaTargetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.TargetMetadataCompositeKey;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link TargetMetadata} repository.
 */
@Transactional(readOnly = true)
public interface TargetMetadataRepository
        extends PagingAndSortingRepository<JpaTargetMetadata, TargetMetadataCompositeKey>,
        CrudRepository<JpaTargetMetadata, TargetMetadataCompositeKey>,
        JpaSpecificationExecutor<JpaTargetMetadata> {

    /**
     * Counts the meta data entries that match the given target ID.
     * 
     * @param id
     *            of the target.
     * 
     * @return The number of matching meta data entries.
     */
    long countByTargetId(@Param("id") Long id);
}
