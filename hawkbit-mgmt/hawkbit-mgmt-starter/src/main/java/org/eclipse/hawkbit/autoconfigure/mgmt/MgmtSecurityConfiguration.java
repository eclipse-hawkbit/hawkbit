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

import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.HashSet;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.oidc.OidcProperties;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.rest.SecurityManagedConfiguration;
import org.eclipse.hawkbit.rest.security.DosFilter;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.MdcHandler;
import org.eclipse.hawkbit.security.SystemSecurityContext;
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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.session.SessionManagementFilter;

/**
 * Security configuration for the REST management API.
 */
@Configuration
@EnableConfigurationProperties({ HawkbitSecurityProperties.class, OidcProperties.class })
@EnableWebSecurity
public class MgmtSecurityConfiguration {

    private final HawkbitSecurityProperties securityProperties;
    private final OidcProperties oidcProperties;

    public MgmtSecurityConfiguration(final HawkbitSecurityProperties securityProperties,final OidcProperties oidcProperties) {
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

    public class DefaultOAuth2ResourceServerCustomizer implements Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> {

        static class HawkbitJwtAuthenticationToken extends AbstractOAuth2TokenAuthenticationToken<Jwt> {
            private static final long serialVersionUID = 1L;
            private final String name;
            public HawkbitJwtAuthenticationToken(Jwt jwt, TenantAwareUser user, Collection<GrantedAuthority> authorities) {
                super(jwt,user,jwt,authorities);
                setDetails(new TenantAwareAuthenticationDetails(user.getTenant(),false));
                this.name = jwt.getSubject();
                setAuthenticated(true);
            }

            public Map<String, Object> getTokenAttributes() {
                return (this.getToken()).getClaims();
            }

            public String getName() {
                return this.name;
            }
        }

        static Object followPathInJWTClaims(Jwt jwt, String path){
            String[] parts = path.split("\\.");
            Object current = jwt.getClaims();
            for(String part : parts){
                if(current instanceof Map) {
                    current = ((Map<String, Object>) current).get(part);
                } else {
                    break;
                }
            }
            return current;
        }

        @Override
        public void customize(OAuth2ResourceServerConfigurer<HttpSecurity> oauth2ResourceServerConfigurer) {
            final String usernameClaim = oidcProperties.getOauth2().getResourceserver().getJwt().getClaim().getUsername();
            final String rolesClaim = oidcProperties.getOauth2().getResourceserver().getJwt().getClaim().getRoles();
            final String tenantClaim = oidcProperties.getOauth2().getResourceserver().getJwt().getClaim().getTenant();
            oauth2ResourceServerConfigurer.jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwt -> {

                String username = (String) followPathInJWTClaims(jwt,usernameClaim);
                String tenantName = tenantClaim == null ? "DEFAULT" : (String) followPathInJWTClaims(jwt,tenantClaim);
                Collection<GrantedAuthority> authorities = new HashSet<GrantedAuthority>();
                Collection<String> resourceRoles = (Collection<String>)followPathInJWTClaims(jwt,rolesClaim);
                if(resourceRoles != null){
                    authorities.addAll(resourceRoles.stream()
                            .map(x -> new SimpleGrantedAuthority(x))
                            .collect(Collectors.toSet()));
                }
                TenantAwareUser user = new TenantAwareUser(username,username,authorities,tenantName);
                return new HawkbitJwtAuthenticationToken(jwt, user, authorities);
            }));
        }
    }

    @Bean(name = "hawkbitOAuth2ResourceServerCustomizer")
    @ConditionalOnProperty(prefix = "hawkbit.server.security.oauth2.resourceserver", name = "enabled", matchIfMissing = false)
    @ConditionalOnMissingBean(name = "hawkbitOAuth2ResourceServerCustomizer")
    Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> defaultOAuth2ResourceServerCustomizer() {
        return new DefaultOAuth2ResourceServerCustomizer();
    }

    @Bean
    @Order(350)
    SecurityFilterChain filterChainREST(
            final HttpSecurity http,
            @Autowired(required = false)
            @Qualifier("hawkbitOAuth2ResourceServerCustomizer") final Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> oauth2ResourceServerCustomizer,
            // called just before build of the SecurityFilterChain.
            // could be used for instance to set authentication provider
            // Note: implementation of the customizer shall always take in account what is the already set by the hawkBit
            @Autowired(required = false)
            @Qualifier("hawkbitHttpSecurityCustomizer") final Customizer<HttpSecurity> httpSecurityCustomizer,
            final SystemManagement systemManagement,
            final SystemSecurityContext systemSecurityContext) throws Exception {
        http
                .securityMatcher(MgmtRestConstants.BASE_REST_MAPPING + "/**", MgmtRestConstants.BASE_SYSTEM_MAPPING + "/admin/**")
                .authorizeHttpRequests(amrmRegistry ->
                        amrmRegistry
                                .requestMatchers(MgmtRestConstants.BASE_SYSTEM_MAPPING + "/admin/**")
                                .hasAnyAuthority(SpPermission.SYSTEM_ADMIN)
                                .anyRequest()
                                .authenticated())
                .anonymous(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(Customizer.withDefaults())
                .sessionManagement(configurer -> configurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterAfter(
                        // Servlet filter to create metadata after successful authentication over RESTful.
                        (request, response, chain) -> {
                            final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                            if (authentication != null && authentication.isAuthenticated()) {
                                systemSecurityContext.runAsSystem(systemManagement::getTenantMetadataWithoutDetails);
                            }
                            chain.doFilter(request, response);
                        },
                        SessionManagementFilter.class);

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

        MdcHandler.Filter.addMdcFilter(http);

        return http.build();
    }
}