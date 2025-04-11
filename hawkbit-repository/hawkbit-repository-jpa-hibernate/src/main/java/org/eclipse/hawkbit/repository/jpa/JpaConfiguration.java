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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * General Hibernate configuration for hawkBit's Repository.
 */
@Configuration
public class JpaConfiguration {

    private final TenantIdentifier tenantIdentifier;
    private final boolean enableLazyLoadNoTrans;

    protected JpaConfiguration(
            final TenantAware.TenantResolver tenantResolver,
            @Value("${hibernate.enable-lazy-load-no-trans:true}") final boolean enableLazyLoadNoTrans) {
        tenantIdentifier = new TenantIdentifier(tenantResolver);
        this.enableLazyLoadNoTrans = enableLazyLoadNoTrans;
    }

    @Bean
    CurrentTenantIdentifierResolver<String> currentTenantIdentifierResolver() {
        return tenantIdentifier;
    }

    @Bean
    HibernatePropertiesCustomizer hibernatePropertiesCustomizers() {
        return hibernateProperties -> {
            // override the default naming strategy
            hibernateProperties.put("hibernate.physical_naming_strategy", org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl.class.getName());
            hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifier);
            hibernateProperties.put("hibernate.multiTenancy", "DISCRIMINATOR");
            // LAZY_LOAD - Enable lazy loading of lazy fields when session is closed - N + 1 problem occur.
            // So it would be good if in future hawkBit run without that
            // Otherwise, if false, call for the lazy field will throw LazyInitializationException
            hibernateProperties.put("hibernate.enable_lazy_load_no_trans", enableLazyLoadNoTrans);
            hibernateProperties.put("hibernate.integrator_provider", (IntegratorProvider) () -> Collections.singletonList(new Integrator() {

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
        };
    }
}