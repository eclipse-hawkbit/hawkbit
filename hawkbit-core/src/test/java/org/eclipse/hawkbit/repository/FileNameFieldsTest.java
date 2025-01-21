/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import io.qameta.allure.Description;
import org.junit.jupiter.api.Test;

class FileNameFieldsTest {

    @Test
    @Description("Verifies that fields classes are correctly implemented")
    @SuppressWarnings("unchecked")
    void repositoryManagementMethodsArePreAuthorizedAnnotated() {
        final String packageName = getClass().getPackage().getName();
        try (final ScanResult scanResult = new ClassGraph().acceptPackages(packageName).scan()) {
            final List<? extends Class<? extends RsqlQueryField>> matchingClasses = scanResult.getAllClasses()
                    .stream()
                    .filter(classInPackage -> classInPackage.implementsInterface(RsqlQueryField.class))
                    .map(ClassInfo::loadClass)
                    .map(clazz -> (Class<? extends RsqlQueryField>) clazz)
                    .toList();
            assertThat(matchingClasses).isNotEmpty();
            matchingClasses.forEach(providerClass -> {
                assertThat(providerClass.getEnumConstants()).isNotEmpty();
                for (final RsqlQueryField provider : providerClass.getEnumConstants()) {
                    if (provider.isMap() && !provider.getSubEntityAttributes().isEmpty()) {
                        throw new UnsupportedOperationException(
                                "Currently sub-entity attributes for maps are not supported, alternatively you could use the key/value tuple, defined by SimpleImmutableEntry class");
                    }
                }
            });
        }
    }
}