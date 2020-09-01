/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.eclipse.hawkbit.im.authentication.MultitenancyIndicator;
import org.eclipse.hawkbit.im.authentication.PermissionUtils;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.im.authentication.UserPrincipal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Auto-configuration for the in-memory-user-management.
 *
 */
@Configuration
@ConditionalOnMissingBean(UserDetailsService.class)
@EnableConfigurationProperties({ MultiUserProperties.class })
public class InMemoryUserManagementAutoConfiguration extends GlobalAuthenticationConfigurerAdapter {

    private static final String DEFAULT_TENANT = "DEFAULT";

    private final SecurityProperties securityProperties;

    private final MultiUserProperties multiUserProperties;

    InMemoryUserManagementAutoConfiguration(final SecurityProperties securityProperties,
            final MultiUserProperties multiUserProperties) {
        this.securityProperties = securityProperties;
        this.multiUserProperties = multiUserProperties;
    }

    @Override
    public void configure(final AuthenticationManagerBuilder auth) throws Exception {
        final DaoAuthenticationProvider userDaoAuthenticationProvider = new TenantDaoAuthenticationProvider();
        userDaoAuthenticationProvider.setUserDetailsService(userDetailsService());
        auth.authenticationProvider(userDaoAuthenticationProvider);
    }

    /**
     * @return the user details service to load a user from memory user manager.
     */
    @Bean
    @ConditionalOnMissingBean
    UserDetailsService userDetailsService() {

        final List<UserPrincipal> userPrincipals = new ArrayList<>();
        for (final MultiUserProperties.User user : multiUserProperties.getUsers()) {
            final List<String> permissions = user.getPermissions();
            List<GrantedAuthority> authorityList;
            // Allows ALL as a shorthand for all permissions
            if (permissions.size() == 1 && "ALL".equals(permissions.get(0))) {
                authorityList = PermissionUtils.createAllAuthorityList();
            } else {
                authorityList = createAuthoritiesFromList(permissions);
            }

            final UserPrincipal userPrincipal = new UserPrincipal(user.getUsername(), user.getPassword(),
                    user.getFirstname(), user.getLastname(), user.getUsername(), user.getEmail(), DEFAULT_TENANT,
                    authorityList);
            userPrincipals.add(userPrincipal);
        }

        // If no users are configured through the multi user properties, set up
        // the default user from security properties
        if (userPrincipals.isEmpty()) {
            final String name = securityProperties.getUser().getName();
            final String password = securityProperties.getUser().getPassword();
            final List<String> roles = securityProperties.getUser().getRoles();
            final List<GrantedAuthority> authorityList = roles.isEmpty() ? PermissionUtils.createAllAuthorityList()
                    : createAuthoritiesFromList(roles);
            userPrincipals
                    .add(new UserPrincipal(name, password, name, name, name, null, DEFAULT_TENANT, authorityList));
        }

        return new FixedInMemoryUserPrincipalUserDetailsService(userPrincipals);
    }

    private static List<GrantedAuthority> createAuthoritiesFromList(final List<String> userAuthorities) {
        final List<GrantedAuthority> grantedAuthorityList = new ArrayList<>(userAuthorities.size());
        for (final String permission : userAuthorities) {
            grantedAuthorityList.add(new SimpleGrantedAuthority(permission));
            grantedAuthorityList.add(new SimpleGrantedAuthority("ROLE_" + permission));
        }
        return grantedAuthorityList;
    }

    private static class FixedInMemoryUserPrincipalUserDetailsService implements UserDetailsService {
        private final HashMap<String, UserPrincipal> userPrincipalMap = new HashMap<>();

        public FixedInMemoryUserPrincipalUserDetailsService(final Collection<UserPrincipal> userPrincipals) {
            for (final UserPrincipal user : userPrincipals) {
                userPrincipalMap.put(user.getUsername(), user);
            }
        }

        private static UserPrincipal clone(final UserPrincipal a) {
            return new UserPrincipal(a.getUsername(), a.getPassword(), a.getFirstname(), a.getLastname(),
                    a.getLoginname(), a.getEmail(), a.getTenant(), a.getAuthorities());
        }

        @Override
        public UserDetails loadUserByUsername(final String username) {
            final UserPrincipal userPrincipal = userPrincipalMap.get(username);
            if (userPrincipal == null) {
                throw new UsernameNotFoundException("No such user");
            }
            // Spring mutates the data, so we must return a copy here
            return clone(userPrincipal);
        }

    }

    /**
     * @return the multi-tenancy indicator to disallow multi-tenancy
     */
    @Bean
    @ConditionalOnMissingBean
    MultitenancyIndicator multiTenancyIndicator() {
        return () -> false;
    }

    private static class TenantDaoAuthenticationProvider extends DaoAuthenticationProvider {

        @Override
        protected Authentication createSuccessAuthentication(final Object principal,
                final Authentication authentication, final UserDetails user) {
            final UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(principal,
                    authentication.getCredentials(), user.getAuthorities());
            result.setDetails(new TenantAwareAuthenticationDetails(DEFAULT_TENANT, false));
            return result;
        }
    }
}
