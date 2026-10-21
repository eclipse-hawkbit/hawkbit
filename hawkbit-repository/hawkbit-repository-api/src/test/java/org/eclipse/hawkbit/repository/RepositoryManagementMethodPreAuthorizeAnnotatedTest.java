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

import java.lang.reflect.Modifier;
import java.util.List;

import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Feature: Unit Tests - Repository<br/>
 * Story: Security Test
 */
class RepositoryManagementMethodPreAuthorizeAnnotatedTest {

    /**
     * Verifies that repository methods are @PreAuthorize annotated
     */
    @Test
    void repositoryManagementMethodsArePreAuthorizedAnnotated() {
        final String packageName = getClass().getPackage().getName();
        try (final ScanResult scanResult = new ClassGraph()
                .enableClasspath().enableClassInfo().enableMethodInfo().enableAnnotationInfo()
                .acceptPackages(packageName).scan()) {
            final List<ClassInfo> matchingClasses = scanResult.getAllClasses()
                    .stream()
                    .filter(classInPackage -> classInPackage.getSimpleName().endsWith("Management") && classInPackage.isInterface())
                    // quota management doesn't need to be explicitly protected
                    .filter(classInfo -> !"org.eclipse.hawkbit.repository.QuotaManagement".equals(classInfo.getName()))
                    .toList();
            assertThat(matchingClasses).isNotEmpty();
            matchingClasses.forEach(
                    RepositoryManagementMethodPreAuthorizeAnnotatedTest::assertDeclaredMethodsContainsPreAuthorizeAnnotations);
        }
    }

    /**
     * asserts that the given methods are annotated with the
     * {@link PreAuthorize} annotation for security. Inherited methods are not
     * checked. The following methods are excluded due inherited from
     * {@link Object}, like equals() or toString().
     *
     * @param classInfo the class info to retrieve the declared methods
     */
    private static void assertDeclaredMethodsContainsPreAuthorizeAnnotations(final ClassInfo classInfo) {
        classInfo.getMethodInfo().forEach(method -> {
            if (method.isSynthetic() || !Modifier.isPublic(method.getModifiers())) {
                // skip synthetic and not public
                return;
            }
            if ("permissionGroup".equals(method.getName()) && method.getParameterInfo().isEmpty()) {
                // PermissionSupport#permissionGroup()
                return;
            }
            final AnnotationInfo annotation = method.getDirectAnnotationInfo(PreAuthorize.class);
            assertThat(annotation)
                    .as(String.format("The method %s#%s is not annotated with @PreAuthorize, security leak?",
                            classInfo.getName(), method.getName()))
                    .isNotNull();
        });
    }
}
