package org.eclipse.hawkbit.autoconfigure.security;

import java.util.ArrayList;

import org.eclipse.hawkbit.im.authentication.MultitenancyIndicator;
import org.eclipse.hawkbit.im.authentication.PermissionUtils;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.im.authentication.UserAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Auto-configuration for the in-memory-user-management.
 *
 */
@Configuration
@ConditionalOnMissingBean(UserDetailsService.class)
public class InMemoryUserManagementConfiguration extends GlobalAuthenticationConfigurerAdapter {

    @Autowired
    private AuthenticationConfiguration configuration;

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
    public UserDetailsService userDetailsService() {
        final InMemoryUserDetailsManager inMemoryUserDetailsManager = new InMemoryUserDetailsManager(new ArrayList<>());
        inMemoryUserDetailsManager.setAuthenticationManager(null);
        inMemoryUserDetailsManager.createUser(new User("admin", "admin", PermissionUtils.createAllAuthorityList()));
        return inMemoryUserDetailsManager;
    }

    /**
     * @return the multi-tenancy indicator to disallow multi-tenancy
     */
    @Bean
    @ConditionalOnMissingBean
    public MultitenancyIndicator multiTenancyIndicator() {
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

    /**
     * @return the {@link UserAuthenticationFilter} to include into the SP
     *         security configuration.
     * @throws Exception
     *             lazy bean exception maybe if the authentication manager
     *             cannot be instantiated
     */
    @Bean
    @ConditionalOnMissingBean
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