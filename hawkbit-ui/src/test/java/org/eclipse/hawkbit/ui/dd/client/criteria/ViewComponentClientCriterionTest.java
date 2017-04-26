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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.vaadin.client.UIDL;
import com.vaadin.client.ui.dd.VDragAndDropManager;
import com.vaadin.client.ui.dd.VDragEvent;
import com.vaadin.client.ui.dd.VDropHandler;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Drag And Drop")
@Stories("Client-side Accept criteria")
@RunWith(GwtMockitoTestRunner.class)
public class ViewComponentClientCriterionTest {

    @Test
    @Description("Process serialized data structure for preparing the drop targets to show.")
    public void processSerializedDropTargetHintsDataStructure() {
        final ViewComponentClientCriterion cut = new ViewComponentClientCriterion();

        // prepare configuration:
        final Document document = Document.get();
        final UIDL uidl = GWT.create(UIDL.class);
        when(uidl.getIntAttribute("cda")).thenReturn(3);
        final Element[] elements = new Element[3];
        for (int i = 0; i < 3; i++) {
            when(uidl.getStringAttribute("da" + String.valueOf(i))).thenReturn("itemId" + String.valueOf(i));
            elements[i] = Mockito.mock(Element.class);
            when(document.getElementById("itemId" + String.valueOf(i))).thenReturn(elements[i]);
        }

        // act
        cut.showDropTargetHints(uidl);

        // assure invocation
        for (int i = 0; i < 3; i++) {
            verify(document).getElementById("itemId" + String.valueOf(i));
            verify(elements[i]).addClassName(ViewComponentClientCriterion.HINT_AREA_STYLE);
        }

        // cross-check
        verify(document, Mockito.times(0)).getElementById("itemId3");

    }

    @Test
    @Description("Exception occures when processing serialized data structure for preparing the drop targets to show.")
    public void exceptionWhenProcessingDropTargetHintsDataStructure() {
        final ViewComponentClientCriterion cut = new ViewComponentClientCriterion();

        // prepare configuration:
        final Document document = Document.get();
        final UIDL uidl = GWT.create(UIDL.class);
        when(uidl.getIntAttribute("cda")).thenReturn(2);
        when(uidl.getStringAttribute("da0")).thenReturn("no-problem");
        when(uidl.getStringAttribute("da1")).thenReturn("problem-bear");
        doThrow(new RuntimeException()).when(uidl).getStringAttribute("da1");

        // act
        try {
            cut.showDropTargetHints(uidl);
        } catch (final RuntimeException re) {
            fail("Exception is not re-thrown in order to continue with the loop");
        }

        // assure that no-problem was invoked anyway
        verify(document).getElementById("no-problem");

        // cross-check that problem-bear was never invoked
        verify(document, Mockito.times(0)).getElementById("problem-bear");
    }

    @Test
    @Description("Verifies that drag source is valid for the configured prefix")
    public void checkDragSourceWithValidId() {
        final ViewComponentClientCriterion cut = new ViewComponentClientCriterion();

        // prepare drag-event:
        final String prefix = "this";
        final String id = "thisId";
        final VDragEvent dragEvent = CriterionTestHelper.createMockedVDragEvent(id);

        // prepare configuration:
        final UIDL uidl = GWT.create(UIDL.class);
        when(uidl.getStringAttribute("ds")).thenReturn(prefix);

        // act
        final boolean result = cut.isValidDragSource(dragEvent, uidl);

        // assure that drag source is valid: [thisId startsWith this]
        assertThat(result).as("Expected: [" + id + " startsWith " + prefix + "].").isTrue();
    }

    @Test
    @Description("Verifies that drag source is not valid for the configured prefix")
    public void checkDragSourceWithInvalidId() {
        final ViewComponentClientCriterion cut = new ViewComponentClientCriterion();

        // prepare drag-event:
        final String prefix = "this";
        final String id = "notThis";
        final VDragEvent dragEvent = CriterionTestHelper.createMockedVDragEvent(id);

        // prepare configuration:
        final UIDL uidl = GWT.create(UIDL.class);
        when(uidl.getStringAttribute("ds")).thenReturn(prefix);

        // act
        final boolean result = cut.isValidDragSource(dragEvent, uidl);

        // assure that drag source is valid: [thisId !startsWith this]
        assertThat(result).as("Expected: [" + id + " !startsWith " + prefix + "].").isFalse();
    }

