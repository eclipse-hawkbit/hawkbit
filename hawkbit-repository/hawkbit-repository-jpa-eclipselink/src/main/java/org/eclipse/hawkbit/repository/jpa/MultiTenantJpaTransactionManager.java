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

import java.io.Serial;
import java.util.Objects;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transaction;

import org.eclipse.hawkbit.repository.jpa.model.EntityPropertyChangeListener;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.DescriptorEventManager;
import org.eclipse.persistence.sessions.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link JpaTransactionManager} that sets the {@link TenantAware#getCurrentTenant()} in the eclipselink session. This has
 * to be done in eclipselink after a {@link Transaction} has been started.
 * <p/>
 * The class also handles setting the {@link EntityPropertyChangeListener} to the {@link DescriptorEventManager} of the
 */
class MultiTenantJpaTransactionManager extends JpaTransactionManager {

    @Serial
    private static final long serialVersionUID = 1L;

    private transient TenantAware tenantAware;

    private static final Class<?> JPA_TARGET;

    static {
        try {
            JPA_TARGET = Class.forName("org.eclipse.hawkbit.repository.jpa.model.JpaTarget");
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    MultiTenantJpaTransactionManager(final TenantAware tenantAware) {
        this.tenantAware = tenantAware;
    }

    private static final EntityPropertyChangeListener ENTITY_PROPERTY_CHANGE_LISTENER = new EntityPropertyChangeListener();

    @Override
    protected void doBegin(final Object transaction, final TransactionDefinition definition) {
        super.doBegin(transaction, definition);

        final EntityManager em = Objects.requireNonNull(
                        (EntityManagerHolder) TransactionSynchronizationManager.getResource(
                                Objects.requireNonNull(
                                        getEntityManagerFactory(),
                                        "No EntityManagerFactory provided by TransactionSynchronizationManager")),
                        "No EntityManagerHolder provided by TransactionSynchronizationManager")
                .getEntityManager();

        final ClassDescriptor classDescriptor = em.unwrap(Session.class).getClassDescriptor(JPA_TARGET);
        if (classDescriptor != null) {
            final DescriptorEventManager dem = classDescriptor.getEventManager();
            if (dem != null && !dem.getEventListeners().contains(ENTITY_PROPERTY_CHANGE_LISTENER)) {
                dem.addListener(ENTITY_PROPERTY_CHANGE_LISTENER);
            }
        }

        final String currentTenant = tenantAware.getCurrentTenant();
        if (currentTenant == null) {
            cleanupTenant(em);
        } else {
            em.setProperty(PersistenceUnitProperties.MULTITENANT_PROPERTY_DEFAULT, currentTenant.toUpperCase());
        }
    }

    private void cleanupTenant(final EntityManager em) {
        if (em.isOpen() && em.getProperties().containsKey(PersistenceUnitProperties.MULTITENANT_PROPERTY_DEFAULT)) {
            em.setProperty(PersistenceUnitProperties.MULTITENANT_PROPERTY_DEFAULT, "");
        }
    }
}