/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui.common;

import java.util.Collections;

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

    private static final String TRIM_APPENDIX = "..";
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
        return loadAndFormatUsername(username, 50);
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
            return StringUtils.EMPTY;
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
            return StringUtils.EMPTY;
        }

        return loadAndFormatUsername(baseEntity.getLastModifiedBy());
    }

    /**
     * Load user details by the current session information and format the user
     * name to max 12 characters. @see
     * {@link UserDetailsFormatter#loadAndFormatUsername(String, int)}
     * 
     * @param baseEntity
     *            the entity
     * @return the formatted user name (max 12 characters) cannot be <null>
     */
    public static String loadAndFormatCurrentUsername() {
        return loadAndFormatUsername(getCurrentUser().getUsername(), 6);
    }

    /**
     * Load user details by the user name and format the user name. If the
     * loaded {@link UserDetails} is not a instance of a {@link UserPrincipal},
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
     * @return the formatted user name (max 2 * expectedNameLength characters)
     *         cannot be <null>
     */
    public static String loadAndFormatUsername(final String username, final int expectedNameLength) {
        final UserDetails userDetails = loadUserByUsername(username);
        if (!(userDetails instanceof UserPrincipal)) {
            return userDetails.getUsername();
        }

        final UserPrincipal userPrincipal = (UserPrincipal) userDetails;

        final String trimmedFirstname = trimAndFormatDetail(userPrincipal.getFirstname(), expectedNameLength);
        final String trimmedLastname = trimAndFormatDetail(userPrincipal.getLastname(), expectedNameLength);

        if (StringUtils.isEmpty(trimmedFirstname) && StringUtils.isEmpty(trimmedLastname)) {
            return StringUtils.substring(userPrincipal.getLoginname(), 0, 2 * expectedNameLength);
        }
        return trimmedFirstname + DETAIL_SEPERATOR + trimmedLastname;
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

    private static UserDetails getCurrentUser() {
        final SecurityContext context = (SecurityContext) VaadinService.getCurrentRequest().getWrappedSession()
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        return (UserDetails) context.getAuthentication().getPrincipal();
    }

    private static String trimAndFormatDetail(final String formatString, final int expectedDetailLength) {
        final String detail = StringUtils.defaultIfEmpty(formatString, StringUtils.EMPTY);
        final String trimmedDetail = StringUtils.substring(detail, 0, expectedDetailLength);
        if (StringUtils.length(detail) > expectedDetailLength) {
            return trimmedDetail + TRIM_APPENDIX;
        }
        return trimmedDetail;
    }

    private static UserDetails loadUserByUsername(final String username) {
        final UserDetailsService userDetailsService = SpringContextHelper.getBean(UserDetailsService.class);
        try {
            final UserDetails loadUserByUsername = userDetailsService.loadUserByUsername(username);
            if (loadUserByUsername == null) {
                throw new UsernameNotFoundException("User not found " + username);
            }
            return loadUserByUsername;
        } catch (final UsernameNotFoundException e) { // NOSONAR
        }
        return new User(username, "", Collections.emptyList());
    }
}
