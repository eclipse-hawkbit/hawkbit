/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.context;

import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Holds the tenant requested by the client before (and independent of) the authentication - e.g. via the Management API tenant path
 * (<code>/{tenant}/rest/**</code>) or the <code>tenant&#92;user</code> basic auth user. It is set by the request processing (servlet filter)
 * and could be used by the authentication (e.g. to authenticate the user in the scope of the requested tenant) or for logging.
 * <p/>
 * The requested tenant is stored in a thread local (while the request filter chain is processed) and additionally, by the filter, in the
 * request attribute {@link #REQUEST_ATTRIBUTE}. The request attribute is used as a fallback when the thread local is not set anymore
 * (e.g. on ERROR or ASYNC dispatches which are processed after the filter, or in another thread).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestedTenant {

    /**
     * Name of the request attribute holding the requested tenant
     */
    public static final String REQUEST_ATTRIBUTE = RequestedTenant.class.getName();

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    /**
     * The tenant must be a non-empty string of URI unreserved chars (RFC 3986: 0-9, A-Z, a-z, -, ., _, ~), but not a dot segment
     * (. or ..). So it could be used, not encoded, as a path segment (<code>/{tenant}/rest/**</code>) without restricting the existing
     * tenants (e.g. <code>acme.com</code>) more than needed. The dot segments are excluded as they are normalized in paths.
     */
    private static final Pattern TENANT_REGEX_PATTERN = Pattern.compile("^(?!\\.{1,2}$)[a-zA-Z0-9._~-]+$");

    /**
     * Returns the requested tenant - from the thread local or, if not set, from the current request attribute.
     *
     * @return the requested tenant or <code>null</code> if there is no requested tenant
     */
    // not validated here - the thread local is validated by withTenant and the request attribute is set (validated) by the filter only
    public static String getTenant() {
        final String tenant = TENANT.get();
        if (tenant != null) {
            return tenant;
        }
        final RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        return requestAttributes != null
                && requestAttributes.getAttribute(REQUEST_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST) instanceof String attributeTenant
                ? attributeTenant
                : null;
    }

    /**
     * Checks if the tenant matches the allowed tenant format.
     *
     * @param tenant the tenant
     * @return <code>true</code> if the tenant is valid, <code>false</code> otherwise
     */
    public static boolean isValidTenant(final String tenant) {
        return tenant != null && TENANT_REGEX_PATTERN.matcher(tenant).matches();
    }

    /**
     * Calls the callable with the requested tenant set in the thread local. After the call the previous requested tenant (if any) is
     * restored.
     *
     * @param tenant the requested tenant
     * @param callable the callable to call
     * @param <T> the result type
     * @return the result of the callable
     * @throws AuthenticationCredentialsNotFoundException if the tenant is <code>null</code>, empty or invalid
     * @throws Exception if thrown by the callable
     */
    // java:S112 - it is generic method so a generic exception is fine
    @SuppressWarnings("java:S112")
    public static <T> T withTenant(final String tenant, final Callable<T> callable) throws Exception {
        if (!isValidTenant(tenant)) {
            throw new AuthenticationCredentialsNotFoundException("Requested tenant must not be null, empty or invalid!");
        }

        final String previous = TENANT.get();
        TENANT.set(tenant);
        try {
            return callable.call();
        } finally {
            if (previous == null) {
                TENANT.remove();
            } else {
                TENANT.set(previous);
            }
        }
    }
}
