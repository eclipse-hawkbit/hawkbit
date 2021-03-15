/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ddi.json.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Check DDI api model classes for '@JsonIgnoreProperties' annotation
 */
@Feature("Unit Tests - Direct Device Integration API")
@Story("Serializability of DDI api Models")
public class JsonIgnorePropertiesAnnotationTest {

    @Test
    @Description(
            "This test verifies that all model classes within the 'org.eclipse.hawkbit.ddi.json.model' package are annotated with '@JsonIgnoreProperties(ignoreUnknown = true)'")
    public void shouldCheckAnnotationsForAllModelClasses() throws IOException {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String packageName = this.getClass().getPackage().getName();

        ImmutableSet<ClassPath.ClassInfo> topLevelClasses = ClassPath.from(loader).getTopLevelClasses(packageName);
        for (ClassPath.ClassInfo classInfo : topLevelClasses) {
            Class<?> modelClass = classInfo.load();
            if (modelClass.getSimpleName().contains("Test") || modelClass.isEnum()) {
                continue;
            }
            JsonIgnoreProperties annotation = modelClass.getAnnotation(JsonIgnoreProperties.class);
            assertThat(annotation).isNotNull();
            assertThat(annotation.ignoreUnknown()).isTrue();
        }
    }
}
