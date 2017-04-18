/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaTenantConfiguration;
import org.eclipse.hawkbit.repository.model.TenantConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * The spring-data repository for the entity {@link TenantConfiguration}.
 *
 */
@Transactional(readOnly = true)
public interface TenantConfigurationRepository extends BaseEntityRepository<JpaTenantConfiguration, Long> {

    /**
     * Finds a specific {@link TenantConfiguration} by the configuration key.
     * 
     * @param configurationKey
     *            the configuration key to find the configuration for
     * @return the found tenant configuration object otherwise {@code null}
     */
    JpaTenantConfiguration findByKey(String configurationKey);

    /**
     * Deletes a tenant configuration by tenant and key.
     * 
     * @param tenant
     *            the tenant for this configuration
     * @param keyName
     *            the name of the key to be deleted
     */
    void deleteByKey(String keyName);

}
