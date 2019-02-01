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
import java.util.List;

import org.eclipse.hawkbit.im.authentication.MultitenancyIndicator;
import org.eclipse.hawkbit.im.authentication.PermissionUtils;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.im.authentication.UserPrincipal;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.User.UserBuilder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Auto-configuration for the in-memory-user-management.
 *
 */
@Configuration
@ConditionalOnMissingBean(UserDetailsService.class)
public class InMemoryUserManagementAutoConfiguration extends GlobalAuthenticationConfigurerAdapter {

    private final SecurityProperties securityProperties;

    InMemoryUserManagementAutoConfiguration(final SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
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
        final InMemoryUserDetailsManager inMemoryUserDetailsManager = new InMemoryUserPrincipalDetailsManager();
        inMemoryUserDetailsManager.setAuthenticationManager(null);
        final SecurityProperties.User user = securityProperties.getUser();
        final UserBuilder userBuilder = User.builder().username(user.getName()).password(user.getPassword())
                .authorities(PermissionUtils.createAllAuthorityList());
        final List<String> roles = user.getRoles();
        if (!roles.isEmpty()) {
            userBuilder.roles(roles.toArray(new String[roles.size()]));
        }
        inMemoryUserDetailsManager.createUser(userBuilder.build());
        return inMemoryUserDetailsManager;
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
            result.setDetails(new TenantAwareAuthenticationDetails("DEFAULT", false));
            return result;
        }
    }

    private static final class InMemoryUserPrincipalDetailsManager extends InMemoryUserDetailsManager {

        private InMemoryUserPrincipalDetailsManager() {
            super(new ArrayList<>());
        }

        @Override
        public UserDetails loadUserByUsername(final String username) {
            final UserDetails loadUserByUsername = super.loadUserByUsername(username);
            return new UserPrincipal(loadUserByUsername.getUsername(), loadUserByUsername.getPassword(),
                    loadUserByUsername.getUsername(), loadUserByUsername.getUsername(),
                    loadUserByUsername.getUsername(), null, "DEFAULT", loadUserByUsername.getAuthorities());
        }
    }
}
