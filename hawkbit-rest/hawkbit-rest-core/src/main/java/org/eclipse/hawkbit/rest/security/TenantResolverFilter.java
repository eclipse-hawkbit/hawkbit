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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.context.Mdc;
import org.eclipse.hawkbit.context.RequestedTenant;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationConverter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.util.UrlPathHelper;

/**
 * Resolves the {@link RequestedTenant} of an API (e.g. Management API) request. It shall be registered before the Spring Security filter
 * chain, so the requested tenant is available for the authentication.
 * <ul>
 *     <li><code>/{tenant}/{api path}/**</code> (if enabled) - sets the tenant as requested tenant and serves the request as
 *     <code>/{api path}/**</code>: the request is wrapped so that <code>/{tenant}</code> becomes part of its context path (as a
 *     <code>X-Forwarded-Prefix</code> would). So the security filter chain and the handlers see <code>/{api path}/**</code>, while the
 *     links built from the request (e.g. HATEOAS) keep the tenant path. If the request has a basic auth user prefixed with the same tenant
 *     (<code>tenant&#92;user</code>), the prefix is removed, otherwise the backslash is assumed to be part of the username.</li>
 *     <li><code>/{api path}/**</code> with basic auth user <code>tenant&#92;user</code> (if enabled) - the part before the first
 *     backslash is the requested tenant, the request continues with <code>user</code> as basic auth user.</li>
 * </ul>
 * An invalid tenant is rejected directly with the given authentication entry point (as for invalid credentials, 401 Unauthorized). The
 * filter runs before the Spring Security filter chain, so an {@link AuthenticationException} thrown here wouldn't be translated.
 * <p/>
 * The tenant prefixed basic auth user (<code>tenant&#92;user</code>) is deprecated since 1.2.1 (a warning is logged on its first use) -
 * the tenant path <code>/{tenant}/{api path}/**</code> shall be used to request a tenant.
 * While the requested tenant is set it is also put in the MDC as tenant (until replaced by the authenticated one), so the log
 * messages before / of the authentication contain the requested tenant.
 */
@Slf4j
public class TenantResolverFilter extends OncePerRequestFilter {

    /**
     * The DDI API path (after the tenant): <code>/{tenant}/controller/**</code>.
     */
    public static final String DDI_PATH = "/controller";

    private static final BasicAuthenticationConverter BASIC_AUTHENTICATION_CONVERTER = new BasicAuthenticationConverter();

    private final String apiPath;
    private final String apiPathAndSlash;
    private final boolean tenantPathEnabled;
    private final boolean tenantPrefixedBasicAuthEnabled;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    // the deprecated tenant prefixed basic auth user is logged once - on its first use (not per request, would flood the log)
    private final AtomicBoolean tenantPrefixedUserWarned = new AtomicBoolean();

