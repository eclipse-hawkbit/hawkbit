/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.ddi.json.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.Test;

/**
 * Check DDI api model classes for '@JsonIgnoreProperties' annotation
 * <p/>
 * Feature: Unit Tests - Direct Device Integration API<br/>
 * Story: Serialization of DDI api Models
 */
class JsonIgnorePropertiesAnnotationTest {

    /**
     * This test verifies that all model classes within the 'org.eclipse.hawkbit.ddi.json.model' package are annotated with '@JsonIgnoreProperties(ignoreUnknown = true)'
     */
    @Test
    void checkAnnotationsForAllModelClasses() {
        final String packageName = getClass().getPackage().getName();
        try (final ScanResult scanResult = new ClassGraph()
                .enableClasspath().enableClassInfo().enableAnnotationInfo()
                .acceptPackages(packageName).scan()) {
            final List<ClassInfo> matchingClasses = scanResult.getAllClasses()
                    .stream()
                    .filter(classInPackage -> !classInPackage.getSimpleName().endsWith("Test") && !classInPackage.isEnum())
                    .toList();

            assertThat(matchingClasses).isNotEmpty();
            matchingClasses.forEach(modelClass -> {
                final AnnotationInfo annotation = modelClass.getDirectAnnotationInfo(JsonIgnoreProperties.class);
                assertThat(annotation)
                        .describedAs("Annotation 'JsonIgnoreProperties' is missing for class: " + modelClass.getSimpleName())
                        .isNotNull();
                assertThat(annotation.getParameterValues().getValue("ignoreUnknown")).isEqualTo(Boolean.TRUE);
            });
        }
    }
}
