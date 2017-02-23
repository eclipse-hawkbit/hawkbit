/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.dd.client.criteria;

import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.DRAG_SOURCE;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.DROP_AREA;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.DROP_AREA_COUNT;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.DROP_TARGET;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.DROP_TARGET_COUNT;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.hawkbit.ui.dd.criteria.ServerViewComponentClientCriterion;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.vaadin.client.UIDL;
import com.vaadin.client.ui.dd.VAcceptCriterion;
import com.vaadin.client.ui.dd.VDragAndDropManager;
import com.vaadin.client.ui.dd.VDragEvent;
import com.vaadin.shared.ui.dd.AcceptCriterion;

/**
 * Client part for the client-side accept criterion.<br>
 * The criterion is not only responsible to check if the current drop location
 * is a valid drop target, but also concerns about:
 * <ul>
 * <li>Fail fast, if the criterion is not responsible of the current drag source
 * (some other criterion will do the job)</li>
 * <li>Highlight the drop target hints for the drag source (only if the
 * criterion is responsible for)</li>
 * <li>Check if the current drop location is valid for the criterion</li>
 * </ul>
 *
 */
@AcceptCriterion(ServerViewComponentClientCriterion.class)
public final class ViewComponentClientCriterion extends VAcceptCriterion {

    private static final Logger LOGGER = Logger.getLogger(ViewComponentClientCriterion.class.getName());

    /**
     * Css style class for drop hints.
     */
    public static final String HINT_AREA_STYLE = "show-drop-hint";

    @Override
    protected boolean accept(final VDragEvent drag, final UIDL configuration) {
        // 1. check if this component is responsible for the drag source:
        if (!isValidDragSource(drag, configuration)) {
            return false;
        }

        // 2. Highlight the valid drop areas
        showDropTargetHints(configuration);

        // 3. Check if the current drop location is a valid drop target
        return isValidDropTarget(configuration);
    }

    /**
     * Checks if this accept criterion is responsible for the current drag
     * source. Therefore the current drag source id has to start with the drag
     * source id-prefix configured for the criterion.
     *
     * @param drag
     *            the current drag event holding the context.
     * @param configuration
     *            for the accept criterion to retrieve the configured drag
     *            source id-prefix.
     * @return <code>true</code> if the criterion is responsible for the current
     *         drag source, otherwise <code>false</code>.
     */
    // Exception squid:S1166 - Hide origin exception
    // Exception squid:S2221 - This code is trans-coded to JavaScript, hence
    // Exception semantics changes
    @SuppressWarnings({ "squid:S1166", "squid:S2221" })
    boolean isValidDragSource(final VDragEvent drag, final UIDL configuration) {
        try {
            final String dragSource = drag.getTransferable().getDragSource().getWidget().getElement().getId();
            final String dragSourcePrefix = configuration.getStringAttribute(DRAG_SOURCE);
            if (dragSource.startsWith(dragSourcePrefix)) {
                return true;
            }
        } catch (final Exception e) {
            // log and continue
            LOGGER.log(Level.SEVERE, "Error verifying drag source: " + e.getLocalizedMessage());
        }

        return false;
    }

    /**
     * Highlights the valid drop targets configured for the criterion.
     *
     * @param configuration
     *            for the accept criterion to retrieve the configured drop hint
     *            areas.
     */
    // Exception squid:S1166 - Hide origin exception
    // Exception squid:S2221 - This code is trans-coded to JavaScript, hence
    // Exception semantics changes
    // Exception squid:S2629 - not supported by GWT
    @SuppressWarnings({ "squid:S1166", "squid:S2221", "squid:S2629" })
    void showDropTargetHints(final UIDL configuration) {
        final int dropAreaCount = configuration.getIntAttribute(DROP_AREA_COUNT);
        for (int dropAreaIndex = 0; dropAreaIndex < dropAreaCount; dropAreaIndex++) {
            try {
                final String dropArea = configuration.getStringAttribute(DROP_AREA + dropAreaIndex);
                LOGGER.log(Level.FINE, "Hint Area: " + dropArea);

                final Element showHintFor = Document.get().getElementById(dropArea);
                if (showHintFor != null) {
                    showHintFor.addClassName(HINT_AREA_STYLE);
                }
            } catch (final Exception e) {
                // log and continue
                LOGGER.log(Level.SEVERE, "Error highlighting drop targets: " + e.getLocalizedMessage());
            }
        }

    }

    /**
     * Checks if the current drop location is a valid drop target for the
     * criterion. Therefore the current drop location id has to start with one
     * of the drop target id-prefixes configured for the criterion.
     *
     * @param configuration
     *            for the accept criterion to retrieve the configured drop
     *            target id-prefixes.
     * @return <code>true</code> if the current drop location is a valid drop
     *         target for the criterion, otherwise <code>false</code>.
     */
    // Exception squid:S1166 - Hide origin exception
    // Exception squid:S2221 - This code is trans-coded to JavaScript, hence
    // Exception semantics changes
    // Exception squid:S2629 - not supported by GWT
    @SuppressWarnings({ "squid:S1166", "squid:S2221", "squid:S2629" })
    boolean isValidDropTarget(final UIDL configuration) {
        try {
            final String dropTarget = VDragAndDropManager.get().getCurrentDropHandler().getConnector().getWidget()
                    .getElement().getId();
            final int dropTargetCount = configuration.getIntAttribute(DROP_TARGET_COUNT);
            for (int dropTargetIndex = 0; dropTargetIndex < dropTargetCount; dropTargetIndex++) {
                final String dropTargetPrefix = configuration.getStringAttribute(DROP_TARGET + dropTargetIndex);
                LOGGER.log(Level.FINE, "Drop Target: " + dropTargetPrefix);
                if (dropTarget.startsWith(dropTargetPrefix)) {
                    return true;
                }
            }
        } catch (final Exception e) {
            // log and continue
            LOGGER.log(Level.SEVERE, "Error verifying drop target: " + e.getLocalizedMessage());
        }
        return false;
    }

    @Override
    public boolean needsServerSideCheck(final VDragEvent drag, final UIDL criterioUIDL) {
        // client-side only
        return false;
    }
}
