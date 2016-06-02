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

import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DsMetadataCompositeKey;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link DistributionSetMetadata} repository.
 *
 *
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
public interface DistributionSetMetadataRepository extends
        PagingAndSortingRepository<DistributionSetMetadata, DsMetadataCompositeKey>,
        JpaSpecificationExecutor<DistributionSetMetadata> {

    /**
     * Find list of metadata by distribution set id.
     * 
     * @param id
     *            id of distribution set
     * @return list of {DistributionSetMetadata}
     */
    List<DistributionSetMetadata> findByDistributionSetId(Long id);

}
