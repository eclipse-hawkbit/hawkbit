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
import java.util.function.Supplier;

import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.Tag;
import io.micrometer.observation.ObservationRegistry;
import lombok.NonNull;
import org.eclipse.hawkbit.context.AccessContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationProperties;
import org.springframework.boot.actuate.autoconfigure.observation.web.servlet.WebMvcObservationAutoConfiguration;
import org.springframework.boot.actuate.metrics.data.DefaultRepositoryTagsProvider;
import org.springframework.boot.actuate.metrics.data.RepositoryTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.http.server.observation.ServerRequestObservationContext;
import org.springframework.http.server.observation.ServerRequestObservationConvention;
import org.springframework.web.filter.ServerHttpObservationFilter;

@AutoConfiguration
public class DefaultTenantConfiguration {

    public static final String TENANT_TAG = "tenant";

    public static final Supplier<String> TENANT_TAG_VALUE_PROVIDER = () -> Optional.ofNullable(AccessContext.tenant())
                .map(String::toUpperCase)
                .orElse("N/A");

    @Bean
    @ConditionalOnMissingBean
    TenantAwareCacheManager cacheManager() {
        return TenantAwareCacheManager.getInstance();
    }

    @AutoConfiguration(afterName = {
            "org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration",
            "org.eclipse.hawkbit.autoconfigure.security.SecurityAutoConfiguration" })
    @ConditionalOnProperty(name = "hawkbit.metrics.tenancy.web.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = { "org.springframework.web.servlet.DispatcherServlet", "io.micrometer.observation.Observation" })
    @ConditionalOnBean(ObservationRegistry.class)
    public static class WebConfig {

        @Bean
        @Primary
        public DefaultServerRequestObservationConvention serverRequestObservationConvention() {
            return new DefaultServerRequestObservationConvention() {

                @NonNull
                @Override
                public KeyValues getLowCardinalityKeyValues(@NonNull final ServerRequestObservationContext context) {
                    // Make sure that KeyValues entries are already sorted by name for better performance
                    return KeyValues.of(exception(context), method(context), outcome(context), status(context), tenant(), uri(context));
                }

                private KeyValue tenant() {
                    return KeyValue.of(TENANT_TAG, TENANT_TAG_VALUE_PROVIDER.get());
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

    @AutoConfiguration(afterName = "org.eclipse.hawkbit.autoconfigure.security.SecurityAutoConfiguration")
    @ConditionalOnProperty(name = "hawkbit.metrics.tenancy.repository.enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnClass(name = {
            "io.micrometer.core.instrument.Tag",
            "org.springframework.data.repository.core.support.RepositoryMethodInvocationListener" })
    public static class RepositoryConfig {

        @Bean
        public RepositoryTagsProvider repositoryTagsProvider() {
            return new DefaultRepositoryTagsProvider() {

                @Override
                public Iterable<Tag> repositoryTags(final RepositoryMethodInvocationListener.RepositoryMethodInvocation invocation) {
                    final Iterable<Tag> defaultTags = super.repositoryTags(invocation);
                    final String tenant = TENANT_TAG_VALUE_PROVIDER.get();
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