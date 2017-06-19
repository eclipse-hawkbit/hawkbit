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

import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * repository for operations on {@link TenantMetaData} entity.
 *
 */
@Transactional(readOnly = true)
public interface TenantMetaDataRepository extends PagingAndSortingRepository<JpaTenantMetaData, Long> {

    /**
     * Search {@link TenantMetaData} by tenant name.
     *
     * @param tenant
     *            to search for
     * @return found {@link TenantMetaData} or <code>null</code>
     */
    TenantMetaData findByTenantIgnoreCase(String tenant);

    @Override
    List<JpaTenantMetaData> findAll();

    /**
     * @param tenant
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM JpaTenantMetaData t WHERE UPPER(t.tenant) = UPPER(:tenant)")
    void deleteByTenantIgnoreCase(@Param("tenant") String tenant);

}
