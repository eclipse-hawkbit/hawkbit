/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * hawkBit (behind a reverse proxy) is designed to run with embedded Tomcat, {@code server.forward-headers-strategy=NATIVE}
 * and {@code server.tomcat.use-relative-redirects=true} (the defaults):
 * <ul>
 *     <li>Tomcat's {@code RemoteIpValve} resolves the client IP (and scheme, host, port) from the forwarded headers only
 *     if the request comes from a trusted proxy ({@code server.tomcat.remoteip.internal-proxies / trusted-proxies}) and
 *     processes {@code X-Forwarded-For} right to left, so a client can't spoof its IP by sending the header by itself.
 *     Then hawkBit uses {@link jakarta.servlet.ServletRequest#getRemoteAddr()} as client IP (the configured
 *     {@link HawkbitSecurityProperties.Clients#getRemoteIpHeader() remote IP header} is cleared) - the valve has already
 *     consumed / rewritten the header</li>
 *     <li>redirects are relative, so they don't contain the request host</li>
 * </ul>
 * If an instance doesn't match (e.g. Jetty, which doesn't check for trusted proxies, or another forward headers strategy)
 * a warning is logged: the client IP is read from the configured remote IP header as is (as before) and the deployment
 * (e.g. the reverse proxy) is responsible for correct forwarded headers and redirects.
 * <p/>
 * The decision is done once, after binding of the {@link HawkbitSecurityProperties}, so all consumers (DoS filter, target
 * address tracking) see the effective remote IP header. In order to see the properties, the post processor is registered
 * first ({@link PriorityOrdered}). If they are still created before it (e.g. a dependency of another early post processor),
 * the startup fails - otherwise the remote IP header would be read as is, silently.
 */
@Slf4j
public class RemoteIpResolutionPostProcessor
        implements BeanPostProcessor, PriorityOrdered, SmartInitializingSingleton, ApplicationContextAware {

    static final String FORWARD_HEADERS_STRATEGY = "server.forward-headers-strategy";
    static final String TOMCAT_RELATIVE_REDIRECTS = "server.tomcat.use-relative-redirects";
    static final String TOMCAT_REMOTE_IP_HEADER = "server.tomcat.remoteip.remote-ip-header";
    /**
     * If <code>true</code>, the client IP is read from the configured remote IP header as is (the legacy behavior), even if Tomcat
     * resolves it (embedded Tomcat with forward headers strategy NATIVE). Just checked, not a {@link HawkbitSecurityProperties} property.
     *
     * @deprecated since 1.2.1 - a temporary solution to return back to the legacy client IP resolution. Will be removed.
     */
    @Deprecated(forRemoval = true, since = "1.2.1")
    @SuppressWarnings("java:S1133")
    static final String LEGACY_REMOTE_IP_RESOLUTION = "hawkbit.server.security.clients.legacy-remote-ip-resolution";
    private static final String TOMCAT_SERVLET_WEB_SERVER_FACTORY = "org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory";
    private static final String NATIVE = "NATIVE";
    private static final String NONE = "NONE";

    private ApplicationContext applicationContext;
    private boolean processed;

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) {
        // no web server (e.g. mock web environment in tests) - nothing to resolve by the container, the header is used
        if (bean instanceof HawkbitSecurityProperties securityProperties && applicationContext instanceof WebServerApplicationContext) {
            processed = true;
            final HawkbitSecurityProperties.Clients clients = securityProperties.getClients();
            final Environment environment = applicationContext.getEnvironment();
            final String strategy = forwardHeadersStrategy(environment);
            final boolean tomcat = isEmbeddedTomcat();
            final boolean legacy = environment.getProperty(LEGACY_REMOTE_IP_RESOLUTION, Boolean.class, false);

            final List<String> mismatches = new ArrayList<>();
            if (!tomcat) {
                mismatches.add("web server is not embedded Tomcat");
            }
            if (!NATIVE.equals(strategy)) {
                mismatches.add(FORWARD_HEADERS_STRATEGY + " is " + strategy);
            }
            if (tomcat && !environment.getProperty(TOMCAT_RELATIVE_REDIRECTS, Boolean.class, false)) {
                mismatches.add(TOMCAT_RELATIVE_REDIRECTS + " is not true");
            }
            if (legacy) {
                mismatches.add(LEGACY_REMOTE_IP_RESOLUTION + " (deprecated) is true");
            }

            if (tomcat && NATIVE.equals(strategy) && !legacy) {
                warnIfRemoteIpHeaderNotEffective(clients.getRemoteIpHeader(), environment);
                log.info("Client IP is resolved by Tomcat (forward headers strategy {})", strategy);
                clients.setRemoteIpHeader(null);
            }
            if (!mismatches.isEmpty()) {
                log.warn("hawkBit is designed to run with embedded Tomcat, {}={} and {}=true, but here: {}. The client IP{} "
                                + "and redirects are not hardened by hawkBit - the deployment (e.g. the reverse proxy) is responsible "
                                + "for correct forwarded headers (e.g. setting / overwriting '{}') and redirects!",
                        FORWARD_HEADERS_STRATEGY, NATIVE, TOMCAT_RELATIVE_REDIRECTS, String.join(", ", mismatches),
                        clients.getRemoteIpHeader() == null ? "" : " (read from the '" + clients.getRemoteIpHeader() + "' header as is)",
                        HawkbitSecurityProperties.Clients.X_FORWARDED_FOR);
            }
        }
        return bean;
    }

    // fail closed if an instantiated HawkbitSecurityProperties singleton bypassed the post processing. Not fixed here - the
    // consumers could have already taken the remote IP header (e.g. the DoS filters). Not yet created (lazy) ones are processed later
    @Override
    public void afterSingletonsInstantiated() {
        if (!processed && applicationContext instanceof WebServerApplicationContext
                && applicationContext instanceof ConfigurableApplicationContext configurableContext) {
            final ConfigurableListableBeanFactory beanFactory = configurableContext.getBeanFactory();
            for (final String name : beanFactory.getBeanNamesForType(HawkbitSecurityProperties.class, false, false)) {
                if (beanFactory.containsSingleton(name)) {
                    throw new IllegalStateException(String.format(
                            "%s bean '%s' is created before %s is registered (e.g. a dependency of another bean post processor), "
                                    + "so the client IP resolution can't be decided!",
                            HawkbitSecurityProperties.class.getSimpleName(), name, getClass().getSimpleName()));
                }
            }
        }
    }

    // the configured remote IP header is not used by hawkBit when Tomcat resolves the client IP - warn if it differs from
    // the header Tomcat actually uses, since the configuration doesn't work as (probably) expected
    private static void warnIfRemoteIpHeaderNotEffective(final String remoteIpHeader, final Environment environment) {
        // Tomcat's RemoteIpValve uses X-Forwarded-For if not configured explicitly
        final String tomcatRemoteIpHeader = environment.getProperty(TOMCAT_REMOTE_IP_HEADER, HawkbitSecurityProperties.Clients.X_FORWARDED_FOR);
        if (!ObjectUtils.isEmpty(remoteIpHeader) && !remoteIpHeader.equalsIgnoreCase(tomcatRemoteIpHeader)) {
            log.warn("Remote IP header '{}' is ignored, Tomcat uses '{}'. Configure '{}={}' instead!",
                    remoteIpHeader, tomcatRemoteIpHeader, TOMCAT_REMOTE_IP_HEADER, remoteIpHeader);
        }
    }

    private static String forwardHeadersStrategy(final Environment environment) {
        final String strategy = environment.getProperty(FORWARD_HEADERS_STRATEGY);
        if (ObjectUtils.isEmpty(strategy)) {
            // same deduction as Spring Boot does - on some cloud platforms (e.g. Kubernetes) NATIVE is the default
            final CloudPlatform platform = CloudPlatform.getActive(environment);
            return platform != null && platform.isUsingForwardHeaders() ? NATIVE : NONE;
        }
        return strategy.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isEmbeddedTomcat() {
        final ClassLoader classLoader = getClass().getClassLoader();
        // bean definitions are checked via the bean factory - it is called while the context is refreshing
        return applicationContext instanceof ConfigurableApplicationContext configurableContext
                && ClassUtils.isPresent(TOMCAT_SERVLET_WEB_SERVER_FACTORY, classLoader)
                && configurableContext.getBeanFactory().getBeanNamesForType(
                ClassUtils.resolveClassName(TOMCAT_SERVLET_WEB_SERVER_FACTORY, classLoader), true, false).length > 0;
    }
}
