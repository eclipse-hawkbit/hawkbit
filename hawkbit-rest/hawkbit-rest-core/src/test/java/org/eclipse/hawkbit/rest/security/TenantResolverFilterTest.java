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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.hawkbit.context.Mdc;
import org.eclipse.hawkbit.context.RequestedTenant;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.web.util.UrlPathHelper;

/**
 * Feature: Unit Tests - Security<br/>
 * Story: Requested tenant filter for /{tenant}/rest/** and tenant prefixed basic auth user
 */
class TenantResolverFilterTest {

    private static final String TENANT = "my_tenant-1";
    private static final BasicAuthenticationEntryPoint ENTRY_POINT = new BasicAuthenticationEntryPoint();
    private static final TenantResolverFilter FILTER = new TenantResolverFilter("/rest", true, true, ENTRY_POINT);

    static {
        ENTRY_POINT.setRealmName("hawkBit");
    }

    /**
     * /{tenant}/rest/** is served as /rest/** - the tenant path becomes part of the context path. The requested tenant is set (thread
     * local, attribute and MDC) during the chain.
     */
    @Test
    void tenantPathIsServedAsApiPath() throws Exception {
        final MockHttpServletRequest request = request("", "/" + TENANT + "/rest/v1/targets");
        final RecordingChain chain = new RecordingChain();

        FILTER.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.contextPath).isEqualTo("/" + TENANT);
        assertThat(chain.servletPath).isEqualTo("/rest/v1/targets");
        assertThat(chain.requestUri).isEqualTo("/" + TENANT + "/rest/v1/targets");
        assertThat(new UrlPathHelper().getPathWithinApplication((HttpServletRequest) chain.request)).isEqualTo("/rest/v1/targets");
        assertThat(chain.tenant).isEqualTo(TENANT);
        assertThat(chain.mdcTenant).isEqualTo(TENANT.toUpperCase());
        // after the filter the thread local is cleared, but the request attribute is kept
        assertThat(RequestedTenant.getTenant()).isNull();
        assertThat(request.getAttribute(RequestedTenant.REQUEST_ATTRIBUTE)).isEqualTo(TENANT);
        assertThat(MDC.get(Mdc.MDC_KEY_TENANT)).isNull();
    }

    /**
     * The tenant path is appended to the context path.
     */
    @Test
    void tenantPathWithContextPath() throws Exception {
        final RecordingChain chain = new RecordingChain();

        FILTER.doFilter(request("/ctx", "/" + TENANT + "/rest/v1/targets"), new MockHttpServletResponse(), chain);

        assertThat(chain.contextPath).isEqualTo("/ctx/" + TENANT);
        assertThat(chain.servletPath).isEqualTo("/rest/v1/targets");
        assertThat(chain.tenant).isEqualTo(TENANT);
    }

    /**
     * The async dispatch is to the original (not wrapped) request, so it is served as /rest/** again.
     */
    @Test
    void tenantPathAsyncDispatch() throws Exception {
        final MockHttpServletRequest request = request("", "/" + TENANT + "/rest/v1/targets");
        request.setDispatcherType(DispatcherType.ASYNC);
        final RecordingChain chain = new RecordingChain();

        FILTER.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.contextPath).isEqualTo("/" + TENANT);
        assertThat(chain.tenant).isEqualTo(TENANT);
    }

    /**
     * On tenant path the basic auth user prefix matching (case-insensitive) the path tenant is removed.
     */
    @Test
    void tenantPathStripsMatchingBasicAuthPrefix() throws Exception {
        final MockHttpServletRequest request = request("", "/" + TENANT + "/rest/v1/targets");
        request.addHeader(HttpHeaders.AUTHORIZATION, basicAuth(TENANT.toUpperCase() + "\\user", "pass"));
        final RecordingChain chain = new RecordingChain();

        FILTER.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.authorization).isEqualTo(basicAuth("user", "pass"));
        assertThat(Collections.list(((HttpServletRequest) chain.request).getHeaders(HttpHeaders.AUTHORIZATION)))
                .containsExactly(basicAuth("user", "pass"));
        assertThat(chain.contextPath).isEqualTo("/" + TENANT);
    }

    /**
     * On tenant path the basic auth user prefix not matching the path tenant is kept (assumed part of the username).
     */
    @Test
    void tenantPathKeepsMismatchingBasicAuthPrefix() throws Exception {
        final MockHttpServletRequest request = request("", "/" + TENANT + "/rest/v1/targets");
        final String authorization = basicAuth("corp.example\\user", "pass");
        request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        final RecordingChain chain = new RecordingChain();

        FILTER.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.tenant).isEqualTo(TENANT);
        assertThat(chain.authorization).isEqualTo(authorization);
    }

    /**
     * Invalid tenant in the path is rejected with the authentication entry point (as invalid credentials).
     */
    @Test
    void invalidTenantPathIsRejected() throws Exception {
        assertRejected(request("", "/inv%41lid/rest/v1/targets"));
    }

    /**
     * The tenant path handling could be disabled.
     */
    @Test
    void tenantPathDisabled() throws Exception {
        final MockHttpServletRequest request = request("", "/" + TENANT + "/rest/v1/targets");
        final RecordingChain chain = new RecordingChain();

        new TenantResolverFilter("/rest", false, true, ENTRY_POINT).doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.request).isSameAs(request);
        assertThat(chain.tenant).isNull();
    }

    /**
     * On /rest/** a tenant prefixed basic auth user sets the requested tenant and continues with the non-prefixed user.
     */
    @Test
    void restWithTenantPrefixedBasicAuth() throws Exception {
        final MockHttpServletRequest request = request("", "/rest/v1/targets");
        request.addHeader(HttpHeaders.AUTHORIZATION, basicAuth(TENANT + "\\user", "pa\\ss:word"));
        final RecordingChain chain = new RecordingChain();

        FILTER.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.contextPath).isEmpty();
        assertThat(chain.tenant).isEqualTo(TENANT);
        assertThat(chain.authorization).isEqualTo(basicAuth("user", "pa\\ss:word"));
        assertThat(request.getAttribute(RequestedTenant.REQUEST_ATTRIBUTE)).isEqualTo(TENANT);
    }

    /**
     * On /rest/** the tenant prefixed basic auth user is matched on the decoded path (as served), while the tenant path is matched on
     * the raw request URI (the tenant path becomes part of the context path which has to be a prefix of it).
     */
    @Test
    void percentEncodedPaths() throws Exception {
        final MockHttpServletRequest restRequest = request("", "/r%65st/v1/targets");
        restRequest.addHeader(HttpHeaders.AUTHORIZATION, basicAuth(TENANT + "\\user", "pass"));
        final RecordingChain restChain = new RecordingChain();
        FILTER.doFilter(restRequest, new MockHttpServletResponse(), restChain);
        assertThat(restChain.tenant).isEqualTo(TENANT);
        assertThat(restChain.authorization).isEqualTo(basicAuth("user", "pass"));

        assertRejected(request("", "/%6Dy_tenant-1/rest/v1/targets"));
    }

    /**
     * On /rest/** the tenant prefixed basic auth user handling could be disabled.
     */
    @Test
    void restWithTenantPrefixedBasicAuthDisabled() throws Exception {
        final MockHttpServletRequest request = request("", "/rest/v1/targets");
        final String authorization = basicAuth(TENANT + "\\user", "pass");
        request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        final RecordingChain chain = new RecordingChain();

        new TenantResolverFilter("/rest", true, false, ENTRY_POINT).doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(chain.tenant).isNull();
        assertThat(chain.authorization).isEqualTo(authorization);
        assertThat(request.getAttribute(RequestedTenant.REQUEST_ATTRIBUTE)).isNull();
    }

    /**
     * On /rest/** not tenant prefixed (or not basic) auth and other paths are not touched.
     */
    @Test
    void notTouched() throws Exception {
        for (final String authorization : new String[] {
                null, basicAuth("user", "pass"), basicAuth("user", "pa\\ss"), basicAuth("\\user", "pass"), basicAuth("tenant\\", "pass"),
                "Basic not-base64!", "Bearer " + TENANT + "\\user" }) {
            final MockHttpServletRequest request = request("", "/rest/v1/targets");
            if (authorization != null) {
                request.addHeader(HttpHeaders.AUTHORIZATION, authorization);
            }
            final RecordingChain chain = new RecordingChain();

            FILTER.doFilter(request, new MockHttpServletResponse(), chain);

            assertThat(chain.request).as(authorization).isSameAs(request);
            assertThat(chain.tenant).as(authorization).isNull();
        }

        final MockHttpServletRequest request = request("", "/" + TENANT + "/controller/v1/device");
        request.addHeader(HttpHeaders.AUTHORIZATION, basicAuth(TENANT + "\\user", "pass"));
        final RecordingChain chain = new RecordingChain();
        FILTER.doFilter(request, new MockHttpServletResponse(), chain);
        assertThat(chain.request).isSameAs(request);
        assertThat(chain.tenant).isNull();
    }

    /**
     * On /rest/** the part of the basic auth user before the first backslash is always the tenant - an invalid one is rejected with the
     * authentication entry point (as invalid credentials).
     */
    @Test
    void invalidTenantPrefixIsRejected() throws Exception {
        final MockHttpServletRequest request = request("", "/rest/v1/targets");
        request.addHeader(HttpHeaders.AUTHORIZATION, basicAuth("corp/example\\user", "pass"));
        assertRejected(request);
    }

    private static void assertRejected(final MockHttpServletRequest request) throws Exception {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final RecordingChain chain = new RecordingChain();

        FILTER.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).startsWith("Basic realm=\"hawkBit\"");
        assertThat(chain.request).isNull();
        assertThat(request.getAttribute(RequestedTenant.REQUEST_ATTRIBUTE)).isNull();
    }

    private static MockHttpServletRequest request(final String contextPath, final String path) {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", contextPath + path);
        request.setContextPath(contextPath);
        request.setServletPath(path); // as for the default ("/") servlet mapping
        return request;
    }

    private static String basicAuth(final String user, final String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    private static class RecordingChain extends MockFilterChain {

        private ServletRequest request;
        private String tenant;
        private String mdcTenant;
        private String authorization;
        private String contextPath;
        private String servletPath;
        private String requestUri;

        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response) {
            this.request = request;
            tenant = RequestedTenant.getTenant();
            mdcTenant = MDC.get(Mdc.MDC_KEY_TENANT);
            final HttpServletRequest httpRequest = (HttpServletRequest) request;
            authorization = httpRequest.getHeader(HttpHeaders.AUTHORIZATION);
            contextPath = httpRequest.getContextPath();
            servletPath = httpRequest.getServletPath();
            requestUri = httpRequest.getRequestURI();
        }
    }

    /**
     * Verifies that the tenant of a tenant scoped API path is the raw first path segment of /{tenant}/{api path}/** (after the context path)
     */
    @Test
    void tenantOfPath() {
        assertThat(TenantResolverFilter.tenant(tenantPathRequest("", "/TENANT/controller/v1/dev1"), "/controller")).isEqualTo("TENANT");
        assertThat(TenantResolverFilter.tenant(tenantPathRequest("", "/acme.com/rest/v1/targets"), "/rest")).isEqualTo("acme.com");
        assertThat(TenantResolverFilter.tenant(tenantPathRequest("/hawkbit", "/hawkbit/TENANT/controller/v1/dev1"), "/controller"))
                .isEqualTo("TENANT");
        // raw - not decoded
        assertThat(TenantResolverFilter.tenant(tenantPathRequest("", "/%44EFAULT/rest/v1/targets"), "/rest")).isEqualTo("%44EFAULT");

        // not matching
        assertThat(TenantResolverFilter.tenant(tenantPathRequest("", "/rest/v1/targets"), "/rest")).isNull();
        assertThat(TenantResolverFilter.tenant(tenantPathRequest("", "/TENANT/rest"), "/rest")).isNull();
        assertThat(TenantResolverFilter.tenant(tenantPathRequest("", "//rest/v1"), "/rest")).isNull();
        assertThat(TenantResolverFilter.tenant(tenantPathRequest("", "/TENANT/restx/v1"), "/rest")).isNull();
        assertThat(TenantResolverFilter.tenant(tenantPathRequest("", "/TENANT/controller/v1/dev1"), "/rest")).isNull();
    }

    private static MockHttpServletRequest tenantPathRequest(final String contextPath, final String uri) {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setContextPath(contextPath);
        return request;
    }
}
