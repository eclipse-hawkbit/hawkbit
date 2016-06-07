/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import static org.fest.assertions.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;

public class MethodSecurityUtil {

    private static final Set<String> METHOD_SECURITY_EXCLUSION = new HashSet<>();

    static {
        METHOD_SECURITY_EXCLUSION.add("equals");
        METHOD_SECURITY_EXCLUSION.add("toString");
        METHOD_SECURITY_EXCLUSION.add("hashCode");
        METHOD_SECURITY_EXCLUSION.add("clone");
        METHOD_SECURITY_EXCLUSION.add("setEnvironment");
        // this method shouldn't be public on the DeploymentManagemeht but it is
        METHOD_SECURITY_EXCLUSION.add("setOverrideObsoleteUpdateActions");
        METHOD_SECURITY_EXCLUSION.add("isOverrideObsoleteUpdateActions");
        // this method must be public accessible without security because it's
        // necessary to acccess
        // the security-token of a target without being authenticated because
        // the security-token is
        // the authentication process
        // ControllerManagement#getSecurityTokenByControllerId()
        METHOD_SECURITY_EXCLUSION.add("getSecurityTokenByControllerId");
    }

    /**
     * asserts that the given methods are annotated with the
     * {@link PreAuthorize} annotation for security. Inherited methods are not
     * checked. The following methods are excluded due inherited from
     * {@link Object}, like equals() or toString().
     * 
     * @param clazz
     *            the class to retrieve the public declared methods
     */
    public static void assertDeclaredMethodsContainsPreAuthorizeAnnotaions(final Class<?> clazz) {
        final Method[] declaredMethods = clazz.getDeclaredMethods();
        for (final Method method : declaredMethods) {
            if (!METHOD_SECURITY_EXCLUSION.contains(method.getName()) && !method.isSynthetic()
                    && Modifier.isPublic(method.getModifiers())) {
                final PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
                assertThat(annotation).as(
                        "The public method " + method.getName() + " is not annoated with @PreAuthorize, security leak?")
                        .isNotNull();
            }
        }
    }
}
