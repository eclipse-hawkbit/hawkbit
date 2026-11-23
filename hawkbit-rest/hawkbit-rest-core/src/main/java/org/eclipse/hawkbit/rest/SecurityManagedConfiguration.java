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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.rest.security.ClientAddress;
import org.eclipse.hawkbit.rest.security.DosFilter;
import org.eclipse.hawkbit.rest.security.ForwardedFilter;
import org.eclipse.hawkbit.rest.security.ForwardedResolver;
import org.eclipse.hawkbit.rest.security.ForwardedValve;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.utils.IpUtil;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * All configurations related to HawkBit's authentication and authorization layer.
 */
@Slf4j
@AutoConfiguration
@EnableWebSecurity
@Order(Ordered.HIGHEST_PRECEDENCE)
@PropertySource("classpath:hawkbit-rest-defaults.properties")
public class SecurityManagedConfiguration {

    public static final int DOS_FILTER_ORDER = -200;

    static final String FORWARD_HEADERS_STRATEGY = "server.forward-headers-strategy";
    private static final String TOMCAT_SERVLET_WEB_SERVER_FACTORY = "org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory";
    private static final String NONE = "NONE";
    private static final List<String> TOMCAT_REMOTE_IP_PROPERTIES = List.of(
            "server.tomcat.remoteip.internal-proxies", "server.tomcat.remoteip.trusted-proxies", "server.tomcat.remoteip.remote-ip-header",
            "server.tomcat.remoteip.protocol-header", "server.tomcat.remoteip.host-header", "server.tomcat.remoteip.port-header");

    /**
     * Client IP resolution mode.
     */
    public enum Mode {

        /**
         * Forwarded headers processed by hawkBit with the {@link ForwardedValve} (embedded Tomcat).
         */
        VALVE,
        /**
         * Forwarded headers processed by hawkBit with the {@link ForwardedFilter} (other web servers).
         */
        FILTER,
        /**
         * Forwarded headers not processed by hawkBit - the client IP is read from the remote IP header as is ({@link LegacyClientAddressFilter}).
         */
        LEGACY
    }

    /**
     * The forwarded headers processing (client IP, scheme, host and port resolution) - used by the {@link ForwardedValve} or
     * {@link ForwardedFilter}.
     */
    @Bean
    public ForwardedResolver forwardedResolver(final HawkbitSecurityProperties securityProperties) {
        return ForwardedResolver.of(securityProperties);
    }

    /**
     * The client address filter - the first filter (for all dispatches), depending on the {@link Mode}:
     * <ul>
     *     <li>{@link Mode#FILTER} - the {@link ForwardedFilter}</li>
     *     <li>{@link Mode#LEGACY} - the {@link LegacyClientAddressFilter}</li>
     *     <li>{@link Mode#VALVE} - disabled, the {@link ForwardedValve} does the job</li>
     * </ul>
     * The mode is decided (and logged) once, here.
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public FilterRegistrationBean<Filter> clientAddressFilter(
            final ApplicationContext applicationContext, final HawkbitSecurityProperties securityProperties,
            final ForwardedResolver forwardedResolver) {
        final Mode mode = mode(applicationContext);
        logMode(mode, applicationContext, securityProperties);
        final String remoteIpHeader = securityProperties.getClients().getRemoteIpHeader();
        // a bean method can't return null - in valve mode a disabled registration
        final FilterRegistrationBean<Filter> filterRegBean = new FilterRegistrationBean<>(mode == Mode.FILTER
                ? new ForwardedFilter(forwardedResolver)
                : new LegacyClientAddressFilter(remoteIpHeader));
        filterRegBean.setEnabled(mode != Mode.VALVE);
        filterRegBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        filterRegBean.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR, DispatcherType.ASYNC);
        filterRegBean.setName("clientAddressFilter");
        return filterRegBean;
    }

    /**
     * @param applicationContext the application context
     * @return the client IP resolution mode - {@link Mode#LEGACY} if not running with a web server (e.g. mock web environment in tests) or
     *         with forward headers strategy other than <code>NONE</code>
     */
    public static Mode mode(final ApplicationContext applicationContext) {
        if (!(applicationContext instanceof WebServerApplicationContext) || !isForwardedProcessing(applicationContext.getEnvironment())) {
            return Mode.LEGACY;
        }
        return isEmbeddedTomcat(applicationContext) ? Mode.VALVE : Mode.FILTER;
    }

    /**
     * @param environment the environment
     * @return if hawkBit processes the forwarded headers - the effective forward headers strategy is <code>NONE</code>
     */
    public static boolean isForwardedProcessing(final Environment environment) {
        return NONE.equals(forwardHeadersStrategy(environment));
    }

