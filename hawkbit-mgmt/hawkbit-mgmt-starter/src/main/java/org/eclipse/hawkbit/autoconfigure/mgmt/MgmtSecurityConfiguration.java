/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.mgmt;

import static org.eclipse.hawkbit.context.AccessContext.asSystem;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.servlet.Filter;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.context.Mdc;
import org.eclipse.hawkbit.context.Principal;
import org.eclipse.hawkbit.context.RequestedTenant;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.oidc.OidcProperties;
import org.eclipse.hawkbit.oidc.OidcProperties.Oauth2.ResourceServer.Jwt.Claim;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.exception.TenantSoftDeletedException;
import org.eclipse.hawkbit.rest.SecurityManagedConfiguration;
import org.eclipse.hawkbit.rest.security.DosFilter;
import org.eclipse.hawkbit.rest.security.TenantResolverFilter;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;

/**
 * Security configuration for the REST management API.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({ HawkbitSecurityProperties.class, OidcProperties.class })
@EnableWebSecurity
public class MgmtSecurityConfiguration {

    private final HawkbitSecurityProperties securityProperties;
    private final OidcProperties oidcProperties;

    public MgmtSecurityConfiguration(final HawkbitSecurityProperties securityProperties, final OidcProperties oidcProperties) {
        this.securityProperties = securityProperties;
        this.oidcProperties = oidcProperties;
    }

    /**
     * Filter to protect the hawkBit server Management interface against too many requests. A servlet filter, so it runs before the
     * authentication: the basic / bearer authentication rejects invalid credentials directly (doesn't continue the chain), so a filter
     * after the authentication wouldn't throttle (and blacklist) them. Hence, the requests are counted per client IP.
     *
     * @return the spring filter registration bean for registering a denial of service protection filter in the filter chain
     */
    @Bean
    @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.filter", name = "enabled", matchIfMissing = true)
    public FilterRegistrationBean<DosFilter> dosFilterREST() {
        // registered for all paths and restricted by the include paths, since a servlet url pattern can't match /*/rest/* (the
        // tenant path, see tenantResolverFilterREST). Runs before the tenant resolution, so it matches the original (decoded) path
        final List<String> includePaths = securityProperties.getTenantResolver().isTenantPath()
                ? List.of(MgmtRestConstants.REST + "/**", "/*" + MgmtRestConstants.REST + "/**")
                : List.of(MgmtRestConstants.REST + "/**");
        final FilterRegistrationBean<DosFilter> filterRegBean = SecurityManagedConfiguration.dosFilter(includePaths, null,
                securityProperties.getDos().getFilter(), securityProperties.getDos().getFilter().getMgmt(),
                securityProperties.getClients());
        filterRegBean.setOrder(SecurityManagedConfiguration.DOS_FILTER_ORDER);
        filterRegBean.setName("dosMgmtFilter");
        return filterRegBean;
    }

    /**
     * Filter resolving the {@link RequestedTenant} of the Management API requests (see {@link TenantResolverFilter}): serves
     * <code>/{tenant}/rest/**</code> as <code>/rest/**</code> and handles <code>tenant&#92;user</code> basic auth users. The requested
     * tenant is checked against the authenticated principal in the security filter chain (see {@link #tenantAuthenticationFilter}).
     * <p/>
     * Registered for all paths since a servlet url pattern can't match <code>/*&#47;rest/*</code>. Shall be after Spring's
     * RequestContextFilter (so the requested tenant is set in the current request attributes) and before the Spring Security
     * filter chain (so the requested tenant is available for the authentication). Requests with an invalid tenant are rejected with the
     * same authentication entry point as the security filter chain uses for invalid credentials.
     *
     * @return the spring filter registration bean for registering the requested tenant filter
     */
    @Bean
    @SuppressWarnings("removal") // the tenant prefixed basic auth is deprecated, but still supported
    public FilterRegistrationBean<TenantResolverFilter> tenantResolverFilterREST(
            final SecurityFilterProperties securityFilterProperties,
            @Autowired(required = false) @Qualifier("hawkbitOAuth2ResourceServerCustomizer") final Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> oauth2ResourceServerCustomizer) {
        final FilterRegistrationBean<TenantResolverFilter> filterRegBean = new FilterRegistrationBean<>(new TenantResolverFilter(
                MgmtRestConstants.REST,
                securityProperties.getTenantResolver().isTenantPath(),
                securityProperties.getTenantResolver().isTenantPrefixedBasicAuth(),
                authenticationEntryPoint(oauth2ResourceServerCustomizer != null)));
        filterRegBean.setOrder(securityFilterProperties.getOrder() - 2);
        filterRegBean.setName("tenantResolverMgmtFilter");
        return filterRegBean;
    }

    @Bean(name = "hawkbitOAuth2ResourceServerCustomizer")
    @ConditionalOnProperty(prefix = "hawkbit.server.security.oauth2.resourceserver", name = "enabled")
    @ConditionalOnMissingBean(name = "hawkbitOAuth2ResourceServerCustomizer")
    Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> defaultOAuth2ResourceServerCustomizer() {
        return new DefaultOAuth2ResourceServerCustomizer();
    }

    @Bean
    @Order(350)
    SecurityFilterChain filterChainREST(
            final HttpSecurity http,
            @Autowired(required = false) @Qualifier("hawkbitOAuth2ResourceServerCustomizer") final Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> oauth2ResourceServerCustomizer,
            // called just before build of the SecurityFilterChain.
            // could be used for instance to set authentication provider
            // Note: implementation of the customizer shall always take in account what is the already set by the hawkBit
            @Autowired(required = false) @Qualifier("hawkbitHttpSecurityCustomizer") final Customizer<HttpSecurity> httpSecurityCustomizer,
            final SystemManagement systemManagement) {
        http
                .securityMatcher(MgmtRestConstants.REST + "/**")
                .authorizeHttpRequests(amrmRegistry -> amrmRegistry
                        .anyRequest()
                        .authenticated())
                .anonymous(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                // after the ExceptionTranslationFilter - so the AuthenticationExceptions it throws stop the chain and are answered by the
                // authentication entry point (401 with WWW-Authenticate, as for invalid credentials)
                .addFilterAfter(tenantAuthenticationFilter(systemManagement), ExceptionTranslationFilter.class)
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(Customizer.withDefaults())
                .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (securityProperties.getCors().isEnabled()) {
            http.cors(configurer -> configurer.configurationSource(securityProperties.getCors().toCorsConfigurationSource()));
        }

        if (securityProperties.isRequireSsl()) {
            http.redirectToHttps(Customizer.withDefaults());
        }

        if (oauth2ResourceServerCustomizer != null) {
            http.oauth2ResourceServer(oauth2ResourceServerCustomizer);
        }
        if (isBasicAuthEnabled(oauth2ResourceServerCustomizer != null)) {
            http.httpBasic(configurer -> configurer.authenticationEntryPoint(basicAuthenticationEntryPoint()));
        }

        if (httpSecurityCustomizer != null) {
            httpSecurityCustomizer.customize(http);
        }

        Mdc.Filter.addMdcFilter(http);

        return http.build();
    }

    // the authentication entry point for rejecting requests before the security filter chain (e.g. with an invalid requested tenant, see
    // TenantResolverFilter) as the chain rejects invalid credentials: basic (with the configured realm) if the basic auth is enabled,
    // otherwise bearer token
    private AuthenticationEntryPoint authenticationEntryPoint(final boolean oauth2Enabled) {
        return isBasicAuthEnabled(oauth2Enabled) ? basicAuthenticationEntryPoint() : new BearerTokenAuthenticationEntryPoint();
    }

    private boolean isBasicAuthEnabled(final boolean oauth2Enabled) {
        return !oauth2Enabled || securityProperties.isAllowHttpBasicOnOAuthEnabled();
    }

    private BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
        final BasicAuthenticationEntryPoint basicAuthEntryPoint = new BasicAuthenticationEntryPoint();
        basicAuthEntryPoint.setRealmName(securityProperties.getBasicRealm());
        return basicAuthEntryPoint;
    }

    /**
     * Checks the tenant of the authenticated principal - after the authentication, so it covers all authentication types (basic, OAuth2,
     * custom ones). Rejects (with {@link BadCredentialsException}, i.e. 401 Unauthorized):
     * <ul>
     *     <li>tenant scoped principal ({@link Principal}) of another tenant than the {@link RequestedTenant} (case-insensitive). Not
     *     403 - the tenant is part of the identity (as with <code>tenant&#92;user</code>), so the credentials are not valid for the
     *     requested tenant. Also, a 403 would reveal that the credentials are valid in another tenant</li>
     *     <li>soft deleted tenant</li>
     *     <li>global (not tenant scoped) principal acting in the requested tenant (if enabled by
     *     <code>hawkbit.server.security.tenant-resolver.global-user-requested-tenant</code>, otherwise it stays not tenant scoped) -
     *     if the tenant doesn't exist. The requested tenant is never created implicitly, e.g. on a typo</li>
     * </ul>
     * Looks up the tenant metadata once - for the principal's own tenant it is created if missing and the implicit tenant creation is
     * allowed.
     */
    private Filter tenantAuthenticationFilter(final SystemManagement systemManagement) {
        return (request, response, chain) -> {
            final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                boolean actsInRequestedTenant = false;
                final String requestedTenant = RequestedTenant.getTenant();
                if (requestedTenant != null) {
                    if (authentication.getPrincipal() instanceof Principal principal) {
                        if (!requestedTenant.equalsIgnoreCase(principal.getTenant())) {
                            throw new BadCredentialsException("Authenticated principal is not of the requested tenant");
                        }
                    } else if (securityProperties.getTenantResolver().isGlobalUserRequestedTenant()) {
                        final SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                        securityContext.setAuthentication(
                                new AccessContext.AuthenticationDelegate(requestedTenant, authentication.getName(), authentication));
                        SecurityContextHolder.setContext(securityContext);
                        actsInRequestedTenant = true;
                    }
                }

                // global (not tenant scoped) users not acting in a requested tenant have no tenant (metadata)
                if (AccessContext.tenant() != null) {
                    try {
                        if (actsInRequestedTenant) {
                            if (asSystem(systemManagement::findTenantMetadataWithoutDetails).isEmpty()) {
                                throw new BadCredentialsException("Requested tenant doesn't exist");
                            }
                        } else {
                            asSystem(systemManagement::getTenantMetadataWithoutDetails);
                        }
                    } catch (final TenantSoftDeletedException e) {
                        log.warn("Soft deleted tenant access attempted via MGMT");
                        throw new BadCredentialsException("Tenant is soft deleted", e);
                    }
                }
            }
            chain.doFilter(request, response);
        };
    }

    private class DefaultOAuth2ResourceServerCustomizer implements Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> {

        @SuppressWarnings("unchecked")
        @Override
        public void customize(OAuth2ResourceServerConfigurer<HttpSecurity> oauth2ResourceServerConfigurer) {
            final Claim claim = oidcProperties.getOauth2().getResourceserver().getJwt().getClaim();
            final String usernameClaim = claim.getUsername();
            final String tenantClaim = claim.getTenant();
            final String rolesClaim = claim.getRoles();
            oauth2ResourceServerConfigurer.jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwt -> {
                final String username = Objects.requireNonNull(followPathInJwtClaims(jwt, usernameClaim, String.class));
                final String tenant = tenantClaim == null
                        ? "DEFAULT"
                        : Objects.requireNonNull(followPathInJwtClaims(jwt, tenantClaim, String.class));
                final Collection<GrantedAuthority> authorities = Optional
                        .ofNullable(followPathInJwtClaims(jwt, rolesClaim, Collection.class))
                        .map(resourceRoles -> ((Collection<String>) resourceRoles).stream()
                                .distinct()
                                .map(SimpleGrantedAuthority::new)
                                .map(GrantedAuthority.class::cast)
                                .toList())
                        .orElseGet(Collections::emptyList);
                return new HawkbitJwtAuthenticationToken(jwt, new Principal(tenant, username), authorities);
            }));
        }

        @SuppressWarnings("unchecked")
        private static <T> T followPathInJwtClaims(final Jwt jwt, final String path, final Class<T> clazz) {
            final String[] chunks = path.split("\\.");
            Object current = jwt.getClaims();
            if (current == null) {
                return null;
            }
            for (final String chunk : chunks) {
                if (current instanceof Map<?, ?> map) {
                    current = map.get(chunk);
                    if (current == null) {
                        log.warn("Path {} not found in claim", path);
                        return null;
                    }
                } else {
                    log.warn("Unexpected claim type for path {} (chunk {})! Expected a Map but got {}", path, chunk, current.getClass());
                    return null;
                }
            }

            if (!clazz.isInstance(current)) {
                log.warn("Unexpected claim type for path {}! Expected a {} but got {}", path, clazz.getName(), current.getClass());
                return null;
            }

            return (T) current;
        }

        @Getter
        @EqualsAndHashCode(callSuper = true)
        private static class HawkbitJwtAuthenticationToken extends AbstractOAuth2TokenAuthenticationToken<Jwt> {

            @Serial
            private static final long serialVersionUID = 1L;

            private final String name;

            private HawkbitJwtAuthenticationToken(final Jwt jwt, Principal principal, final Collection<GrantedAuthority> authorities) {
                super(jwt, principal, jwt, authorities);
                name = jwt.getSubject();
                setAuthenticated(true);
            }

            @Override
            public Map<String, Object> getTokenAttributes() {
                return getToken().getClaims();
            }
        }
    }
}
