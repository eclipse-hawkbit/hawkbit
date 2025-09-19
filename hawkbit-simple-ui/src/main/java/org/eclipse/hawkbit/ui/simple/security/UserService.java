/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.simple.security;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.ui.simple.HawkbitMgmtClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private SecurityContextHolderStrategy securityContextHolderStrategy;
    private HawkbitMgmtClient hawkbitClient;

    @Override
    @Cacheable(cacheNames = "userDetails")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Objects.requireNonNull(username);
        var authentication = securityContextHolderStrategy.getContext().getAuthentication();
        return new User(authentication.getName(), authentication.getCredentials().toString(), getGrantedAuthorities());
    }

    public List<SimpleGrantedAuthority> getGrantedAuthorities() {
        final List<String> roles = new LinkedList<>();
        roles.add("ANONYMOUS");
        if (hawkbitClient.hasSoftwareModulesRead()) {
            roles.add("SOFTWARE_MODULE_READ");
        }
        if (hawkbitClient.hasRolloutRead()) {
            roles.add("ROLLOUT_READ");
        }
        if (hawkbitClient.hasDistributionSetRead()) {
            roles.add("DISTRIBUTION_SET_READ");
        }
        if (hawkbitClient.hasTargetRead()) {
            roles.add("TARGET_READ");
        }
        if (hawkbitClient.hasConfigRead()) {
            roles.add("CONFIG_READ");
        }
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }

    @Scheduled(fixedRateString = "${caching.spring.userDetailsTTL:1h}")
    @CacheEvict(cacheNames = "userDetails", allEntries = true)
    public void emptyCache() {
        log.debug("emptying userDetails cache");
    }
}