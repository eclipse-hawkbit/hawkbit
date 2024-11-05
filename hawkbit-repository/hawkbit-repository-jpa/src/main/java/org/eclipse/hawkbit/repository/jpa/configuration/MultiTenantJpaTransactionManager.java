/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.configuration;

import java.io.Serial;
import java.util.Objects;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transaction;

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
 */
public class MultiTenantJpaTransactionManager extends JpaTransactionManager {

    @Serial
    private static final long serialVersionUID = 1L;

    @Autowired
    private transient TenantAware tenantAware;

    @Override
    protected void doBegin(final Object transaction, final TransactionDefinition definition) {
        super.doBegin(transaction, definition);

        final String currentTenant = tenantAware.getCurrentTenant();
        if (currentTenant != null) {
            final EntityManagerFactory emFactory = Objects.requireNonNull(getEntityManagerFactory());
            final EntityManagerHolder emHolder = Objects.requireNonNull(
                    (EntityManagerHolder) TransactionSynchronizationManager.getResource(emFactory),
                    "No EntityManagerHolder provided by TransactionSynchronizationManager");
            final EntityManager em = emHolder.getEntityManager();
            em.setProperty(PersistenceUnitProperties.MULTITENANT_PROPERTY_DEFAULT, currentTenant.toUpperCase());
        }
    }
}
