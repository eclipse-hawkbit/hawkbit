/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ExceptionInfoTest {

    @Test
    public void setterAndGetter() {
        final String knownExceptionClass = "hawkbit.test.exception.Class";
        final String knownErrorCode = "hawkbit.error.code.Known";
        final String knownMessage = "a known message";
        final List<String> knownParameters = new ArrayList<>();
        knownParameters.add("param1");
        knownParameters.add("param2");

        final ExceptionInfo underTest = new ExceptionInfo();
        underTest.setErrorCode(knownErrorCode);
        underTest.setExceptionClass(knownExceptionClass);
        underTest.setMessage(knownMessage);
        underTest.setParameters(knownParameters);

        assertThat(underTest.getErrorCode()).as("The error code should match with the known error code in the test")
                .isEqualTo(knownErrorCode);
        assertThat(underTest.getExceptionClass())
                .as("The exception class should match with the known error code in the test")
                .isEqualTo(knownExceptionClass);
        assertThat(underTest.getMessage()).as("The message should match with the known error code in the test")
                .isEqualTo(knownMessage);
        assertThat(underTest.getParameters()).as("The parameters should match with the known error code in the test")
                .isEqualTo(knownParameters);
    }

}
