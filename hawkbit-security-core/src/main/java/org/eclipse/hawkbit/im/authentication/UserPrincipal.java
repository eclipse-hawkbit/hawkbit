/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.im.authentication;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;

/**
 * A software provisioning user principal definition stored in the
 * {@link SecurityContext} which contains the user specific attributes.
 * 
 */
public class UserPrincipal extends User {

    private static final long serialVersionUID = 1L;

    private final String firstname;
    private final String lastname;
    private final String loginname;
    private final String tenant;
    private final String email;

    /**
     * @param username
     *            the user name of the user
     * @param firstname
     *            the first name of the user
     * @param lastname
     *            the last name of the user
     * @param loginname
     *            the login name of user
     * @param tenant
     *            the tenant of the user
     * @param email
     *            address of the user
     */
    public UserPrincipal(final String username, final String firstname, final String lastname, final String loginname,
            final String email, final String tenant) {
        this(username, "***", lastname, firstname, loginname, email, tenant, Collections.emptyList());
    }

    /**
     * @param username
     *            the user name of the user
     * @param password
     *            the password of the user
     * @param firstname
     *            the first name of the user
     * @param lastname
     *            the last name of the user
     * @param loginname
     *            the login name of user
     * @param tenant
     *            the tenant of the user
     * @param email
     *            address of the user
     * @param authorities
     *            the authorities which the user has
     */
    // too many parameters, builder pattern wouldn't work easy due the super
    // constructor.
    @SuppressWarnings("squid:S00107")
    public UserPrincipal(final String username, final String password, final String firstname, final String lastname,
            final String loginname, final String email, final String tenant,
            final Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.firstname = firstname;
        this.lastname = lastname;
        this.loginname = loginname;
        this.tenant = tenant;
        this.email = email;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getLoginname() {
        return loginname;
    }

    public String getTenant() {
        return tenant;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
