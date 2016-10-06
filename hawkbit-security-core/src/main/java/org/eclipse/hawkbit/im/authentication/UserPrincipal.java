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
 *
 *
 *
 */
public class UserPrincipal extends User {

    /**
    * 
    */
    private static final long serialVersionUID = 1L;

    private final String firstname;
    private final String lastname;
    private final String loginname;
    private final String tenant;

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
     */
    public UserPrincipal(final String username, final String firstname, final String lastname, final String loginname,
            final String tenant) {
        this(username, "***", lastname, firstname, loginname, tenant, Collections.emptyList());
    }

    public UserPrincipal(final String username, final String password, final String firstname, final String lastname,
            final String loginname, final String tenant, final Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.firstname = firstname;
        this.lastname = lastname;
        this.loginname = loginname;
        this.tenant = tenant;
    }

    /**
     * @return the firstname
     */
    public String getFirstname() {
        return firstname;
    }

    /**
     * @return the lastname
     */
    public String getLastname() {
        return lastname;
    }

    /**
     * @return the loginname
     */
    public String getLoginname() {
        return loginname;
    }

    /**
     * @return the tenant
     */
    public String getTenant() {
        return tenant;
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
