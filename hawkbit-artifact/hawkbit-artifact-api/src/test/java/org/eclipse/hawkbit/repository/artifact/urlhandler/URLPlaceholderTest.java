/**
 * Copyright (c) 2025 Bosch Digital GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.artifact.urlhandler;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.repository.artifact.urlhandler.URLPlaceholder;
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
    @Test
    // Exception squid:S5785 - JUnit assertTrue/assertFalse should be simplified to the corresponding dedicated assertion
    // Need to test the equals method and need to bypass magic logic in utility classes
    @SuppressWarnings({ "squid:S5838" })
    void sameObjectShouldBeEqual() {
        assertThat(softwareData.equals(softwareData)).isTrue();
        assertThat(placeholder.equals(placeholder)).isTrue();
    }

    /**
     * Different object should not be equal
     */
    @Test
    @SuppressWarnings({ "squid:S5838" })
    void differentObjectShouldNotBeEqual() {
        final URLPlaceholder.SoftwareData softwareData2 = new URLPlaceholder.SoftwareData(2L, "file.txt", 123L, "someHash123");
        final URLPlaceholder placeholder2 = new URLPlaceholder("SuperCorp", 123L, "Super-2", 2L, softwareData2);
        final URLPlaceholder placeholderWithOtherSoftwareData = new URLPlaceholder(placeholder.tenant(),
                placeholder.tenantId(), placeholder.controllerId(), placeholder.targetId(), softwareData2);
        assertThat(placeholder.equals(placeholder2)).isFalse();
        assertThat(placeholder2.equals(placeholder)).isFalse();
        assertThat(softwareData.equals(softwareData2)).isFalse();
        assertThat(softwareData2.equals(softwareData)).isFalse();
        assertThat(placeholder.equals(placeholderWithOtherSoftwareData)).isFalse();
    }

    /**
     * Different objects with same properties should be equal
     */
    @Test
    void differentObjectsWithSamePropertiesShouldBeEqual() {
        final URLPlaceholder placeholderWithSameProperties = new URLPlaceholder(placeholder.tenant(), placeholder.tenantId(),
                placeholder.controllerId(), placeholder.targetId(), softwareData);
        assertThat(placeholder).isEqualTo(placeholderWithSameProperties);
        assertThat(placeholderWithSameProperties).isEqualTo(placeholder);
    }

    /**
     * HashCode should not change
     */
    @Test
    void hashCodeShouldNotChange() {
        final URLPlaceholder placeholderWithSameProperties = new URLPlaceholder(placeholder.tenant(), placeholder.tenantId(),
                placeholder.controllerId(), placeholder.targetId(), softwareData);
        assertThat(placeholder).hasSameHashCodeAs(placeholder).hasSameHashCodeAs(placeholderWithSameProperties);
    }
}