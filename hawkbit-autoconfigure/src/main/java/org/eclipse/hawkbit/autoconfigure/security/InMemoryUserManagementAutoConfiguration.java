/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.autoconfigure.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.eclipse.hawkbit.im.authentication.MultitenancyIndicator;
import org.eclipse.hawkbit.im.authentication.PermissionUtils;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.im.authentication.TenantAwareUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.ObjectUtils;

/**
 * Autoconfiguration for the in-memory-user-management.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties({ TenantAwareUserProperties.class })
public class InMemoryUserManagementAutoConfiguration extends GlobalAuthenticationConfigurerAdapter {

    private static final String DEFAULT_TENANT = "DEFAULT";

    private final UserDetailsService userDetailsService;

    InMemoryUserManagementAutoConfiguration(
            final SecurityProperties securityProperties,
            final TenantAwareUserProperties tenantAwareUserProperties,
            final Optional<PasswordEncoder> passwordEncoder) {
        userDetailsService = userDetailsService(
                securityProperties, tenantAwareUserProperties, passwordEncoder.orElse(null));
    }

    @Override
    public void configure(final AuthenticationManagerBuilder auth) {
        final DaoAuthenticationProvider userDaoAuthenticationProvider = new TenantDaoAuthenticationProvider();
        userDaoAuthenticationProvider.setUserDetailsService(userDetailsService);
        auth.authenticationProvider(userDaoAuthenticationProvider);
    }

    private static UserDetailsService userDetailsService(
            final SecurityProperties securityProperties,
            final TenantAwareUserProperties tenantAwareUserProperties,
            final PasswordEncoder passwordEncoder) {
        final List<User> userPrincipals = new ArrayList<>();
        tenantAwareUserProperties.getUsers().forEach((username, user) -> {
            final TenantAwareUser userPrincipal = new TenantAwareUser(
                    username, password(user.getPassword(), passwordEncoder),
                    createAuthorities(user.getRoles(), user.getPermissions(), Collections::emptyList),
                    ObjectUtils.isEmpty(user.getTenant()) ? DEFAULT_TENANT : user.getTenant());
            userPrincipals.add(userPrincipal);
        });

        // If no tenant users are configured through the tenant user properties, set up
        // the default user from spring security properties as super DEFAULT tenant user
        if (userPrincipals.isEmpty()) {
            userPrincipals
                    .add(new TenantAwareUser(
                            securityProperties.getUser().getName(),
                            password(securityProperties.getUser().getPassword(), passwordEncoder),
                            createAuthorities(
                                    securityProperties.getUser().getRoles(), Collections.emptyList(),
                                    PermissionUtils::createAllAuthorityList),
                            DEFAULT_TENANT));
        } else if (securityProperties != null && securityProperties.getUser() != null &&
                !securityProperties.getUser().isPasswordGenerated()) {
            // otherwise if the security user is explicitly setup (no autogenerated password)
            // set it up as generic non tenant user
            userPrincipals
                    .add(new User(
                            securityProperties.getUser().getName(),
                            password(securityProperties.getUser().getPassword(), passwordEncoder),
                            createAuthorities(
                                    securityProperties.getUser().getRoles(), Collections.emptyList(),
                                    PermissionUtils::createAllAuthorityList)));
        }

        return new FixedInMemoryTenantAwareUserDetailsService(userPrincipals);
    }

    private static String password(final String password, final PasswordEncoder passwordEncoder) {
        return passwordEncoder == null && !Pattern.compile("^\\{.+}.*$").matcher(password).matches() ?
                "{noop}" + password : password;
    }

    private static List<GrantedAuthority> createAuthorities(
            final List<String> userRoles, final List<String> userPermissions,
            final Supplier<List<GrantedAuthority>> defaultRolesSupplier) {
        if (ObjectUtils.isEmpty(userRoles) && ObjectUtils.isEmpty(userPermissions)) {
            return defaultRolesSupplier.get();
        }

        final List<GrantedAuthority> grantedAuthorityList = new ArrayList<>();
        if (userRoles != null) {
            for (final String role : userRoles) {
                grantedAuthorityList.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
        }
        // Allows ALL as a shorthand for all permissions
        if (userPermissions.size() == 1 && "ALL".equals(userPermissions.get(0))) {
            grantedAuthorityList.addAll(PermissionUtils.createAllAuthorityList());
        } else {
            for (final String permission : userPermissions) {
                grantedAuthorityList.add(new SimpleGrantedAuthority(permission));
            }
        }

        return grantedAuthorityList;
    }

    private static class FixedInMemoryTenantAwareUserDetailsService implements UserDetailsService {

        private final HashMap<String, User> userMap = new HashMap<>();

        private FixedInMemoryTenantAwareUserDetailsService(final Collection<User> userPrincipals) {
            for (final User user : userPrincipals) {
                userMap.put(user.getUsername(), user);
            }
        }

        @Override
        public UserDetails loadUserByUsername(final String username) {
            final User user = userMap.get(username);
            if (user == null) {
                throw new UsernameNotFoundException("No such user");
            }
            // Spring mutates the data, so we must return a copy here
            return clone(user);
        }

        private static User clone(final User user) {
            if (user instanceof TenantAwareUser) {
                return new TenantAwareUser(user.getUsername(), user.getPassword(), user.getAuthorities(),
                        ((TenantAwareUser)user).getTenant());
            } else {
                return new User(user.getUsername(), user.getPassword(), user.getAuthorities());
            }
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