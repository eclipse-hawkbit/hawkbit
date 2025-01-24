/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

@Feature("Unit Tests - Repository")
@Story("Security Test")
class RepositoryManagementMethodPreAuthorizeAnnotatedTest {

    // if some methods are to be excluded
    private static final Set<Method> METHOD_SECURITY_EXCLUSION = new HashSet<>();

    @Test
    @Description("Verifies that repository methods are @PreAuthorize annotated")
    void repositoryManagementMethodsArePreAuthorizedAnnotated() {
        final String packageName = getClass().getPackage().getName();
        try (final ScanResult scanResult = new ClassGraph().acceptPackages(packageName).scan()) {
            final List<? extends Class<?>> matchingClasses = scanResult.getAllClasses()
                    .stream()
                    .filter(classInPackage -> classInPackage.getSimpleName().endsWith("Management") && classInPackage.isInterface())
                    .map(ClassInfo::loadClass)
                    .toList();
            assertThat(matchingClasses).isNotEmpty();
            matchingClasses.forEach(
                    RepositoryManagementMethodPreAuthorizeAnnotatedTest::assertDeclaredMethodsContainsPreAuthorizeAnnotations);
        }

        // all exclusion should be used, otherwise the method exclusion should be
        // cleaned up again
        assertThat(METHOD_SECURITY_EXCLUSION).isEmpty();
    }

    /**
     * asserts that the given methods are annotated with the
     * {@link PreAuthorize} annotation for security. Inherited methods are not
     * checked. The following methods are excluded due inherited from
     * {@link Object}, like equals() or toString().
     *
     * @param clazz the class to retrieve the declared methods
     */
    private static void assertDeclaredMethodsContainsPreAuthorizeAnnotations(final Class<?> clazz) {
        final Method[] declaredMethods = clazz.getDeclaredMethods();
        for (final Method method : declaredMethods) {
            final boolean methodExcluded = METHOD_SECURITY_EXCLUSION.contains(method);
            if (methodExcluded || method.isSynthetic() || Modifier.isPublic(method.getModifiers())) {
                // skip method because it should be excluded
                METHOD_SECURITY_EXCLUSION.remove(method);
                continue;
            }
            final PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
            assertThat(annotation)
                    .as("The method " + method.getName() + " in class " + clazz.getName() +
                            " is not annotated with @PreAuthorize, security leak?")
                    .isNotNull();
        }
    }
}