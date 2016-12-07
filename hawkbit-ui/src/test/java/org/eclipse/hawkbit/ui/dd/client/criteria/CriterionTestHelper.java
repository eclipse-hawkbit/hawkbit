/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.dd.client.criteria;

import static org.mockito.Mockito.when;

import org.mockito.Mockito;

import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.ui.dd.VDragEvent;
import com.vaadin.client.ui.dd.VDropHandler;
import com.vaadin.client.ui.dd.VTransferable;
import com.vaadin.client.ui.ui.UIConnector;

public class CriterionTestHelper {

    static VDragEvent createMockedVDragEvent(String dragSourceId, Widget widget, String theme) {
        VDragEvent dragEvent = createMockedVDragEvent(dragSourceId, widget);
        ApplicationConnection connection = Mockito.mock(ApplicationConnection.class);
        when(dragEvent.getTransferable().getDragSource().getConnection()).thenReturn(connection);
        UIConnector uiConnector = Mockito.mock(UIConnector.class);
        when(connection.getUIConnector()).thenReturn(uiConnector);
        when(uiConnector.getActiveTheme()).thenReturn(theme);

        return dragEvent;
    }

    static VDropHandler createMockedVDropHandler(String dropTargetId) {
        com.google.gwt.user.client.Element element = Mockito.mock(com.google.gwt.user.client.Element.class);
        when(element.getId()).thenReturn(dropTargetId);
        Widget widget = Mockito.mock(Widget.class);
        when(widget.getElement()).thenReturn(element);
        ComponentConnector connector = Mockito.mock(ComponentConnector.class);
        when(connector.getWidget()).thenReturn(widget);
        VDropHandler dropHandler = Mockito.mock(VDropHandler.class);
        when(dropHandler.getConnector()).thenReturn(connector);

        return dropHandler;
    }

    static VDragEvent createMockedVDragEvent(String dragSourceId, Widget widget) {
        com.google.gwt.user.client.Element element = Mockito.mock(com.google.gwt.user.client.Element.class);
        when(element.getId()).thenReturn(dragSourceId);
        when(widget.getElement()).thenReturn(element);
        ComponentConnector dragSource = Mockito.mock(ComponentConnector.class);
        when(dragSource.getWidget()).thenReturn(widget);
        VTransferable transferable = Mockito.mock(VTransferable.class);
        when(transferable.getDragSource()).thenReturn(dragSource);
        VDragEvent dragEvent = Mockito.mock(VDragEvent.class);
        when(dragEvent.getTransferable()).thenReturn(transferable);

        return dragEvent;
    }

    static VDragEvent createMockedVDragEvent(String dragSourceId) {
        Widget widget = Mockito.mock(Widget.class);
        return createMockedVDragEvent(dragSourceId, widget);
    }

}
