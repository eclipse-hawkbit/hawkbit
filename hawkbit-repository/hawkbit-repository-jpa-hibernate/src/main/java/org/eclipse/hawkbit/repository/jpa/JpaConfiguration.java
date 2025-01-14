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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.eclipse.hawkbit.repository.jpa.model.EntityPropertyChangeListener;

import org.eclipse.hawkbit.tenancy.TenantAware;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * General Hibernate configuration for hawkBit's Repository.
 */
@Configuration
public class JpaConfiguration extends JpaBaseConfiguration {

    private final TenantIdentifier tenantIdentifier;
    private final boolean enableLazyLoadNoTrans;

    protected JpaConfiguration(
            final DataSource dataSource, final JpaProperties properties,
            final ObjectProvider<JtaTransactionManager> jtaTransactionManagerProvider,
            final TenantAware.TenantResolver tenantResolver,
            @Value("${hibernate.enable_lazy_load_no_trans:true}") final boolean enableLazyLoadNoTrans) {
        super(dataSource, properties, jtaTransactionManagerProvider);
        tenantIdentifier = new TenantIdentifier(tenantResolver);
        this.enableLazyLoadNoTrans = enableLazyLoadNoTrans;
    }

    @Bean
    CurrentTenantIdentifierResolver<String> currentTenantIdentifierResolver() {
        return tenantIdentifier;
    }

    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
        return new HibernateJpaVendorAdapter() {

            private final HibernateJpaDialect jpaDialect = new HibernateJpaDialect();

            @Override
            public HibernateJpaDialect getJpaDialect() {
                return jpaDialect;
            }
        };
    }

    @Override
    protected Map<String, Object> getVendorProperties() {
        final Map<String, Object> properties = new HashMap<>(4);

        properties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifier);
        properties.put("hibernate.multiTenancy", "DISCRIMINATOR");
        // LAZY_LOAD - Enable lazy loading of lazy fields when session is closed - N + 1 problem occur.
        // So it would be good if in future hawkBit run without that
        // Otherwise, if false, call for the lazy field will throw LazyInitializationException
        properties.put("hibernate.enable_lazy_load_no_trans", enableLazyLoadNoTrans);
        properties.put("hibernate.integrator_provider", (IntegratorProvider) () -> Collections.singletonList(new Integrator() {

            @Override
            public void integrate(
                    final Metadata metadata, final BootstrapContext bootstrapContext,
                    final SessionFactoryImplementor sessionFactory) {
                sessionFactory.getServiceRegistry()
                        .getService(EventListenerRegistry.class)
                        .appendListeners(EventType.POST_UPDATE, new EntityPropertyChangeListener());
            }

            @Override
            public void disintegrate(final SessionFactoryImplementor sessionFactory, final SessionFactoryServiceRegistry serviceRegistry) {
                // do nothing
            }
        }));
        return properties;
    }
}