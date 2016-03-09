/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.security;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.hawkbit.im.authentication.MultitenancyIndicator;
import org.eclipse.hawkbit.im.authentication.PermissionService;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.im.authentication.UserAuthenticationFilter;
import org.eclipse.hawkbit.security.SecurityContextTenantAware;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.SpringSecurityAuditorAware;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for security.
 * 
 *
 *
 */
@Configuration
@EnableConfigurationProperties(DdiSecurityProperties.class)
public class SecurityAutoConfiguration {

    /**
     * @return the {@link TenantAware} singleton bean which holds the current
     *         {@link TenantAware} service and make it accessible in beans which
     *         cannot access the service directly, e.g. JPA entities.
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantAware tenantAware() {
        return new SecurityContextTenantAware();
    }

    /**
     * @return permission service to check if current user has the necessary
     *         permissions.
     */
    @Bean
    @ConditionalOnMissingBean
    public PermissionService permissionService() {
        return new PermissionService();
    }

    /**
     * Creates the auditore aware.
     * 
     * @return the spring security auditore aware
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditorAware<String> auditorAware() {
        return new SpringSecurityAuditorAware();
    }

    /**
     * Auto-configuration for the in-memory-user-management.
     * 
     *
     *
     */
    @Configuration
    @ConditionalOnMissingBean(value = { UserAuthenticationFilter.class })
    public static class InMemoryUserManagementConfiguration extends GlobalAuthenticationConfigurerAdapter {

        private static final Logger LOGGER = LoggerFactory.getLogger(InMemoryUserManagementConfiguration.class);

        @Autowired
        private AuthenticationConfiguration configuration;

        /*
         * (non-Javadoc)
         * 
         * @see org.springframework.security.config.annotation.authentication.
         * configurers. GlobalAuthenticationConfigurerAdapter
         * #configure(org.springframework.security.config.annotation.
         * authentication.builders.AuthenticationManagerBuilder)
         */
        @Override
        public void configure(final AuthenticationManagerBuilder auth) throws Exception {
            final DaoAuthenticationProvider userDaoAuthenticationProvider = new TenantDaoAuthenticationProvider();
            userDaoAuthenticationProvider.setUserDetailsService(userDetailsService());
            auth.authenticationProvider(userDaoAuthenticationProvider);
        }

        /**
         * @return the user details service to load a user from memory user
         *         manager.
         */
        @Bean
        public UserDetailsService userDetailsService() {
            final InMemoryUserDetailsManager inMemoryUserDetailsManager = new InMemoryUserDetailsManager(
                    new ArrayList<>());
            inMemoryUserDetailsManager.setAuthenticationManager(null);
            inMemoryUserDetailsManager.createUser(new User("admin", "admin", getAllAuthorities()));
            return inMemoryUserDetailsManager;
        }

        /**
         * @return the multi-tenancy indicator to disallow multi-tenancy
         */
        @Bean
        public MultitenancyIndicator multiTenancyIndicator() {
            return new MultitenancyIndicator() {
                @Override
                public boolean isMultiTenancySupported() {
                    return false;
                }
            };
        }

        private Collection<SimpleGrantedAuthority> getAllAuthorities() {
            final List<SimpleGrantedAuthority> allPermissions = new ArrayList<>();
            final Field[] declaredFields = SpPermission.class.getDeclaredFields();
            for (final Field field : declaredFields) {
                if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    try {
                        final String permissionName = (String) field.get(null);
                        allPermissions.add(new SimpleGrantedAuthority(permissionName));
                    } catch (final IllegalAccessException e) {
                        LOGGER.error(e.getMessage(), e);
                    }
                }
            }
            return allPermissions;
        }

        private static class TenantDaoAuthenticationProvider extends DaoAuthenticationProvider {
            /*
             * (non-Javadoc)
             * 
             * @see org.springframework.security.authentication.dao.
             * AbstractUserDetailsAuthenticationProvider
             * #createSuccessAuthentication(java.lang.Object,
             * org.springframework.security.core.Authentication,
             * org.springframework.security.core.userdetails.UserDetails)
             */
            @Override
            protected Authentication createSuccessAuthentication(final Object principal,
                    final Authentication authentication, final UserDetails user) {
                final UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(principal,
                        authentication.getCredentials(), user.getAuthorities());
                result.setDetails(new TenantAwareAuthenticationDetails("DEFAULT", false));
                return result;
            }
        }

        /**
         * @return the {@link UserAuthenticationFilter} to include into the SP
         *         security configuration.
         * @throws Exception
         *             lazy bean exception maybe if the authentication manager
         *             cannot be instantiated
         */
        @Bean
        public UserAuthenticationFilter userAuthenticationFilter() throws Exception {
            return new UserAuthenticationFilterBasicAuth(configuration.getAuthenticationManager());
        }

        private static final class UserAuthenticationFilterBasicAuth extends BasicAuthenticationFilter
                implements UserAuthenticationFilter {

            private UserAuthenticationFilterBasicAuth(final AuthenticationManager authenticationManager) {
                super(authenticationManager);
            }

        }
    }

}