    /**
     * Constructor.
     *
     * @param apiPath the api path, e.g. <code>/rest</code>
     * @param tenantPathEnabled if to resolve the requested tenant from the <code>/{tenant}/{api path}/**</code> path
     * @param tenantPrefixedBasicAuthEnabled if to resolve the requested tenant from <code>tenant&#92;user</code> basic auth user on
     *         <code>/{api path}/**</code>
     * @param authenticationEntryPoint the authentication entry point rejecting the requests with an invalid tenant - the same as of the
     *         API security filter chain, so they are answered as with invalid credentials
     */
    public TenantResolverFilter(
            final String apiPath, final boolean tenantPathEnabled, final boolean tenantPrefixedBasicAuthEnabled,
            final AuthenticationEntryPoint authenticationEntryPoint) {
        this.apiPath = apiPath;
        apiPathAndSlash = apiPath + "/";
        this.tenantPathEnabled = tenantPathEnabled;
        this.tenantPrefixedBasicAuthEnabled = tenantPrefixedBasicAuthEnabled;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    // the async dispatch is to the original (not wrapped) request, so it has to be wrapped again - otherwise it would be served as
    // /{tenant}/{api path}/**
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request, @NonNull final HttpServletResponse response, @NonNull final FilterChain filterChain)
            throws ServletException, IOException {
        // the tenant path is matched against the raw (not decoded / normalized) request URI - the tenant path becomes part of the
        // context path, which has to be a prefix of the raw request URI (e.g. /%44EFAULT/rest/** is an invalid tenant, not DEFAULT).
        // Not normalized paths (e.g. /{tenant}/rest/../x) are still matched here, but they are rejected by the Spring Security
        // firewall which checks the same (not changed) request URI
        final String pathTenant = tenantPathEnabled ? tenant(request, apiPath) : null;
        if (pathTenant != null) {
            if (!RequestedTenant.isValidTenant(pathTenant)) {
                rejectInvalidTenant(request, response, "Invalid tenant in the request path");
                return;
            }

            final HttpServletRequest tenantPathRequest = tenantPathRequest(request, pathTenant);
            // remove the tenant prefix of the basic auth user only if it matches the path tenant (case-insensitive as tenants are).
            // otherwise, assume that the backslash is part of the username
            final HttpServletRequest apiRequest = TenantPrefixedBasicAuthUser.of(request)
                    .filter(tenantPrefixedBasicAuthUser -> tenantPrefixedBasicAuthUser.tenant().equalsIgnoreCase(pathTenant))
                    .map(tenantPrefixedBasicAuthUser -> {
                        warnTenantPrefixedUserDeprecated();
                        return tenantPrefixedBasicAuthUser.withNonTenantPrefixedUser(tenantPathRequest);
                    })
                    .orElse(tenantPathRequest);
            withRequestedTenant(request, pathTenant, () -> filterChain.doFilter(apiRequest, response));
            return;
        } else if (tenantPrefixedBasicAuthEnabled
                // the decoded path - as the servlet container, the DoS filter and the handler mappings match it (e.g. /r%65st/**)
                && UrlPathHelper.defaultInstance.getPathWithinApplication(request).startsWith(apiPathAndSlash)) {
            final TenantPrefixedBasicAuthUser tenantPrefixedBasicAuthUser = TenantPrefixedBasicAuthUser.of(request).orElse(null);
            if (tenantPrefixedBasicAuthUser != null) {
                final String tenant = tenantPrefixedBasicAuthUser.tenant();
                warnTenantPrefixedUserDeprecated();
                if (!RequestedTenant.isValidTenant(tenant)) {
                    // while the tenant prefixed basic auth is enabled, the part before the backslash is always the tenant
                    rejectInvalidTenant(request, response, "Invalid tenant in the basic auth user");
                    return;
                }

                withRequestedTenant(request, tenant, () ->
                        filterChain.doFilter(tenantPrefixedBasicAuthUser.withNonTenantPrefixedUser(request), response));
                return; // processed with the tenant set and the user without tenant prefix
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Returns the tenant of a tenant scoped API request <code>/{tenant}/{api path}/**</code> (after the context path) - matched against the
     * raw (not decoded / normalized) request URI, as the filter does it.
     *
     * @param request the request
     * @param apiPath the api path, e.g. <code>/rest</code> or <code>/controller</code> (DDI)
     * @return the (raw, not validated) tenant path segment, <code>null</code> if the request path doesn't match
     */
    public static String tenant(final HttpServletRequest request, final String apiPath) {
        final String rawPath = request.getRequestURI().substring(request.getContextPath().length());
        final int tenantEnd = rawPath.indexOf('/', 1);
        return rawPath.startsWith("/") && tenantEnd > 1 && rawPath.startsWith(apiPath + "/", tenantEnd)
                ? rawPath.substring(1, tenantEnd)
                : null;
    }

    private void rejectInvalidTenant(final HttpServletRequest request, final HttpServletResponse response, final String message)
            throws ServletException, IOException {
        log.debug(message);
        authenticationEntryPoint.commence(request, response, new BadCredentialsException(message));
    }

    private void warnTenantPrefixedUserDeprecated() {
        if (tenantPrefixedUserWarned.compareAndSet(false, true)) {
            log.warn("Tenant prefixed basic auth user (tenant\\user) is deprecated since 1.2.1 and will be removed! " +
                    "Use the tenant path /{tenant}{}/** to request a tenant.", apiPath);
        }
    }

    // serves /{tenant}/{api path}/** as /{api path}/** - the tenant path segment becomes part of the context path (the request URI and
    // URL are not changed). Spring MVC, Spring Security matchers and the link builders take the path within the application as
    // request URI minus context path, so they see /{api path}/** while the built links keep /{tenant}
    private static HttpServletRequest tenantPathRequest(final HttpServletRequest request, final String tenant) {
        // the paths parsed (and cached in the request attributes) from the not wrapped request shall not be reused
        ServletRequestPathUtils.clearParsedRequestPath(request);
        request.removeAttribute(UrlPathHelper.PATH_ATTRIBUTE);

        final String tenantPath = "/" + tenant;
        final String contextPath = request.getContextPath() + tenantPath;
        return new HttpServletRequestWrapper(request) {

            @Override
            public String getContextPath() {
                return contextPath;
            }

            // the servlet path (for a default, "/", servlet mapping) is the path within the application - and shall be consistent
            // with the context path
            @Override
            public String getServletPath() {
                final String servletPath = super.getServletPath();
                return servletPath.startsWith(tenantPath + "/") ? servletPath.substring(tenantPath.length()) : servletPath;
            }
        };
    }

    private static void withRequestedTenant(final HttpServletRequest request, final String tenant, final ServletAction action)
            throws ServletException, IOException {
        // the request attribute keeps the requested tenant available after the filter (e.g. for ERROR / ASYNC dispatches)
        request.setAttribute(RequestedTenant.REQUEST_ATTRIBUTE, tenant);
        try {
            // MDC tenant is upper case (as normalized by the Principal) - so it doesn't change after successful authentication
            RequestedTenant.withTenant(tenant, () -> Mdc.asTenantAsActor(tenant.toUpperCase(Locale.ROOT), null, () -> {
                action.run();
                return null;
            }));
        } catch (final ServletException | IOException | RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new ServletException(e);
        }
    }

    @FunctionalInterface
    private interface ServletAction {

        void run() throws ServletException, IOException;
    }

    private record TenantPrefixedBasicAuthUser(String tenant, String nonTenantPrefixedAuthorization) {

        // parses the basic auth header as Spring's BasicAuthenticationFilter does
        private static Optional<TenantPrefixedBasicAuthUser> of(final HttpServletRequest request) {
            final UsernamePasswordAuthenticationToken token;
            try {
                token = BASIC_AUTHENTICATION_CONVERTER.convert(request);
            } catch (final AuthenticationException e) {
                return Optional.empty(); // invalid basic auth - let the authentication reject it
            }
            if (token == null) {
                return Optional.empty(); // not basic auth
            }

            final String username = token.getName();
            final int backslashIndex = username.indexOf('\\');
            if (backslashIndex <= 0 || backslashIndex == username.length() - 1) {
                return Optional.empty(); // no tenant prefix (or empty tenant / user)
            }

            final String userAndPassword = username.substring(backslashIndex + 1) + ":" + token.getCredentials();
            return Optional.of(new TenantPrefixedBasicAuthUser(
                    username.substring(0, backslashIndex),
                    "Basic " + Base64.getEncoder().encodeToString(userAndPassword.getBytes(StandardCharsets.UTF_8))));
        }

        private HttpServletRequest withNonTenantPrefixedUser(final HttpServletRequest request) {
            return new HttpServletRequestWrapper(request) {

                @Override
                public String getHeader(final String name) {
                    return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name) ? nonTenantPrefixedAuthorization : super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(final String name) {
                    return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)
                            ? Collections.enumeration(Collections.singletonList(nonTenantPrefixedAuthorization))
                            : super.getHeaders(name);
                }
            };
        }
    }
}
