/*
 *  Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.eclipse.hawkbit.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Unit Tests - Artifact URL Handler")
@Story("URL placeholder tests")
class URLPlaceholderTest {

    private URLPlaceholder.SoftwareData softwareData;
    private URLPlaceholder placeholder;

    public URLPlaceholderTest() {
        this.softwareData = new URLPlaceholder.SoftwareData(1L, "file.txt", 123L, "someHash123");
        this.placeholder = new URLPlaceholder("SuperCorp", 123L, "Super-1", 1L, softwareData);
    }

    @Test
    @Description("Same object should be equal")
    // Exception squid:S5785 - JUnit assertTrue/assertFalse should be simplified to
    // the corresponding dedicated assertion
    // Need to test the equals method and need to bypass magic logic in utility
    // classes
    @SuppressWarnings({ "squid:S5785" })
    void sameObjectShouldBeEqual() {
        assertTrue(softwareData.equals(softwareData));
        assertTrue(placeholder.equals(placeholder));
    }

    @Test
    @Description("Different object should not be equal")
    void differentObjectShouldNotBeEqual() {
        final URLPlaceholder.SoftwareData softwareData2 = new URLPlaceholder.SoftwareData(2L, "file.txt", 123L, "someHash123");
        final URLPlaceholder placeholder2 = new URLPlaceholder("SuperCorp", 123L, "Super-2", 2L, softwareData2);
        final URLPlaceholder placeholderWithOtherSoftwareData = new URLPlaceholder(placeholder.getTenant(),
                placeholder.getTenantId(), placeholder.getControllerId(), placeholder.getTargetId(), softwareData2);
        assertNotEquals(placeholder, placeholder2);
        assertNotEquals(softwareData, softwareData2);
        assertNotEquals(softwareData2, softwareData);
        assertNotEquals(placeholder, placeholderWithOtherSoftwareData);
    }

    @Test
    @Description("Different objects with same properties should be equal")
    void differentObjectsWithSamePropertiesShouldBeEqual() {
        final URLPlaceholder placeholderWithSameProperties = new URLPlaceholder(placeholder.getTenant(), placeholder.getTenantId(),
                placeholder.getControllerId(), placeholder.getTargetId(), softwareData);
        assertEquals(placeholder, placeholderWithSameProperties);
        assertEquals(placeholderWithSameProperties, placeholder);
    }

    @Test
    @Description("Should not equal null")
    // Exception squid:S5785 - JUnit assertTrue/assertFalse should be simplified to
    // the corresponding dedicated assertion
    // Need to test the equals method and need to bypass magic logic in utility
    // classes
    @SuppressWarnings({ "squid:S5785" })
    void shouldNotEqualNull() {
        assertFalse(placeholder.equals(null));
        assertFalse(softwareData.equals(null));
    }

    @Test
    @Description("HashCode should not change")
    void hashCodeShouldNotChange() {
        final URLPlaceholder placeholderWithSameProperties = new URLPlaceholder(placeholder.getTenant(), placeholder.getTenantId(),
                placeholder.getControllerId(), placeholder.getTargetId(), softwareData);
        assertEquals(placeholder.hashCode(), placeholder.hashCode());
        assertEquals(placeholder.hashCode(), placeholderWithSameProperties.hashCode());
    }
}
