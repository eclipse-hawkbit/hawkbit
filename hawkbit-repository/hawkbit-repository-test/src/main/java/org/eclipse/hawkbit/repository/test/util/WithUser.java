/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.util;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to run test classes or test methods with a specific user with
 * specific permissions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@WithSecurityContext(factory = WithUser.WithUserPrincipalSecurityContextFactory.class)
@Inherited
public @interface WithUser {

    /**
     * Gets the test principal.
     * 
     * @return test principal
     */
    String principal() default "TestPrincipal";

    /**
     * Gets the test credentials.
     * 
     * @return test credentials
     */
    String credentials() default "TestCredentials";

    /**
     * Gets the test tenant id.
     * 
     * @return test tenant id
     */
    String tenantId() default "default";

    /**
     * Should tenant auto created.
     * 
     * @return <code>true</code> = auto create <code>false</code> not create
     */
    boolean autoCreateTenant() default true;

    /**
     * Gets the test authorities.
     * 
     * @return authorities
     */
    String[] authorities() default {};

    /**
     * Gets the test all permissions.
     * 
     * @return permissions
     */
    boolean allSpPermissions() default false;

    /**
     * Gets the test removeFromAllPermission.
     * 
     * @return removeFromAllPermission
     */
    String[] removeFromAllPermission() default {};

    boolean controller() default false;

    class WithUserPrincipalSecurityContextFactory implements WithSecurityContextFactory<WithUser> {
        @Override
        public SecurityContext createSecurityContext(final WithUser withUserPrincipal) {
            return new SecurityContextSwitch.WithUserSecurityContext(withUserPrincipal);
        }
    }
}