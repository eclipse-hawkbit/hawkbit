/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for {@link SoftwareModuleType}.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
public interface SoftwareModuleTypeRepository
        extends BaseEntityRepository<JpaSoftwareModuleType, Long>, JpaSpecificationExecutor<JpaSoftwareModuleType> {

    /**
     * @param pageable
     * @param isDeleted
     *            to <code>true</code> if only marked as deleted have to be
     *            count or all undeleted.
     * @return found {@link SoftwareModuleType}s.
     */
    Page<SoftwareModuleType> findByDeleted(Pageable pageable, boolean isDeleted);

    /**
     * @param isDeleted
     *            to <code>true</code> if only marked as deleted have to be
     *            count or all undeleted.
     * @return number of {@link SoftwareModuleType}s in the repository.
     */
    Long countByDeleted(boolean isDeleted);

    /**
     *
     * @param key
     *            to search for
     * @return all {@link SoftwareModuleType}s in the repository with given
     *         {@link SoftwareModuleType#getKey()}
     */
    JpaSoftwareModuleType findByKey(String key);

    /**
     *
     * @param name
     *            to search for
     * @return all {@link SoftwareModuleType}s in the repository with given
     *         {@link SoftwareModuleType#getName()}
     */
    JpaSoftwareModuleType findByName(String name);
}
