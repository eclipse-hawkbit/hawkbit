/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.eclipse.hawkbit.tenancy.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.tenancy.TenantAwareUser;
import org.eclipse.hawkbit.tenancy.TenantAwareUserProperties;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.ObjectUtils;

/**
 * Authentication provider for configured via spring application properties users.
 * The users could be tenant scoped ({@link TenantAwareUserProperties}) or global ({@link SecurityProperties}).
 */
public class StaticAuthenticationProvider extends DaoAuthenticationProvider {

    public StaticAuthenticationProvider(
            final TenantAwareUserProperties tenantAwareUserProperties, final SecurityProperties securityProperties) {
        super(userDetailsService(tenantAwareUserProperties, securityProperties));
    }

    @Override
    protected Authentication createSuccessAuthentication(final Object principal, final Authentication authentication, final UserDetails user) {
        final UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(
                principal, authentication.getCredentials(), user.getAuthorities());
        result.setDetails(user instanceof TenantAwareUser tenantAwareUser
                ? new TenantAwareAuthenticationDetails(tenantAwareUser.getTenant(), false)
                : user);
        return result;
    }

    private static UserDetailsService userDetailsService(
            final TenantAwareUserProperties tenantAwareUserProperties, final SecurityProperties securityProperties) {
        final List<User> userPrincipals = new ArrayList<>();
        tenantAwareUserProperties.getUser().forEach((username, user) -> {
            final String password = password(user.getPassword());

            final List<GrantedAuthority> credentials =
                    createAuthorities(user.getRoles(), user.getPermissions(), Collections::emptyList);
            userPrincipals.add(ObjectUtils.isEmpty(user.getTenant())
                    ? new User(username, password, credentials)
                    : new TenantAwareUser(username, password, credentials, user.getTenant()));
        });

        if (securityProperties != null && securityProperties.getUser() != null && !securityProperties.getUser().isPasswordGenerated()) {
            // explicitly setup system user - add is as a regular (non-tenant scoped) user
            userPrincipals.add(new User(
                    securityProperties.getUser().getName(),
                    password(securityProperties.getUser().getPassword()),
                    createAuthorities(
                            securityProperties.getUser().getRoles(), Collections.emptyList(),
                            () -> SpPermission.getAllAuthorities().stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .map(GrantedAuthority.class::cast)
                                    .toList())));
        }

        return new FixedInMemoryTenantAwareUserDetailsService(userPrincipals);
    }

    public static final Pattern HAS_SCHEMA = Pattern.compile("^\\{[^{]+}.+$");

    private static String password(final String password) {
        return !HAS_SCHEMA.matcher(password).matches() ? "{noop}" + password : password;
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
        for (final String permission : userPermissions) {
            grantedAuthorityList.add(new SimpleGrantedAuthority(permission));
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
            if (user instanceof TenantAwareUser tenantAwareUser) {
                return new TenantAwareUser(user.getUsername(), user.getPassword(), user.getAuthorities(), tenantAwareUser.getTenant());
            } else {
                return new User(user.getUsername(), user.getPassword(), user.getAuthorities());
            }
        }
    }
}