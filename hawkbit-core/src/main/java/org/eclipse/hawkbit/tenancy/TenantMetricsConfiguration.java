/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.tenancy;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.Tag;
import io.micrometer.observation.ObservationRegistry;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.tenancy.TenantAware.TenantResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.boot.actuate.autoconfigure.observation.web.servlet.WebMvcObservationAutoConfiguration;
import org.springframework.boot.actuate.metrics.data.DefaultRepositoryTagsProvider;
import org.springframework.boot.actuate.metrics.data.RepositoryTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocation;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationConvention;
import org.springframework.web.filter.ServerHttpObservationFilter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TenantMetricsConfiguration {

    public static final String TENANT_TAG = "tenant";

    @AutoConfiguration(after = ObservationAutoConfiguration.class)
    @ConditionalOnProperty(name = "hawkbit.metrics.tenancy.web.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = { "org.springframework.web.servlet.DispatcherServlet", "io.micrometer.observation.Observation" })
    @ConditionalOnBean({ ObservationRegistry.class, TenantResolver.class })
    public static class WebConfig {

        @Bean
        @Primary
        public DefaultServerRequestObservationConvention serverRequestObservationConvention(final TenantResolver tenantResolver) {
            return new DefaultServerRequestObservationConvention() {

                @Override
                public KeyValues getLowCardinalityKeyValues(final ServerRequestObservationContext context) {
                    // Make sure that KeyValues entries are already sorted by name for better performance
                    return KeyValues.of(exception(context), method(context), outcome(context), status(context), tenant(), uri(context));
                }

                private KeyValue tenant() {
                    return KeyValue.of(TENANT_TAG, Optional.ofNullable(tenantResolver.resolveTenant()).orElse("n/a"));
                }
            };
        }

        @Bean
        @Primary
        public FilterRegistrationBean<ServerHttpObservationFilter> webMvcObservationFilter(
                final ObservationRegistry registry,
                // should be serverRequestObservationConvention (registered above)
                final ObjectProvider<ServerRequestObservationConvention> customConvention,
                final ObservationProperties observationProperties,
                final SecurityProperties securityProperties) {
            final FilterRegistrationBean<ServerHttpObservationFilter> filterRegistrationBean = new WebMvcObservationAutoConfiguration()
                    .webMvcObservationFilter(registry, customConvention, observationProperties);
            // after security filter, so to be able to log tenant
            filterRegistrationBean.setOrder(securityProperties.getFilter().getOrder() + 1);
            return filterRegistrationBean;
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "hawkbit.metrics.tenancy.repository.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(name = {
            "io.micrometer.core.instrument.Tag",
            "org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocation" })
    @ConditionalOnBean(TenantResolver.class)
    public static class RepositoryConfig {

        @Bean
        public RepositoryTagsProvider repositoryTagsProvider(final TenantResolver tenantResolver) {
            return new DefaultRepositoryTagsProvider() {

                @Override
                public Iterable<Tag> repositoryTags(final RepositoryMethodInvocation invocation) {
                    final Iterable<Tag> defaultTags = super.repositoryTags(invocation);
                    final String tenant = Optional.ofNullable(tenantResolver.resolveTenant()).orElse("n/a");
                    return () -> {
                        final Iterator<Tag> defaultTagsIterator = defaultTags.iterator();
                        return new Iterator<>() {

                            private boolean tenantReturned;

                            @Override
                            public boolean hasNext() {
                                return defaultTagsIterator.hasNext() || !tenantReturned;
                            }

                            @Override
                            public Tag next() {
                                if (defaultTagsIterator.hasNext()) {
                                    return defaultTagsIterator.next();
                                } else {
                                    if (tenantReturned) {
                                        throw new NoSuchElementException();
                                    } else {
                                        tenantReturned = true;
                                        return Tag.of(TENANT_TAG, tenant);
                                    }
                                }
                            }
                        };
                    };
                }
            };
        }
    }
}