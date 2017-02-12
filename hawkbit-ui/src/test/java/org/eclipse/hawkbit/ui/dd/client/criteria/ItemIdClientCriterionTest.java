/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.dd.client.criteria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Description;

import com.google.gwt.core.client.GWT;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.vaadin.client.UIDL;
import com.vaadin.client.ui.dd.VDragEvent;

import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Drag And Drop")
@Stories("Client-side Accept criteria")
@RunWith(GwtMockitoTestRunner.class)
public class ItemIdClientCriterionTest {

    @Test
    @Description("Verifies that drag source is not valid for the configured id (strict mode)")
    public void noMatchInStrictMode() {
        ItemIdClientCriterion cut = new ItemIdClientCriterion();

        // prepare drag-event:
        String testId = "thisId";
        VDragEvent dragEvent = CriterionTestHelper.createMockedVDragEvent(testId);

        // prepare configuration:
        UIDL uidl = GWT.create(UIDL.class);
        String configuredId = "component0";
        String id = "this";
        when(uidl.getStringAttribute(configuredId)).thenReturn(id);
        String configuredMode = "m";
        String strictMode = "s";
        when(uidl.getStringAttribute(configuredMode)).thenReturn(strictMode);
        String count = "c";
        when(uidl.getIntAttribute(count)).thenReturn(1);

        // act
        boolean result = cut.accept(dragEvent, uidl);

        // verify that in strict mode: [thisId !equals this]
        assertThat(result).as("Expected: [" + id + " !equals " + testId + "].").isFalse();
    }

    @Test
    @Description("Verifies that drag source is valid for the configured id (strict mode)")
    public void matchInStrictMode() {
        ItemIdClientCriterion cut = new ItemIdClientCriterion();

        // prepare drag-event:
        String testId = "thisId";
        VDragEvent dragEvent = CriterionTestHelper.createMockedVDragEvent(testId);

        // prepare configuration:
        UIDL uidl = GWT.create(UIDL.class);
        String configuredId = "component0";
        String id = "thisId";
        when(uidl.getStringAttribute(configuredId)).thenReturn(id);
        String configuredMode = "m";
        String strictMode = "s";
        when(uidl.getStringAttribute(configuredMode)).thenReturn(strictMode);
        String count = "c";
        when(uidl.getIntAttribute(count)).thenReturn(1);

        // act
        boolean result = cut.accept(dragEvent, uidl);

        // verify that in strict mode: [thisId equals thisId]
        assertThat(result).as("Expected: [" + id + " equals " + testId + "].").isTrue();
    }

    @Test
    @Description("Verifies that drag source is not valid for the configured id (prefix mode)")
    public void noMatchInPrefixMode() {
        ItemIdClientCriterion cut = new ItemIdClientCriterion();

        // prepare drag-event:
        String testId = "thisId";
        VDragEvent dragEvent = CriterionTestHelper.createMockedVDragEvent(testId);

        // prepare configuration:
        UIDL uidl = GWT.create(UIDL.class);
        String configuredId = "component0";
        String prefix = "any";
        when(uidl.getStringAttribute(configuredId)).thenReturn(prefix);
        String configuredMode = "m";
        String prefixMode = "p";
        when(uidl.getStringAttribute(configuredMode)).thenReturn(prefixMode);
        String count = "c";
        when(uidl.getIntAttribute(count)).thenReturn(1);

        // act
        boolean result = cut.accept(dragEvent, uidl);

        // verify that in strict mode: [thisId !startsWith any]
        assertThat(result).as("Expected: [" + testId + " !startsWith " + prefix + "].").isFalse();
    }

    @Test
    @Description("Verifies that drag source is valid for the configured id (prefix mode)")
    public void matchInPrefixMode() {
        ItemIdClientCriterion cut = new ItemIdClientCriterion();

        // prepare drag-event:
        String testId = "thisId";
        VDragEvent dragEvent = CriterionTestHelper.createMockedVDragEvent(testId);

        // prepare configuration:
        UIDL uidl = GWT.create(UIDL.class);
        String configuredId = "component0";
        String prefix = "this";
        when(uidl.getStringAttribute(configuredId)).thenReturn(prefix);
        String configuredMode = "m";
        String prefixMode = "p";
        when(uidl.getStringAttribute(configuredMode)).thenReturn(prefixMode);
        String count = "c";
        when(uidl.getIntAttribute(count)).thenReturn(1);

        // act
        boolean result = cut.accept(dragEvent, uidl);

        // verify that in strict mode: [thisId startsWith this]
        assertThat(result).as("Expected: [" + testId + " startsWith " + prefix + "].").isTrue();
    }

}