    @Test
    @Description("An exception occures while the drag source is validated against the configured prefix")
    public void exceptionWhenCheckingDragSource() {

        final ViewComponentClientCriterion cut = new ViewComponentClientCriterion();

        // prepare drag-event:
        final String prefix = "this";
        final String id = "notThis";
        final VDragEvent dragEvent = CriterionTestHelper.createMockedVDragEvent(id);
        doThrow(new RuntimeException()).when(dragEvent).getTransferable();

        // prepare configuration:
        final UIDL uidl = GWT.create(UIDL.class);
        when(uidl.getStringAttribute("ds")).thenReturn(prefix);

        // act
        Boolean result = null;
        try {
            result = cut.isValidDragSource(dragEvent, uidl);
        } catch (final Exception ex) {
            fail("Exception is not re-thrown");
        }

        // assure that in case of exception the drag source is declared invalid
        assertThat(result).as("Expected: Invalid drag if exception occures.").isFalse();
    }

    @Test
    @Description("Successfully checks if the current drop location is in the list of valid drop targets")
    public void successfulCheckValidDropTarget() {
        final ViewComponentClientCriterion cut = new ViewComponentClientCriterion();

        // prepare drag and drop manager:
        final String dtargetid = "dropTarget1Id";
        final VDropHandler dropHandler = CriterionTestHelper.createMockedVDropHandler(dtargetid);
        when(VDragAndDropManager.get().getCurrentDropHandler()).thenReturn(dropHandler);

        // prepare configuration:
        final UIDL uidl = createUidlWithThreeDropTargets();

        // act
        final boolean result = cut.isValidDropTarget(uidl);

        // assure drop target is valid: [dropTarget1Id startsWith dropTarget1]
        assertThat(result).as("Expected: [" + dtargetid + " startsWith dropTarget1].").isTrue();
    }

    @Test
    @Description("Failed check if the current drop location is in the list of valid drop targets")
    public void failedCheckValidDropTarget() {
        final ViewComponentClientCriterion cut = new ViewComponentClientCriterion();

        // prepare drag and drop manager:
        final String dtargetid = "no-hit";
        final VDropHandler dropHandler = CriterionTestHelper.createMockedVDropHandler(dtargetid);
        when(VDragAndDropManager.get().getCurrentDropHandler()).thenReturn(dropHandler);

        // prepare configuration:
        final UIDL uidl = createUidlWithThreeDropTargets();

        // act
        final boolean result = cut.isValidDropTarget(uidl);

        // assure "no-hit" does not match [dropTarget0,dropTarget1,dropTarget2]
        assertThat(result).as("Expected: [" + dtargetid + " does not match one of the list entries].").isFalse();
    }

    @Test
    @Description("An exception occures while the current drop location is validated against the list of valid drop target prefixes")
    public void exceptionWhenCheckingValidDropTarget() {
        final ViewComponentClientCriterion cut = new ViewComponentClientCriterion();

        // prepare drag and drop manager:
        final String dtargetid = "no-hit";
        final VDropHandler dropHandler = CriterionTestHelper.createMockedVDropHandler(dtargetid);
        when(VDragAndDropManager.get().getCurrentDropHandler()).thenReturn(dropHandler);
        doThrow(new RuntimeException()).when(dropHandler).getConnector();

        // prepare configuration:
        final UIDL uidl = createUidlWithThreeDropTargets();

        // act
        Boolean result = null;
        try {
            result = cut.isValidDropTarget(uidl);
        } catch (final Exception ex) {
            fail("Exception is not re-thrown");
        }

        // assure that in case of exception the drop target is declared invalid
        assertThat(result).as("Expected: Invalid drop if exception occures.").isFalse();
    }

    private UIDL createUidlWithThreeDropTargets() {
        final UIDL uidl = GWT.create(UIDL.class);
        when(uidl.getIntAttribute("cdt")).thenReturn(3);
        for (int i = 0; i < 3; i++) {
            when(uidl.getStringAttribute("dt" + String.valueOf(i))).thenReturn("dropTarget" + String.valueOf(i));
        }
        return uidl;
    }

}