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

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.vendor.AbstractJpaVendorAdapter;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaDialect;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * General EclipseLink configuration for hawkBit's Repository.
 */
@Configuration
public class JpaConfiguration extends JpaBaseConfiguration {

    private final TenantAware tenantAware;

    protected JpaConfiguration(
            final DataSource dataSource, final JpaProperties properties,
            final ObjectProvider<JtaTransactionManager> jtaTransactionManagerProvider,
            final TenantAware tenantAware) {
        super(dataSource, properties, jtaTransactionManagerProvider);
        this.tenantAware = tenantAware;
    }

    /**
     * {@link MultiTenantJpaTransactionManager} bean.
     *
     * @return a new {@link PlatformTransactionManager}
     * @see org.springframework.boot.autoconfigure.orm.jpa.JpaBaseConfiguration#transactionManager(ObjectProvider)
     */
    @Override
    @Bean
    public PlatformTransactionManager transactionManager(final ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
        return new MultiTenantJpaTransactionManager(tenantAware);
    }

    @Override
    protected AbstractJpaVendorAdapter createJpaVendorAdapter() {
        return new EclipseLinkJpaVendorAdapter() {

            private final HawkbitEclipseLinkJpaDialect jpaDialect = new HawkbitEclipseLinkJpaDialect();

            @Override
            public EclipseLinkJpaDialect getJpaDialect() {
                return jpaDialect;
            }
        };
    }

    @Override
    protected Map<String, Object> getVendorProperties() {
        final Map<String, Object> properties = new HashMap<>(7);
        // Turn off dynamic weaving to disable LTW lookup in static weaving mode
        properties.put(PersistenceUnitProperties.WEAVING, "false");
        // needed for reports
        properties.put(PersistenceUnitProperties.ALLOW_NATIVE_SQL_QUERIES, "true");
        // flyway
        properties.put(PersistenceUnitProperties.DDL_GENERATION, "none");
        // Embed into hawkBit logging
        properties.put(PersistenceUnitProperties.LOGGING_LOGGER, "JavaLogger");
        // Ensure that we flush only at the end of the transaction
        properties.put(PersistenceUnitProperties.PERSISTENCE_CONTEXT_FLUSH_MODE, "COMMIT");
        // Enable batch writing
        properties.put(PersistenceUnitProperties.BATCH_WRITING, "JDBC");
        // Batch size
        properties.put(PersistenceUnitProperties.BATCH_WRITING_SIZE, "500");
        return properties;
    }
}