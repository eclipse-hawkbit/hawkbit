/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import com.google.common.reflect.ClassPath;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Unit Tests - Repository")
@Story("Security Test")
public class RepositoryManagementMethodPreAuthorizeAnnotatedTest {

    private static final Set<Method> METHOD_SECURITY_EXCLUSION = new HashSet<>();

    static {
        METHOD_SECURITY_EXCLUSION.add(getMethod(SystemManagement.class, "currentTenant"));
    }

    @Test
    @Description("Verfies that repository methods are @PreAuthorize annotated")
    public void repositoryManagementMethodsArePreAuthorizedAnnotated()
            throws ClassNotFoundException, URISyntaxException, IOException {
        final List<Class<?>> findInterfacesInPackage = findInterfacesInPackage(getClass().getPackage(),
                Pattern.compile(".*Management"));

        assertThat(findInterfacesInPackage).isNotEmpty();
        for (final Class<?> interfaceToCheck : findInterfacesInPackage) {
            assertDeclaredMethodsContainsPreAuthorizeAnnotaions(interfaceToCheck);
        }

        // all exclusion should be used, otherwise the method exlusion should be
        // cleaned up again
        assertThat(METHOD_SECURITY_EXCLUSION).isEmpty();
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
    private static void assertDeclaredMethodsContainsPreAuthorizeAnnotaions(final Class<?> clazz) {
        final Method[] declaredMethods = clazz.getDeclaredMethods();
        for (final Method method : declaredMethods) {
            final boolean methodExcluded = METHOD_SECURITY_EXCLUSION.contains(method);
            if (methodExcluded || method.isSynthetic() || Modifier.isPublic(method.getModifiers())) {
                // skip method because it should be excluded
                METHOD_SECURITY_EXCLUSION.remove(method);
                continue;
            }
            final PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertThat(annotation).as("The public method " + method.getName() + " in class " + clazz.getName()
                    + " is not annoated with @PreAuthorize, security leak?").isNotNull();
        }
    }

    private List<Class<?>> findInterfacesInPackage(final Package p, final Pattern includeFilter)
            throws URISyntaxException, IOException, ClassNotFoundException {
        return ClassPath.from(Thread.currentThread().getContextClassLoader()).getTopLevelClasses(p.getName()).stream()
                .filter(clazzInfo -> includeFilter.matcher(clazzInfo.getSimpleName()).matches())
                .map(clazzInfo -> clazzInfo.load()).filter(clazz -> clazz.isInterface()).collect(Collectors.toList());
    }

    private static Method getMethod(final Class<?> clazz, final String methodName, final Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
