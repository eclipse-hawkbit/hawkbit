/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest.json.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Feature: Unit Tests - Management API<br/>
 * Story: Error Handling
 */
class ExceptionInfoTest {

    /**
     * Ensures that setters and getters match on teh payload.
     */
    @Test
    void setterAndGetterOnExceptionInfo() {
        final String knownExceptionClass = "hawkbit.test.exception.Class";
        final String knownErrorCode = "hawkbit.error.code.Known";
        final String knownMessage = "a known message";
        final Map<String, Object> knownInfo = new HashMap<>();
        knownInfo.put("param1", "1");
        knownInfo.put("param2", 2);

        final ExceptionInfo underTest = new ExceptionInfo();
        underTest.setErrorCode(knownErrorCode);
        underTest.setExceptionClass(knownExceptionClass);
        underTest.setMessage(knownMessage);
        underTest.setInfo(knownInfo);

        assertThat(underTest.getErrorCode()).as("The error code should match with the known error code in the test")
                .isEqualTo(knownErrorCode);
        assertThat(underTest.getExceptionClass())
                .as("The exception class should match with the known error code in the test")
                .isEqualTo(knownExceptionClass);
        assertThat(underTest.getMessage()).as("The message should match with the known error code in the test")
                .isEqualTo(knownMessage);
        assertThat(underTest.getInfo()).as("The parameters should match with the known error code in the test")
                .isEqualTo(knownInfo);
    }
}