/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.tenancy;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.User;

/**
 * A software provisioning user principal definition stored in the {@link SecurityContext} which contains the user specific attributes.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TenantAwareUser extends User {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String tenant;

    public TenantAwareUser(
            final String username, final String password, final Collection<? extends GrantedAuthority> authorities,
            final String tenant) {
        super(username, password, authorities == null ? Collections.emptyList() : authorities);
        this.tenant = tenant;
    }

    @Override
    public boolean isEnabled() {
        return true;
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
}