/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.dd.client.criteria;

import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.DROP_AREA_CONFIG;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.DROP_AREA_CONFIG_COUNT;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.hawkbit.ui.dd.criteria.ServerViewClientCriterion;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.UIDL;
import com.vaadin.client.ui.VNotification;
import com.vaadin.client.ui.VScrollTable;
import com.vaadin.client.ui.dd.VAcceptCallback;
import com.vaadin.client.ui.dd.VAcceptCriteria;
import com.vaadin.client.ui.dd.VAcceptCriterion;
import com.vaadin.client.ui.dd.VDragEvent;
import com.vaadin.shared.Position;
import com.vaadin.shared.ui.dd.AcceptCriterion;

/**
 * Client part for the client-side accept criterion.<br>
 * This class represents a composite for
 * <code>ViewComponentClientCriterion</code> elements.<br>
 * The criterion is not only responsible to check if the current drop location
 * is a valid drop target, but also concerns about:
 * <ul>
 * <li>Hide drop hints when drop operation is finished or aborted.</li>
 * <li>Show error message (via message box) if the drop location is not a valid
 * drop target.</li>
 * <li>In case of multi-row selection: Decorate the drag element with the number
 * of dragged elements.</li>
 * </ul>
 */
@AcceptCriterion(ServerViewClientCriterion.class)
public final class ViewClientCriterion extends VAcceptCriterion implements VAcceptCallback {

    private static final Logger LOGGER = Logger.getLogger(ViewClientCriterion.class.getName());

    static final String SP_DRAG_COUNT = "sp-drag-count";

    private boolean accepted;

    private ViewCriterionTemplates multiRowSelectStyle;

    private VDragEvent previousDragEvent;

    private HandlerRegistration nativeEventHandlerRegistration;

    /**
     * This interface is used to compile string templates in the GWT context (as
     * other approaches like <code>MessageFormat</code> would fail).
     */
    interface ViewCriterionTemplates extends SafeHtmlTemplates {
        /**
         * @param theme
         *            the current UI schema
         * @param rowCount
         *            the amount of selected rows
         * @return compiled template
         */
        @Template(".{0} tbody.v-drag-element tr:after{ content:\"{1}\"; } "
                + ".{0} tr.v-drag-element:after{ content:\"{1}\"; } "
                + ".{0} table.v-drag-element:after{ content:\"{1}\"; } ")
        SafeHtml multiSelectionStyle(String theme, String rowCount);

        /**
         * @param msg
         *            the message to style
         * @return compiled style template
         */
        @Template("<p class=\"v-Notification-description\"><span class=\"v-icon\" style=\"font-family: FontAwesome;\">&#xF071;</span> {0}</p>")
        SafeHtml notificationMsg(String msg);
    }

    private static VAcceptCriterion getCriteria(UIDL configuration, int i) {
        UIDL childUIDL = configuration.getChildUIDL(i);
        return VAcceptCriteria.get(childUIDL.getStringAttribute("name"));
    }

    /**
     * Lazy compile the string templates.
     *
     * @return templates
     */
    private ViewCriterionTemplates getDraggableTemplate() {
        // no need to synchronize, JavaScript in the browser is single-threaded
        if (multiRowSelectStyle == null) {
            multiRowSelectStyle = GWT.create(ViewCriterionTemplates.class);
        }
        return multiRowSelectStyle;
    }

    @Override
    // Exception squid:S1604 - GWT 2.7 does not support Java 8
    @SuppressWarnings("squid:S1604")
    public void accept(final VDragEvent drag, final UIDL configuration, final VAcceptCallback callback) {

        if (isDragStarting(drag)) {
            final NativePreviewHandler nativeEventHandler = new NativePreviewHandler() {
                @Override
                public void onPreviewNativeEvent(NativePreviewEvent event) {
                    if (isEscKey(event) || isMouseUp(event)) {
                        try {
                            hideDropTargetHints(configuration);
                        } finally {
                            nativeEventHandlerRegistration.removeHandler();
                        }
                    }
                }
            };

            nativeEventHandlerRegistration = Event.addNativePreviewHandler(nativeEventHandler);
            setMultiRowDragDecoration(drag);
        }

        int childCount = configuration.getChildCount();
        accepted = false;
        for (int childIndex = 0; childIndex < childCount; childIndex++) {
            VAcceptCriterion crit = getCriteria(configuration, childIndex);
            crit.accept(drag, configuration.getChildUIDL(childIndex), this);
            if (Boolean.TRUE.equals(accepted)) {
                callback.accepted(drag);
                return;
            }
        }

        // if no VAcceptCriterion accepts and the mouse is release, an error
        // message is shown
        if (Event.ONMOUSEUP == Event.getTypeInt(drag.getCurrentGwtEvent().getType())) {
            showErrorNotification(drag);
        }
    }

    @Override
    public boolean needsServerSideCheck(VDragEvent drag, UIDL criterioUIDL) {
        return false;
    }

    @Override
    protected boolean accept(VDragEvent drag, UIDL configuration) {
        // not used here:
        return false;
    }

