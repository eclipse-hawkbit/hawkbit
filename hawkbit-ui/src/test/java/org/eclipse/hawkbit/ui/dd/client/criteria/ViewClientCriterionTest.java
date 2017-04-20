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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.hawkbit.ui.dd.client.criteria.ViewClientCriterion.ViewCriterionTemplates;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import com.google.gwtmockito.WithClassesToStub;
import com.vaadin.client.UIDL;
import com.vaadin.client.ui.VDragAndDropWrapper;
import com.vaadin.client.ui.VScrollTable;
import com.vaadin.client.ui.dd.VDragEvent;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Unit Tests - Drag And Drop")
@Stories("Client-side Accept criteria")
@RunWith(GwtMockitoTestRunner.class)
@WithClassesToStub({ RootPanel.class })
public class ViewClientCriterionTest {

    @Test
    @Description("Verfies compilation of template strings.")
    public void verifyCompiledTemplateStrings() {
        final ViewCriterionTemplates templates = GWT.create(ViewCriterionTemplates.class);

        // compile templates
        final String multiSelectionStyle = templates.multiSelectionStyle("my-theme", "10").asString();
        final String notificationMsg = templates.notificationMsg("some-message").asString();

        // assure compilation
        assertThat(multiSelectionStyle).as("Expected: Compiled template string").isNotNull();
        assertThat(notificationMsg).as("Expected: Compiled template string").isNotNull();
    }

    @Test
    @Description("Process serialized config for hiding the drop hints.")
    public void processSerializedDropTargetHintsConfig() {
        final ViewClientCriterion cut = new ViewClientCriterion();

        // prepare configuration:
        final Document document = Document.get();
        final UIDL uidl = GWT.create(UIDL.class);
        when(uidl.getIntAttribute("cdac")).thenReturn(3);
        final Element[] elements = new Element[3];
        for (int i = 0; i < 3; i++) {
            when(uidl.getStringAttribute("dac" + String.valueOf(i))).thenReturn("itemId" + String.valueOf(i));
            elements[i] = Mockito.mock(Element.class);
            when(document.getElementById("itemId" + String.valueOf(i))).thenReturn(elements[i]);
        }

        // act
        try {
            cut.hideDropTargetHints(uidl);

            // assure invocation
            for (int i = 0; i < 3; i++) {
                verify(document).getElementById("itemId" + String.valueOf(i));
                verify(elements[i]).removeClassName(ViewComponentClientCriterion.HINT_AREA_STYLE);
            }

            // cross-check
            verify(document, Mockito.never()).getElementById("itemId3");

        } finally {
            reset(Document.get());
        }
    }

    @Test
    @Description("Exception occures when processing serialized config for hiding the drop hints.")
    public void exceptionWhenProcessingDropTargetHintsDataStructure() {
        final ViewClientCriterion cut = new ViewClientCriterion();

        // prepare configuration:
        final Document document = Document.get();
        final UIDL uidl = GWT.create(UIDL.class);
        when(uidl.getIntAttribute("cdac")).thenReturn(2);
        when(uidl.getStringAttribute("dac0")).thenReturn("no-problem");
        when(uidl.getStringAttribute("dac1")).thenReturn("problem-bear");
        doThrow(new RuntimeException()).when(uidl).getStringAttribute("dac1");

        // act
        try {
            cut.hideDropTargetHints(uidl);

            // assure that no-problem was invoked anyway
            verify(document).getElementById("no-problem");

            // cross-check that problem-bear was never invoked
            verify(document, Mockito.never()).getElementById("problem-bear");
        } catch (final RuntimeException re) {
            fail("Exception is not re-thrown in order to continue with the loop");
        } finally {
            reset(Document.get());
        }
    }

    @Test
    @Description("Check multi row drag decoration with non-table widget")
    public void processMultiRowDragDecorationNonTable() {
        final ViewClientCriterion cut = new ViewClientCriterion();

        // prepare drag-event with non table widget:
        final VDragAndDropWrapper nonTable = Mockito.mock(VDragAndDropWrapper.class);
        final VDragEvent dragEvent = CriterionTestHelper.createMockedVDragEvent("thisId", nonTable);
        final Document document = Document.get();

        // act
        cut.setMultiRowDragDecoration(dragEvent);

        // assure that multi-row decoration processing was skipped
        verify(document, Mockito.never()).getElementById(ViewClientCriterion.SP_DRAG_COUNT);
    }

