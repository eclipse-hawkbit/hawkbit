/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.acceptcriteria.ServerSideCriterion;
import com.vaadin.server.Page;
import com.vaadin.ui.Component;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.DragAndDropWrapper.WrapperTargetDetails;
import com.vaadin.ui.Table;

/**
 * Abstract class for Accept criteria.
 * 
 *
 *
 * 
 */
public abstract class AbstractAcceptCriteria extends ServerSideCriterion {

    private static final long serialVersionUID = 3218899104852691974L;

    private int previousRowCount;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vaadin.event.dd.acceptcriteria.AcceptCriterion#accept(com.vaadin.
     * event.dd.DragAndDropEvent )
     */
    @Override
    public boolean accept(final DragAndDropEvent dragEvent) {
        final Component compsource = dragEvent.getTransferable().getSourceComponent();
        final Component compdestination = dragEvent.getTargetDetails().getTarget();
        final int typeVal = getMouseEventType(compdestination, dragEvent);

        showHideDropAreaHighlights(typeVal, compsource, dragEvent);
        if (isValidDrop(compsource, compdestination)) {
            return true;
        } else {
            // Display action not allowed notification for invalid drop
            /* mouse event will be Event.ONMOUSEUP on drop */
            // From com.google.gwt.user.client.Event
            if (typeVal == 8) {
                invalidDrop();
            }
            return false;
        }

    }

    private int getMouseEventType(final Component compdestination, final DragAndDropEvent dragEvent) {
        int typeVal = 0;
        if (compdestination instanceof DragAndDropWrapper) {
            final WrapperTargetDetails details = (WrapperTargetDetails) dragEvent.getTargetDetails();
            typeVal = details.getMouseEvent().getType();

        } else if (compdestination instanceof Table && dragEvent.getTargetDetails().getData("mouseEvent") != null) {
            final String mouseEventDetails = dragEvent.getTargetDetails().getData("mouseEvent").toString();
            final String[] strArray = mouseEventDetails.split("\\,");
            typeVal = Integer.parseInt(strArray[7]);
        }
        return typeVal;
    }

    private void showHideDropAreaHighlights(final int typeVal, final Component compsource,
            final DragAndDropEvent dragEvent) {
        /* mouse event will be Event.ONMOUSEUP on drop */
        // From com.google.gwt.user.client.Event
        if (typeVal == 8) {
            hideDropHints();
        } else {
            if (compsource instanceof Table) {
                showRowCount(dragEvent, (Table) compsource);
            }
            analyseDragComponent(compsource);
        }
    }

    /**
     * 
     * @param dragEvent
     * @param compsource
     */
    protected void showRowCount(final DragAndDropEvent dragEvent, final Table compsource) {
        /* Show the number of rows that are dragging in the drag image */
        final Set<String> targetSelectedList = (Set<String>) compsource.getValue();
        if (previousRowCount != targetSelectedList.size()) {
            previousRowCount = targetSelectedList.size();
            /*
             * Prepare the hava script to add the <style> tag to the head of the
             * html
             */
            if (!targetSelectedList.contains(dragEvent.getTransferable().getData(SPUIDefinitions.ITEMID))) {
                previousRowCount = 1;
            }
            final String exeJS = HawkbitCommonUtil.getDragRowCountJavaScript(previousRowCount);
            Page.getCurrent().getJavaScript().execute(exeJS);
        }
    }

    /**
     * Analyze the dragging component and do respective actions.
     * 
     * @param compsource
     *            reference of drag component.
     */
    protected void analyseDragComponent(final Component compsource) {
        final String sourceID = getComponentId(compsource);
        final Object event = getDropHintConfigurations().get(sourceID);
        publishDragStartEvent(event);
    }

    /**
     * Check if source is valid drop on destination.
     * 
     * @param compsource
     *            is the component which is dragging.
     * @param compdestination
     *            is the destination component trying to drop.
     * @return
     */
    private boolean isValidDrop(final Component compsource, final Component compdestination) {
        final String sourceID = getComponentId(compsource);
        final String destinationID = getComponentId(compdestination);
        final List<String> acceptableComponents = getDropConfigurations().get(destinationID);
        // check if the destination component Id is available in acceptable
        // components
        return acceptableComponents != null && acceptableComponents.contains(sourceID);
    }

    /**
     * Find the id the component if it is button get its prefix.
     * 
     * @param component
     *            for the id has to identify.
     * @return 'id' of the component.
     */
    protected abstract String getComponentId(final Component component);

    /**
     * publish the given event into eventBus.
     * 
     * @param event
     *            to be published in eventBus.
     */
    protected abstract void publishDragStartEvent(Object event);

    /**
     * Hide the drop hints. Dragging is stopped.
     */
    protected abstract void hideDropHints();

    /**
     * Display invalid drop message.
     */
    protected abstract void invalidDrop();

    /**
     * @return
     */
    protected abstract Map<String, Object> getDropHintConfigurations();

    /**
     * Get drop configurations in Map collection like component Id as "key" and
     * its list of acceptable component Id as "value".
     * 
     * @return reference of {@link Map} of component Id
     */
    protected abstract Map<String, List<String>> getDropConfigurations();

}
