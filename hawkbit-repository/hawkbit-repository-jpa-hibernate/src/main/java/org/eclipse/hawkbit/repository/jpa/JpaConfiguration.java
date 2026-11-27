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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import jakarta.persistence.spi.PersistenceUnitInfo;

import lombok.Data;
import org.eclipse.hawkbit.repository.jpa.model.EntityPropertyChangeListener;
import org.eclipse.hawkbit.repository.jpa.utils.ExceptionMapper;
import org.eclipse.hawkbit.repository.jpa.utils.JpaExceptionTranslator;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.MultiTenancySettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jpa.autoconfigure.JpaBaseConfiguration;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * General Hibernate configuration for hawkBit's Repository.
 */
@Configuration
@Import(JpaConfiguration.Properties.class)
public class JpaConfiguration extends JpaBaseConfiguration {

    @Data
    @ConfigurationProperties // predix is "/" intentionally
    protected static class Properties {

        private final Map<String, String> hibernate = new HashMap<>();
    }

    private final TenantIdentifier tenantIdentifier;
    private final Map<String, String> hibernateProperties;

    protected JpaConfiguration(
            final DataSource dataSource, final JpaProperties properties,
            final ObjectProvider<JtaTransactionManager> jtaTransactionManagerProvider,
            final Properties hibernateProperties) {
        super(dataSource, properties, jtaTransactionManagerProvider);
        tenantIdentifier = new TenantIdentifier();
        this.hibernateProperties = hibernateProperties.getHibernate();
    }

    @Bean
    CurrentTenantIdentifierResolver<String> currentTenantIdentifierResolver() {
        return tenantIdentifier;
    }

    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {

        return new HibernateJpaVendorAdapter() {

            private final HibernateJpaDialect hibernateJpaDialect = new CustomHibernateJpaDialect();

            @Override
            public HibernateJpaDialect getJpaDialect() {
                return hibernateJpaDialect;
            }

            // Spring sets 'hibernate.dialect' derived from 'spring.jpa.database' (always set by hawkBit, e.g. for the Flyway
            // script suffix). Hibernate detects the dialect from the JDBC metadata and logs a deprecation warning (HHH90000025)
            // for an explicitly set dialect. So, keep it only if explicitly configured via 'spring.jpa.database-platform'.
            @Override
            public Map<String, Object> getJpaPropertyMap(final PersistenceUnitInfo persistenceUnitInfo) {
                return withoutDerivedDialect(super.getJpaPropertyMap(persistenceUnitInfo));
            }

            @Override
            public Map<String, Object> getJpaPropertyMap() {
                return withoutDerivedDialect(super.getJpaPropertyMap());
            }

            private Map<String, Object> withoutDerivedDialect(final Map<String, Object> jpaPropertyMap) {
                if (getDatabasePlatform() != null) {
                    return jpaPropertyMap;
                }
                final Map<String, Object> withoutDialect = new HashMap<>(jpaPropertyMap);
                withoutDialect.remove(JdbcSettings.DIALECT);
                return withoutDialect;
            }
        };
    }

    /**
     * {@link PlatformTransactionManager} bean. It handles conversion of dao / jpa exceptions to transaction exceptions
     */
    @Override
    @Bean
    public PlatformTransactionManager transactionManager(final ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
        return new JpaTransactionManager() {

            @Override
            protected void doCommit(final DefaultTransactionStatus status) {
                try {
                    super.doCommit(status);
                } catch (final RuntimeException e) {
                    throw ExceptionMapper.mapRe(e);
                }
            }
        };
    }

    @Override
    protected Map<String, Object> getVendorProperties(final DataSource dataSource) {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.physical_naming_strategy", PhysicalNamingStrategyStandardImpl.class.getName());
        properties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifier);
        properties.put("hibernate.multiTenancy", "DISCRIMINATOR");
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
        // JDBC batching of updates / deletes (the counterpart of EclipseLink's batch writing), ordered so that more
        // statements fall into one batch
        properties.put("hibernate.jdbc.batch_size", "500");
        properties.put("hibernate.order_updates", "true");

        // override with all explicitly configured properties
        properties.putAll(hibernateProperties);
        return properties;
    }

    static class CustomHibernateJpaDialect extends HibernateJpaDialect {

        @Serial
        private static final long serialVersionUID = 1L;

        protected CustomHibernateJpaDialect() {
            super();
            this.setJdbcExceptionTranslator(JpaExceptionTranslator.getTranslator());
        }
    }
}
