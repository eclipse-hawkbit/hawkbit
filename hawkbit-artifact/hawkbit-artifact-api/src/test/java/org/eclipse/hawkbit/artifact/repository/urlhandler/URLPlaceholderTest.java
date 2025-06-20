/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.artifact.repository.urlhandler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Feature: Unit Tests - Artifact URL Handler<br/>
 * Story: URL placeholder tests
 */
class URLPlaceholderTest {

    private final URLPlaceholder.SoftwareData softwareData;
    private final URLPlaceholder placeholder;

    public URLPlaceholderTest() {
        this.softwareData = new URLPlaceholder.SoftwareData(1L, "file.txt", 123L, "someHash123");
        this.placeholder = new URLPlaceholder("SuperCorp", 123L, "Super-1", 1L, softwareData);
    }

    /**
     * Same object should be equal
     */
    @Test    // Exception squid:S5785 - JUnit assertTrue/assertFalse should be simplified to the corresponding dedicated assertion
    // Need to test the equals method and need to bypass magic logic in utility classes
    @SuppressWarnings({ "squid:S5838" })
    void sameObjectShouldBeEqual() {
        assertThat(softwareData.equals(softwareData)).isTrue();
        assertThat(placeholder.equals(placeholder)).isTrue();
    }

    /**
     * Different object should not be equal
     */
    @Test    @SuppressWarnings({ "squid:S5838" })
    void differentObjectShouldNotBeEqual() {
        final URLPlaceholder.SoftwareData softwareData2 = new URLPlaceholder.SoftwareData(2L, "file.txt", 123L, "someHash123");
        final URLPlaceholder placeholder2 = new URLPlaceholder("SuperCorp", 123L, "Super-2", 2L, softwareData2);
        final URLPlaceholder placeholderWithOtherSoftwareData = new URLPlaceholder(placeholder.getTenant(),
                placeholder.getTenantId(), placeholder.getControllerId(), placeholder.getTargetId(), softwareData2);
        assertThat(placeholder.equals(placeholder2)).isFalse();
        assertThat(placeholder2.equals(placeholder)).isFalse();
        assertThat(softwareData.equals(softwareData2)).isFalse();
        assertThat(softwareData2.equals(softwareData)).isFalse();
        assertThat(placeholder.equals(placeholderWithOtherSoftwareData)).isFalse();
    }

    /**
     * Different objects with same properties should be equal
     */
    @Test    void differentObjectsWithSamePropertiesShouldBeEqual() {
        final URLPlaceholder placeholderWithSameProperties = new URLPlaceholder(placeholder.getTenant(), placeholder.getTenantId(),
                placeholder.getControllerId(), placeholder.getTargetId(), softwareData);
        assertThat(placeholder).isEqualTo(placeholderWithSameProperties);
        assertThat(placeholderWithSameProperties).isEqualTo(placeholder);
    }

    /**
     * Should not equal null
     */
    @Test    // Exception squid:S5785 - JUnit assertTrue/assertFalse should be simplified to
    // the corresponding dedicated assertion
    // Need to test the equals method and need to bypass magic logic in utility
    // classes
    @SuppressWarnings({ "squid:S5838" })
    void shouldNotEqualNull() {
        assertThat(placeholder.equals(null)).isFalse();
        assertThat(softwareData.equals(null)).isFalse();
    }

    /**
     * HashCode should not change
     */
    @Test    void hashCodeShouldNotChange() {
        final URLPlaceholder placeholderWithSameProperties = new URLPlaceholder(placeholder.getTenant(), placeholder.getTenantId(),
                placeholder.getControllerId(), placeholder.getTargetId(), softwareData);
        assertThat(placeholder).hasSameHashCodeAs(placeholder).hasSameHashCodeAs(placeholderWithSameProperties);
    }
}
