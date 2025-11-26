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

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.context.Mdc;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.oidc.OidcProperties;
import org.eclipse.hawkbit.oidc.OidcProperties.Oauth2.ResourceServer.Jwt.Claim;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.rest.SecurityManagedConfiguration;
import org.eclipse.hawkbit.rest.security.DosFilter;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.tenancy.TenantAwareUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.session.SessionManagementFilter;

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
     * Filter to protect the hawkBit server Management interface against to many requests.
     *
     * @return the spring filter registration bean for registering a denial of service protection filter in the filter chain
     */
    @Bean
    @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.filter", name = "enabled", matchIfMissing = true)
    public FilterRegistrationBean<DosFilter> dosFilterREST() {
        final FilterRegistrationBean<DosFilter> filterRegBean = SecurityManagedConfiguration.dosFilter(null,
                securityProperties.getDos().getFilter(), securityProperties.getClients());
        filterRegBean.setUrlPatterns(List.of(
                MgmtRestConstants.BASE_REST_MAPPING + "/*",
                MgmtRestConstants.BASE_SYSTEM_MAPPING + "/admin/*"));
        filterRegBean.setOrder(SecurityManagedConfiguration.DOS_FILTER_ORDER);
        filterRegBean.setName("dosMgmtFilter");

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
            // could be used for instance to set auth provider
            // Note: implementation of the customizer shall always take in account what is the already set by the hawkBit
            @Autowired(required = false) @Qualifier("hawkbitHttpSecurityCustomizer") final Customizer<HttpSecurity> httpSecurityCustomizer,
            final SystemManagement systemManagement) throws Exception {
        http
                .securityMatcher(MgmtRestConstants.BASE_REST_MAPPING + "/**", MgmtRestConstants.BASE_SYSTEM_MAPPING + "/admin/**")
                .authorizeHttpRequests(amrmRegistry -> amrmRegistry
                        .requestMatchers(MgmtRestConstants.BASE_SYSTEM_MAPPING + "/admin/**")
                        .hasAnyAuthority(SpPermission.SYSTEM_ADMIN)
                        .anyRequest()
                        .authenticated())
                .anonymous(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .addFilterAfter(
                        // Servlet filter to create metadata after successful auth over RESTful.
                        (request, response, chain) -> {
                            final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                            if (authentication != null && authentication.isAuthenticated()) {
                                AccessContext.asSystem(systemManagement::getTenantMetadataWithoutDetails);
                            }
                            chain.doFilter(request, response);
                        },
                        SessionManagementFilter.class)
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(Customizer.withDefaults())
                .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (securityProperties.getCors().isEnabled()) {
            http.cors(configurer -> configurer.configurationSource(securityProperties.getCors().toCorsConfigurationSource()));
        }

        if (securityProperties.isRequireSsl()) {
            http.requiresChannel(crmRegistry -> crmRegistry.anyRequest().requiresSecure());
        }

        if (oauth2ResourceServerCustomizer != null) {
            http.oauth2ResourceServer(oauth2ResourceServerCustomizer);
        }
        if (oauth2ResourceServerCustomizer == null || securityProperties.isAllowHttpBasicOnOAuthEnabled()) {
            http.httpBasic(configurer -> {
                final BasicAuthenticationEntryPoint basicAuthEntryPoint = new BasicAuthenticationEntryPoint();
                basicAuthEntryPoint.setRealmName(securityProperties.getBasicRealm());
                configurer.authenticationEntryPoint(basicAuthEntryPoint);
            });
        }

        if (httpSecurityCustomizer != null) {
            httpSecurityCustomizer.customize(http);
        }

        Mdc.Filter.addMdcFilter(http);

        return http.build();
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
                final String username = followPathInJwtClaims(jwt, usernameClaim, String.class);
                final String tenant = tenantClaim == null ? "DEFAULT" : followPathInJwtClaims(jwt, tenantClaim, String.class);
                final Collection<GrantedAuthority> authorities = Optional
                        .ofNullable(followPathInJwtClaims(jwt, rolesClaim, Collection.class))
                        .map(resourceRoles -> ((Collection<String>) resourceRoles).stream()
                                .distinct()
                                .map(SimpleGrantedAuthority::new)
                                .map(GrantedAuthority.class::cast)
                                .toList())
                        .orElseGet(Collections::emptyList);
                return new HawkbitJwtAuthenticationToken(jwt, new TenantAwareUser(username, username, authorities, tenant), authorities);
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
                } else if (current == null) {
                    return null;
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

            private HawkbitJwtAuthenticationToken(final Jwt jwt, TenantAwareUser user, final Collection<GrantedAuthority> authorities) {
                super(jwt, user, jwt, authorities);
                setDetails(new TenantAwareAuthenticationDetails(user.getTenant(), false));
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