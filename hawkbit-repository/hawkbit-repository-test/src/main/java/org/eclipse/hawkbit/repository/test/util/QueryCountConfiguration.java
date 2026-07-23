/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Opt-in test configuration that wraps the application {@link DataSource} with a {@link QueryCountingDataSource} and
 * exposes the shared {@link QueryCount}. Import it on a test that needs to assert how many SQL statements reach the DB:
 *
 * <pre>{@code
 * @Import(QueryCountConfiguration.class)
 * class MyPerfTest extends AbstractJpaIntegrationTest {
 *     @Autowired QueryCount queryCount;
 * }
 * }</pre>
 *
 * Provider-agnostic - counting happens at the JDBC layer, so it behaves identically under EclipseLink and Hibernate.
 */
@Configuration
public class QueryCountConfiguration {

    @Bean
    public QueryCount queryCount() {
        return new QueryCount();
    }

    @Bean
    static BeanPostProcessor queryCountingDataSourcePostProcessor(final ObjectProvider<QueryCount> queryCount) {
        return new BeanPostProcessor() {

            @Override
            public Object postProcessAfterInitialization(final Object bean, final String beanName) {
                if (bean instanceof DataSource dataSource && !(bean instanceof QueryCountingDataSource)) {
                    return new QueryCountingDataSource(dataSource, queryCount.getObject());
                }
                return bean;
            }
        };
    }
}
