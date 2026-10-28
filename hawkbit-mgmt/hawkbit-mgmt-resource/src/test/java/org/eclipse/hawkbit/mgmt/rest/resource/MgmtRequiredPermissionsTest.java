/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.rest.RequiredPermissions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Feature: Unit Tests - Management API<br/>
 * Story: Required permissions
 */
class MgmtRequiredPermissionsTest {

    /**
     * Verifies that every Management API endpoint documents its required permissions via {@link RequiredPermissions}
     * and that they (incl. alternatives and conditionally required) are existing {@link SpPermission} authorities.
     */
    @Test
    void allEndpointsDeclareExistingPermissions() {
        final List<Method> endpoints = restApiInterfaces().stream()
                .flatMap(restApi -> Arrays.stream(restApi.getMethods()))
                .filter(method -> AnnotatedElementUtils.hasAnnotation(method, RequestMapping.class))
                .toList();

        assertThat(endpoints).isNotEmpty();
        assertThat(endpoints).allSatisfy(endpoint -> {
            final RequiredPermissions requiredPermissions = AnnotatedElementUtils.findMergedAnnotation(endpoint, RequiredPermissions.class);
            assertThat(requiredPermissions).as("@RequiredPermissions of %s", endpoint).isNotNull();
            // empty (authentication only) is allowed
            final List<String> permissions = new ArrayList<>(List.of(requiredPermissions.value()));
            Arrays.stream(requiredPermissions.anyOf()).forEach(anyOf -> {
                assertThat(anyOf.value()).as("@RequiredPermissions.AnyOf of %s has alternatives", endpoint).hasSizeGreaterThan(1);
                permissions.addAll(List.of(anyOf.value()));
            });
            permissions.addAll(List.of(requiredPermissions.optional()));
            assertThat(permissions)
                    .as("@RequiredPermissions of %s are known permissions", endpoint)
                    .isSubsetOf(SpPermission.getAllAuthorities());
        });
    }

    private static List<Class<?>> restApiInterfaces() {
        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {

            @Override
            protected boolean isCandidateComponent(final AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isInterface();
            }
        };
        scanner.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*RestApi")));
        return scanner.findCandidateComponents(MgmtRestConstants.class.getPackageName()).stream()
                .map(beanDefinition -> ClassUtils.resolveClassName(
                        beanDefinition.getBeanClassName(), MgmtRequiredPermissionsTest.class.getClassLoader()))
                .collect(Collectors.toList());
    }
}
