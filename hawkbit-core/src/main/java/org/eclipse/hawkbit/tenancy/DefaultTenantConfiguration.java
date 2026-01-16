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

import jakarta.servlet.DispatcherType;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.Tag;
import io.micrometer.observation.ObservationRegistry;
import lombok.NonNull;
import org.eclipse.hawkbit.context.AccessContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.data.metrics.DefaultRepositoryTagsProvider;
import org.springframework.boot.data.metrics.RepositoryTagsProvider;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.web.filter.ServerHttpObservationFilter;

@AutoConfiguration
public class DefaultTenantConfiguration {

    public static final String TENANT_TAG = "tenant";

    @Bean
    @ConditionalOnMissingBean
    TenantAwareCacheManager cacheManager() {
        return TenantAwareCacheManager.getInstance();
    }

    @AutoConfiguration(afterName = {
            "org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration",
            "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration" })
    @ConditionalOnProperty(name = "hawkbit.metrics.tenancy.web.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = { "io.micrometer.observation.Observation", "org.springframework.web.servlet.DispatcherServlet" })
    @ConditionalOnBean(ObservationRegistry.class)
    public static class WebConfig {

        @Bean
        @Primary
        public FilterRegistrationBean<ServerHttpObservationFilter> webMvcObservationFilter(
                final ObservationRegistry registry,
                final SecurityFilterProperties securityFilterProperties) {
            final FilterRegistrationBean<ServerHttpObservationFilter> filterRegistrationBean = new FilterRegistrationBean<>(
                    new ServerHttpObservationFilter(registry, new DefaultServerRequestObservationConvention() {

                        @NonNull
                        @Override
                        public KeyValues getLowCardinalityKeyValues(@NonNull final ServerRequestObservationContext context) {
                            // Make sure that KeyValues entries are already sorted by name for better performance
                            return KeyValues.of(exception(context), method(context), outcome(context), status(context), tenant(), uri(context));
                        }

                        private static KeyValue tenant() {
                            return KeyValue.of(TENANT_TAG, Optional.ofNullable(AccessContext.tenant()).orElse("n/a"));
                        }
                    }));
            filterRegistrationBean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD);
            // after security filter, so to be able to log tenant
            filterRegistrationBean.setOrder(securityFilterProperties.getOrder() + 1);
            return filterRegistrationBean;
        }
    }

    @AutoConfiguration(afterName = "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration")
    @ConditionalOnClass(name = "org.springframework.boot.data.metrics.DefaultRepositoryTagsProvider")
    @ConditionalOnProperty(name = "hawkbit.metrics.tenancy.repository.enabled", havingValue = "true", matchIfMissing = true)
    public static class RepositoryConfig {

        @Bean
        public RepositoryTagsProvider repositoryTagsProvider() {
            return new DefaultRepositoryTagsProvider() {

                @NonNull
                @Override
                public Iterable<Tag> repositoryTags(@NonNull final RepositoryMethodInvocationListener.RepositoryMethodInvocation invocation) {
                    final Iterable<Tag> defaultTags = super.repositoryTags(invocation);
                    final String tenant = Optional.ofNullable(AccessContext.tenant()).orElse("n/a");
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