    @Override
    public void accepted(VDragEvent event) {
        accepted = true;
    }

    /**
     * Styles a multi-row selection with the number of elements.
     *
     * @param drag
     *            the current drag event holding the context.
     */
    void setMultiRowDragDecoration(VDragEvent drag) {
        Widget widget = drag.getTransferable().getDragSource().getWidget();

        if (widget instanceof VScrollTable) {
            VScrollTable table = (VScrollTable) widget;
            int rowCount = table.selectedRowKeys.size();

            Element dragCountElement = Document.get().getElementById(SP_DRAG_COUNT);
            if (rowCount > 1 && table.selectedRowKeys.contains(table.focusedRow.getKey())) {
                if (dragCountElement == null) {
                    dragCountElement = Document.get().createStyleElement();
                    dragCountElement.setId(SP_DRAG_COUNT);
                    HeadElement head = HeadElement.as(Document.get().getElementsByTagName(HeadElement.TAG).getItem(0));
                    head.appendChild(dragCountElement);
                }
                SafeHtml formattedCssStyle = getDraggableTemplate().multiSelectionStyle(determineActiveTheme(drag),
                        String.valueOf(rowCount));
                StyleElement dragCountStyleElement = StyleElement.as(dragCountElement);
                dragCountStyleElement.setInnerSafeHtml(formattedCssStyle);
            } else if (dragCountElement != null) {
                dragCountElement.removeFromParent();
            }
        }
    }

    /**
     * Checks if the origin of the given event is a pressed ESC key.
     *
     * @param event
     *            the event to analyze
     * @return <code>true</code> if the origin of the event is a pressed ESC
     *         key, otherwise <code>false</code>.
     */
    private static boolean isEscKey(final NativePreviewEvent event) {
        int typeInt = event.getTypeInt();
        if (typeInt == Event.ONKEYDOWN) {
            int keyCode = event.getNativeEvent().getKeyCode();
            if (KeyCodes.KEY_ESCAPE == keyCode) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given event is of type <code>Event.ONMOUSEUP</code>.
     *
     * @param event
     *            the event to analyze
     * @return <code>true</code> if the given event is of type
     *         <code>Event.ONMOUSEUP</code>, otherwise <code>false</code>.
     */
    private static boolean isMouseUp(final NativePreviewEvent event) {
        return Event.ONMOUSEUP == Event.getTypeInt(event.getNativeEvent().getType());
    }

    /**
     * Hides the highlighted drop target hints.
     *
     * @param configuration
     *            for the accept criterion to retrieve the drop target hints.
     */
    // Exception squid:S1166 - Hide origin exception
    // Exception squid:S2221 - This code is trans-coded to JavaScript, hence
    // Exception semantics changes
    @SuppressWarnings({ "squid:S1166", "squid:S2221" })
    void hideDropTargetHints(UIDL configuration) {
        int totalDropTargetHintsCount = configuration.getIntAttribute(DROP_AREA_CONFIG_COUNT);
        for (int dropAreaIndex = 0; dropAreaIndex < totalDropTargetHintsCount; dropAreaIndex++) {
            try {
                String dropArea = configuration.getStringAttribute(DROP_AREA_CONFIG + dropAreaIndex);
                Element hideHintFor = Document.get().getElementById(dropArea);
                if (hideHintFor != null) {
                    hideHintFor.removeClassName(ViewComponentClientCriterion.HINT_AREA_STYLE);
                }
            } catch (Exception e) {
                // log and continue
                LOGGER.log(Level.SEVERE, "Error highlighting valid drop targets: " + e.getLocalizedMessage());
            }
        }
    }

    /**
     * Displays a message box telling that the action is not allowed.
     *
     * @param drag
     *            the current drag event holding the context.
     */
    private void showErrorNotification(VDragEvent drag) {
        VNotification n = VNotification.createNotification(SPUILabelDefinitions.SP_DELAY,
                drag.getTransferable().getDragSource().getWidget());
        n.show(getDraggableTemplate().notificationMsg(SPUILabelDefinitions.ACTION_NOT_ALLOWED).asString(),
                Position.BOTTOM_RIGHT, SPUILabelDefinitions.SP_NOTIFICATION_ERROR_MESSAGE_STYLE);
    }

    /**
     * Determines the active UI theme for a given event.
     *
     * @param drag
     *            the event the UI theme is retrieved for.
     * @return the active theme (e.g. "hawkbit").
     */
    private static String determineActiveTheme(VDragEvent drag) {
        return drag.getTransferable().getDragSource().getConnection().getUIConnector().getActiveTheme();
    }

    /**
     * Tests whether this drag operation has just started or if it is just
     * proceeded.
     *
     * @param drag
     *            the event that indicates if this is a starting drag operation
     *            or a proceeding one.
     * @return <code>true</code> if the drag operation is starting, otherwise
     *         <code>false</code>
     */
    private boolean isDragStarting(VDragEvent drag) {
        boolean result = false;
        if (!drag.equals(previousDragEvent)) {
            result = true;
            previousDragEvent = drag;
        }
        return result;
    }
}
