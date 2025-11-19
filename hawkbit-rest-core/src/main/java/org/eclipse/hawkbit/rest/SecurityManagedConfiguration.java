/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.rest.security.DosFilter;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.util.CollectionUtils;

/**
 * All configurations related to HawkBit's auth and authorization layer.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@Order(Ordered.HIGHEST_PRECEDENCE)
@PropertySource("classpath:hawkbit-security-defaults.properties")
public class SecurityManagedConfiguration {

    public static final String ANONYMOUS_CONTROLLER_SECURITY_ENABLED_SHOULD_ONLY_BE_USED_FOR_DEVELOPMENT_PURPOSES = """
            ******************
            ** Anonymous controller security enabled, should only be used for development purposes **
            ******************""";
    public static final int DOS_FILTER_ORDER = -200;

    public static FilterRegistrationBean<DosFilter> dosFilter(final Collection<String> includeAntPaths,
            final HawkbitSecurityProperties.Dos.Filter filterProperties,
            final HawkbitSecurityProperties.Clients clientProperties) {
        final FilterRegistrationBean<DosFilter> filterRegBean = new FilterRegistrationBean<>();

        filterRegBean.setFilter(new DosFilter(includeAntPaths, filterProperties.getMaxRead(),
                filterProperties.getMaxWrite(), filterProperties.getWhitelist(), clientProperties.getBlacklist(),
                clientProperties.getRemoteIpHeader()));

        return filterRegBean;
    }

    /**
     * Filter to protect the hawkBit server system management interface against too many requests.
     *
     * @param securityProperties for filter configuration
     * @return the spring filter registration bean for registering a denial of service protection filter in the filter chain
     */
    @Bean
    @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.filter", name = "enabled", matchIfMissing = true)
    public FilterRegistrationBean<DosFilter> dosSystemFilter(final HawkbitSecurityProperties securityProperties) {
        final FilterRegistrationBean<DosFilter> filterRegBean = dosFilter(Collections.emptyList(),
                securityProperties.getDos().getFilter(), securityProperties.getClients());
        filterRegBean.setUrlPatterns(List.of("/system/*"));
        filterRegBean.setOrder(DOS_FILTER_ORDER);
        filterRegBean.setName("dosSystemFilter");

        return filterRegBean;
    }

    /**
     * HttpFirewall which enables to define a list of allowed host names.
     *
     * @return the http firewall.
     */
    @Bean
    public HttpFirewall httpFirewall(final HawkbitSecurityProperties hawkbitSecurityProperties) {
        final List<String> allowedHostNames = hawkbitSecurityProperties.getAllowedHostNames();
        final IgnorePathsStrictHttpFirewall firewall = new IgnorePathsStrictHttpFirewall(
                hawkbitSecurityProperties.getHttpFirewallIgnoredPaths());

        if (!CollectionUtils.isEmpty(allowedHostNames)) {
            firewall.setAllowedHostnames(hostName -> {
                log.debug("Firewall check host: {}, allowed: {}", hostName, allowedHostNames.contains(hostName));
                return allowedHostNames.contains(hostName);
            });
        }
        return firewall;
    }

    private static class IgnorePathsStrictHttpFirewall extends StrictHttpFirewall {

        private final Collection<String> pathsToIgnore;

        public IgnorePathsStrictHttpFirewall(final Collection<String> pathsToIgnore) {
            super();
            this.pathsToIgnore = pathsToIgnore;
        }

        @Override
        public FirewalledRequest getFirewalledRequest(final HttpServletRequest request) {
            if (pathsToIgnore != null && pathsToIgnore.contains(request.getRequestURI())) {
                return new FirewalledRequest(request) {

                    @Override
                    public void reset() {
                        // nothing to do
                    }
                };
            }
            return super.getFirewalledRequest(request);
        }
    }
}
