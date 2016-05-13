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

import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * repository for operations on {@link TenantMetaData} entity.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
public interface TenantMetaDataRepository extends PagingAndSortingRepository<TenantMetaData, Long> {

    /**
     * Search {@link TenantMetaData} by tenant name.
     *
     * @param tenant
     *            to search for
     * @return found {@link TenantMetaData} or <code>null</code>
     */
    TenantMetaData findByTenantIgnoreCase(String tenant);

    /**
     * Counts the tenant by the tenant field which is either a count of one or a
     * count of zero, this is mostly to check if the tenant exists.
     * 
     * @param tenant
     *            the name of the tenant to check if it is exists
     * @return the count of the tenant by name which is either one or zero
     */
    Long countByTenantIgnoreCase(String tenant);

    @Override
    List<TenantMetaData> findAll();

    /**
     * @param tenant
     */
    void deleteByTenantIgnoreCase(String tenant);

}
