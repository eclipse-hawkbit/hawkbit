/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.configuration;

import javax.persistence.EntityManager;
import javax.transaction.Transaction;

import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link JpaTransactionManager} that sets the
 * {@link TenantAware#getCurrentTenant()} in the eclipselink session. This has
 * to be done in eclipselink after a {@link Transaction} has been started.
 *
 */
public class MultiTenantJpaTransactionManager extends JpaTransactionManager {
    private static final long serialVersionUID = 1L;

    @Autowired
    private transient TenantAware tenantAware;

    @Override
    protected void doBegin(final Object transaction, final TransactionDefinition definition) {
        super.doBegin(transaction, definition);

        final String currentTenant = tenantAware.getCurrentTenant();
        if (currentTenant != null) {
            final EntityManagerHolder emHolder = (EntityManagerHolder) TransactionSynchronizationManager
                    .getResource(getEntityManagerFactory());
            final EntityManager em = emHolder.getEntityManager();
            em.setProperty(PersistenceUnitProperties.MULTITENANT_PROPERTY_DEFAULT, currentTenant.toUpperCase());
        }
    }
}
