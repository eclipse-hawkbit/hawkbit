/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import java.util.Collections;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.im.authentication.UserPrincipal;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.vaadin.server.VaadinService;

/**
 * A Utility class to user details e.g. username
 */
public final class UserDetailsFormatter {

    private static final String TRIM_APPENDIX = "...";
    private static final String DETAIL_SEPERATOR = ", ";

    private UserDetailsFormatter() {
    }

    /**
     * Load user details by the user name and format the user name to max 100
     * characters.
     * 
     * @see {@link UserDetailsFormatter#loadAndFormatUsername(String, int)}
     * 
     * @param username
     *            the user name
     * @return the formatted user name (max 100 characters) cannot be <null>
     */
    public static String loadAndFormatUsername(final String username) {
        return loadAndFormatUsername(username, 100);
    }

    /**
     * Load user details by {@link BaseEntity#getCreatedBy()} and format the
     * user name. Use {@link UserDetailsFormatter#loadAndFormatUsername(String)}
     * 
     * @param baseEntity
     *            the entity
     * @return the formatted 'created at user name' (max 100 characters) cannot
     *         be <null>
     */
    public static String loadAndFormatCreatedBy(final BaseEntity baseEntity) {
        if (baseEntity == null || baseEntity.getCreatedBy() == null) {
            return "";
        }

        return loadAndFormatUsername(baseEntity.getCreatedBy());
    }

    /**
     * Load user details by {@link BaseEntity#getLastModifiedBy()} and format
     * the user name. Use
     * {@link UserDetailsFormatter#loadAndFormatUsername(String)}
     * 
     * @param baseEntity
     *            the entity
     * @return the formatted 'last modefied by user name' (max 100 characters)
     *         cannot be <null>
     */
    public static String loadAndFormatLastModifiedBy(final BaseEntity baseEntity) {
        if (baseEntity == null || baseEntity.getLastModifiedBy() == null) {
            return "";
        }

        return loadAndFormatUsername(baseEntity.getLastModifiedBy());
    }

    /**
     * Load user details by the current session information and format the user
     * name to max 12 characters. @see
     * {@link UserDetailsFormatter#loadAndFormatUsername(String, int)}
     * 
     * @return the formatted user name (max 12 characters) cannot be <null>
     */
    public static String formatCurrentUsername() {
        return loadAndFormatUsername(getCurrentUser().getUsername(), 5);
    }

    /**
     * Load user details by the user name and format the user name. If the
     * loaded {@link UserDetails} is not an instance of a {@link UserPrincipal},
     * then just the {@link UserDetails#getUsername()} will return.
     * 
     * If first and last name available, they will combined. Otherwise the
     * {@link UserPrincipal#getLoginname()} will formatted. The formatted name
     * is reduced to 100 characters.
     * 
     * @param username
     *            the user name
     * @param expectedNameLength
     *            the name size of each name part
     * @return the formatted user name (max expectedNameLength characters)
     *         cannot be <null>
     */
    public static String loadAndFormatUsername(final String username, final int expectedNameLength) {
        final UserDetails userDetails = loadUserByUsername(username);
        return formatUserName(expectedNameLength, userDetails);
    }

    private static String formatUserName(final int expectedNameLength, final UserDetails userDetails) {
        if (!(userDetails instanceof UserPrincipal)) {
            return userDetails.getUsername();
        }

        final UserPrincipal userPrincipal = (UserPrincipal) userDetails;

        String firstname = StringUtils.defaultIfEmpty(userPrincipal.getFirstname(), "");

        if (!StringUtils.isEmpty(firstname)) {
            firstname += DETAIL_SEPERATOR;
        }

        final String firstAndLastname = firstname + StringUtils.defaultIfEmpty(userPrincipal.getLastname(), "");

        final String trimmedUsername = trimAndFormatDetail(firstAndLastname, expectedNameLength);

        if (StringUtils.isEmpty(trimmedUsername)) {
            return trimAndFormatDetail(userPrincipal.getLoginname(), expectedNameLength);
        }
        return trimmedUsername;
    }

    /**
     * Format the current tenant. The information is loaded by the current
     * session information.
     * 
     * @return the formatted user name (max 8 characters) can be <null>
     */
    public static String formatCurrentTenant() {
        final UserDetails userDetails = getCurrentUser();
        if (!(userDetails instanceof UserPrincipal)) {
            return null;
        }

        final UserPrincipal userPrincipal = (UserPrincipal) userDetails;
        return trimAndFormatDetail(userPrincipal.getTenant(), 8);
    }

    /**
     * Format the current tenant. The information is loaded by the current
     * session information.
     * 
     * @return the formatted user name (max 8 characters) can be <null>
     */
    public static Optional<String> getCurrentTenant() {
        final UserDetails userDetails = getCurrentUser();
        if (!(userDetails instanceof UserPrincipal)) {
            return Optional.empty();
        }

        final UserPrincipal userPrincipal = (UserPrincipal) userDetails;
        return Optional.of(userPrincipal.getTenant().trim());
    }

    /**
     * @return logged in users Email address
     */
    public static Optional<String> getCurrentUserEmail() {
        final UserDetails userDetails = getCurrentUser();
        if (!(userDetails instanceof UserPrincipal)) {
            return Optional.empty();
        }

        final UserPrincipal userPrincipal = (UserPrincipal) userDetails;

        return Optional.ofNullable(userPrincipal.getEmail());
    }

    public static UserDetails getCurrentUser() {
        final SecurityContext context = (SecurityContext) VaadinService.getCurrentRequest().getWrappedSession()
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        return (UserDetails) context.getAuthentication().getPrincipal();
    }

    private static String trimAndFormatDetail(final String formatString, final int expectedDetailLength) {
        final String detail = StringUtils.defaultIfEmpty(formatString, "");
        final String trimmedDetail = StringUtils.substring(detail, 0, expectedDetailLength);
        if (StringUtils.length(detail) > expectedDetailLength) {
            return trimmedDetail + TRIM_APPENDIX;
        }
        return trimmedDetail;
    }

    // Exception squid:S1166 - exception has to be hidden
    @SuppressWarnings({ "squid:S1166" })
    private static UserDetails loadUserByUsername(final String username) {
        final UserDetailsService userDetailsService = SpringContextHelper.getBean(UserDetailsService.class);
        try {
            return userDetailsService.loadUserByUsername(username);
        } catch (final UsernameNotFoundException e) {
            return new User(username, "", Collections.emptyList());
        }
    }
}