    @Test
    @Description("Check multi row drag decoration with single selection")
    public void processMultiRowDragDecorationSingleSelection() {
        final ViewClientCriterion cut = new ViewClientCriterion();

        // prepare table
        final VScrollTable table = Mockito.spy(new VScrollTable());
        table.selectedRowKeys.add("one");

        // prepare drag-event with table widget:
        final VDragEvent dragEvent = CriterionTestHelper.createMockedVDragEvent("thisId", table);

        // prepare document
        final Document document = Document.get();
        final Element ele = Mockito.mock(Element.class);
        when(document.getElementById(ViewClientCriterion.SP_DRAG_COUNT)).thenReturn(ele);

        try {
            // act
            cut.setMultiRowDragDecoration(dragEvent);

            // assure that multi-row decoration for the table was processed
            verify(document).getElementById(ViewClientCriterion.SP_DRAG_COUNT);

            // assure that no multi selection was detected
            verify(ele).removeFromParent();
        } finally {
            reset(Document.get());
        }
    }

    @Test
    @Description("Check multi row drag decoration with a single item dragged while a multi selection is active in table")
    public void processMultiRowDragDecorationMultiSelectionNotDragged() {
        final ViewClientCriterion cut = new ViewClientCriterion();

        // prepare table
        final VScrollTable table = Mockito.spy(new VScrollTable());
        table.selectedRowKeys.add("one");
        table.selectedRowKeys.add("two");
        table.focusedRow = Mockito.mock(VScrollTable.VScrollTableBody.VScrollTableRow.class);
        when(table.focusedRow.getKey()).thenReturn("another");

        // prepare drag-event with table widget:
        final VDragEvent dragEvent = CriterionTestHelper.createMockedVDragEvent("thisId", table);

        // prepare document
        final Document document = Document.get();
        final Element ele = Mockito.mock(Element.class);
        when(document.getElementById(ViewClientCriterion.SP_DRAG_COUNT)).thenReturn(ele);

        try {
            // act
            cut.setMultiRowDragDecoration(dragEvent);

            // assure that multi-row decoration for the table was processed
            verify(document).getElementById(ViewClientCriterion.SP_DRAG_COUNT);

            // assure that no multi selection was detected
            verify(ele).removeFromParent();
        } finally {
            reset(Document.get());
        }
    }

    @Test
    @Description("Check multi row drag decoration with a valid multi selection")
    public void processMultiRowDragDecorationMultiSelection() {
        final ViewClientCriterion cut = new ViewClientCriterion();

        // prepare table
        final VScrollTable table = Mockito.spy(new VScrollTable());
        table.selectedRowKeys.add("one");
        table.selectedRowKeys.add("two");
        table.focusedRow = Mockito.mock(VScrollTable.VScrollTableBody.VScrollTableRow.class);
        when(table.focusedRow.getKey()).thenReturn("one");

        // prepare drag-event with table widget:
        final VDragEvent dragEvent = CriterionTestHelper.createMockedVDragEvent("thisId", table, "myTheme");
        dragEvent.getTransferable().getDragSource().getConnection().getUIConnector();

        // prepare document
        final Document document = Document.get();
        final StyleElement ele = Mockito.spy(StyleElement.class);
        when(ele.getTagName()).thenReturn(StyleElement.TAG);
        when(document.getElementById(ViewClientCriterion.SP_DRAG_COUNT)).thenReturn(ele);

        try {
            // act
            cut.setMultiRowDragDecoration(dragEvent);

            // assure that multi-row decoration for the table was processed
            verify(document).getElementById(ViewClientCriterion.SP_DRAG_COUNT);

            // assure that no multi selection was detected
            verify(ele).setInnerSafeHtml(any(SafeHtml.class));
        } finally {
            reset(Document.get());
        }
    }
}