    /**
     * Creates a (not yet registered) {@link DosFilter} registration.
     *
     * @param includeAntPaths paths where filter should hit, <code>null</code> or empty - all
     * @param excludeAntPaths paths (of the included ones) where filter should not hit, <code>null</code> or empty - none
     * @param filterProperties the DoS filter properties
     * @param api the per client IP limits of the API - not set ones fall back to the global ones
     * @param clientProperties the client properties (blacklist)
     * @return the filter registration
     */
    public static FilterRegistrationBean<DosFilter> dosFilter(
            final Collection<String> includeAntPaths,
            final Collection<String> excludeAntPaths,
            final HawkbitSecurityProperties.Dos.Filter filterProperties,
            final HawkbitSecurityProperties.Dos.Filter.Limit api,
            final HawkbitSecurityProperties.Clients clientProperties) {
        final FilterRegistrationBean<DosFilter> filterRegBean = new FilterRegistrationBean<>();
        filterRegBean.setFilter(new DosFilter(
                includeAntPaths, excludeAntPaths,
                new DosFilter.Limits(filterProperties.maxRead(api), filterProperties.maxWrite(api)),
                filterProperties.getMaxAuthFailures(),
                new DosFilter.LoadReport(filterProperties.getReportPercent(), filterProperties.getReportInterval()),
                filterProperties.getWhitelist(), clientProperties.getBlacklist()));
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
        final FilterRegistrationBean<DosFilter> filterRegBean = dosFilter(Collections.emptyList(), null,
                securityProperties.getDos().getFilter(), securityProperties.getDos().getFilter().getSystem(),
                securityProperties.getClients());
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

    // without a web server (e.g. mock web environment in tests) the remote IP header is read as is - nothing to warn about
    private static void logMode(
            final Mode mode, final ApplicationContext applicationContext, final HawkbitSecurityProperties securityProperties) {
        if (!(applicationContext instanceof WebServerApplicationContext)) {
            return;
        }
        final Environment environment = applicationContext.getEnvironment();
        final String remoteIpHeader = securityProperties.getClients().getRemoteIpHeader();
        if (mode == Mode.LEGACY) {
            log.warn("hawkBit doesn't process the forwarded headers ({} is {}, not {}). The client IP{} is not hardened by hawkBit - "
                            + "the deployment (e.g. the reverse proxy) is responsible for correct forwarded headers (e.g. setting / "
                            + "overwriting '{}')!",
                    FORWARD_HEADERS_STRATEGY, forwardHeadersStrategy(environment), NONE,
                    ObjectUtils.isEmpty(remoteIpHeader) ? "" : " (read from the '" + remoteIpHeader + "' header as is)",
                    HawkbitSecurityProperties.Clients.X_FORWARDED_FOR);
        } else {
            log.info("Client IP is resolved by hawkBit from the forwarded headers (trusted proxies only) with the {}",
                    mode == Mode.VALVE ? "Tomcat valve" : "servlet filter");
            // the Tomcat RemoteIpValve properties are not applied (the valve is active only with forward headers strategy NATIVE) - and not
            // used by hawkBit, e.g. left from a configuration for another strategy
            final List<String> tomcatRemoteIp = TOMCAT_REMOTE_IP_PROPERTIES.stream().filter(environment::containsProperty).toList();
            if (!tomcatRemoteIp.isEmpty()) {
                log.warn("{} are ignored! Configure the trusted proxies with hawkbit.server.security.forwarded.trusted-proxies and the client "
                        + "IP header with hawkbit.server.security.clients.remote-ip-header.", tomcatRemoteIp);
            }
        }
    }

    private static String forwardHeadersStrategy(final Environment environment) {
        // NONE is set by the hawkBit defaults (hawkbit-rest-defaults.properties)
        final String strategy = environment.getProperty(FORWARD_HEADERS_STRATEGY, NONE);
        return strategy.isBlank() ? NONE : strategy.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isEmbeddedTomcat(final ApplicationContext applicationContext) {
        final ClassLoader classLoader = SecurityManagedConfiguration.class.getClassLoader();
        // bean definitions are checked via the bean factory - it is called while the context is refreshing
        return applicationContext instanceof ConfigurableApplicationContext configurableContext
                && ClassUtils.isPresent(TOMCAT_SERVLET_WEB_SERVER_FACTORY, classLoader)
                && configurableContext.getBeanFactory().getBeanNamesForType(
                ClassUtils.resolveClassName(TOMCAT_SERVLET_WEB_SERVER_FACTORY, classLoader), true, false).length > 0;
    }

    /**
     * Embedded Tomcat - the {@link ForwardedValve} (only if the forward headers strategy is <code>NONE</code>).
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory")
    static class TomcatForwardedConfiguration {

        @Bean
        WebServerFactoryCustomizer<TomcatServletWebServerFactory> forwardedValveCustomizer(
                final ForwardedResolver forwardedResolver, final Environment environment) {
            return new ForwardedValve.Customizer(forwardedResolver, environment);
        }
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

    /**
     * Legacy mode (forward headers strategy not <code>NONE</code>) - hawkBit doesn't process the forwarded headers. Just stores the
     * {@link ClientAddress} as request attribute (so the consumers could always use {@link ClientAddress#of(ServletRequest)}), with the client IP
     * read as is from the remote IP header (as {@link IpUtil#findClientIpAddress(String)}), and the remote address as
     * peer. No forwarded for hops - not known.
     */
    public static class LegacyClientAddressFilter implements Filter {

        private final String remoteIpHeader;

        /**
         * @param remoteIpHeader the remote IP header the client IP is read from, <code>null</code> - the remote address is used
         */
        public LegacyClientAddressFilter(final String remoteIpHeader) {
            this.remoteIpHeader = remoteIpHeader;
        }

        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
                throws IOException, ServletException {
            if (request instanceof HttpServletRequest httpRequest) {
                final String header = ObjectUtils.isEmpty(remoteIpHeader) ? null : httpRequest.getHeader(remoteIpHeader);
                final String found = header == null ? null : IpUtil.findClientIpAddress(header);
                final String ip = found == null ? null : ForwardedResolver.ip(found);
                final String peer = request.getRemoteAddr();
                request.setAttribute(ClientAddress.ATTRIBUTE, new ClientAddress(peer, ip == null ? peer : ip, List.of(), null));
            }
            chain.doFilter(request, response);
        }
    }
}
