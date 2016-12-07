/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.dd.criteria;

import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.COMPONENT;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.COMPONENT_COUNT;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.MODE;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.PREFIX_MODE;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.STRICT_MODE;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.acceptcriteria.ClientSideCriterion;
import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;

/**
 * Server part for the client-side accept criterion that verifies the selected
 * drag source id-based for for the proposed drop target. Valid drag sources are
 * pre-configured for the criterion. If one pre-configured value matches the
 * selected drag source, the drag source is accepted. The match mode is
 * configurable and may either be {@value Mode#STRICT} or {@value Mode#PREFIX}.
 */
public class ServerItemIdClientCriterion extends ClientSideCriterion {

    /**
     * Specifies the mode for the ServerItemIdClientCriterion.
     *
     */
    public enum Mode {
        /**
         * Valid drop targets are found using {@link String#equals(Object)}.
         */
        STRICT(STRICT_MODE),
        /**
         * Valid drop targets are found using {@link String#startsWith(String)}
         */
        PREFIX(PREFIX_MODE);

        String modeShort;

        /**
         * Constructor.
         *
         * @param modeShort
         *            short string representation
         */
        Mode(String modeShort) {
            this.modeShort = modeShort;
        }

        /**
         * Gets the mode short string representation
         *
         * @return mode short string representation
         */
        String getShort() {
            return modeShort;
        }
    }

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 131826179001511177L;

    private final String[] componentIds;

    private final Mode verificationMode;

    /**
     * Constructor.
     *
     * @param verificationMode
     *            for {@value Mode#STRICT} verification is done using
     *            {@link String#equals(Object)}, for {@value Mode#PREFIX}
     *            verification is done using {@link String#startsWith(String)}
     * @param componentIds
     *            valid drag source IDs (respectively ID-prefixes) for the
     *            proposed drop target verified against the selected drag
     *            source.
     */
    public ServerItemIdClientCriterion(Mode verificationMode, String... componentIds) {
        this.componentIds = componentIds;
        this.verificationMode = verificationMode;
    }

    @Override
    public void paintContent(PaintTarget target) throws PaintException {
        super.paintContent(target);
        int paintedComponents = 0;
        for (String cId : componentIds) {
            target.addAttribute(COMPONENT + paintedComponents, cId);
            paintedComponents++;
        }
        target.addAttribute(COMPONENT_COUNT, paintedComponents);
        target.addAttribute(MODE, verificationMode.getShort());
    }

    @Override
    public boolean accept(DragAndDropEvent dragEvent) {
        String sourceComponentid = dragEvent.getTransferable().getSourceComponent().getId();
        if (sourceComponentid == null) {
            // if there is no source id, criterion fails
            return false;
        }
        for (String cId : componentIds) {
            if ((verificationMode == Mode.PREFIX && sourceComponentid.startsWith(cId))
                    || (verificationMode == Mode.STRICT && sourceComponentid.equals(cId))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getIdentifier() {
        // extending classes use client side implementation from this class
        return ServerItemIdClientCriterion.class.getCanonicalName();
    }

}
