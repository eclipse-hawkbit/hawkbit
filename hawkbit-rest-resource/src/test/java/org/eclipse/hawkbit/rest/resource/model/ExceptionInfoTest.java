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

        assertThat(underTest.getErrorCode()).isEqualTo(knownErrorCode);
        assertThat(underTest.getExceptionClass()).isEqualTo(knownExceptionClass);
        assertThat(underTest.getMessage()).isEqualTo(knownMessage);
        assertThat(underTest.getParameters()).isEqualTo(knownParameters);
    }

}
