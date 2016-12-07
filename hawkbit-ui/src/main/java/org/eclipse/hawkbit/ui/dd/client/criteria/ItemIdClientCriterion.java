/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.dd.client.criteria;

import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.COMPONENT;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.COMPONENT_COUNT;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.MODE;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.PREFIX_MODE;
import static org.eclipse.hawkbit.ui.dd.criteria.AcceptCriteriaConstants.STRICT_MODE;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.hawkbit.ui.dd.criteria.ServerItemIdClientCriterion;

import com.vaadin.client.UIDL;
import com.vaadin.client.ui.dd.VAcceptCriterion;
import com.vaadin.client.ui.dd.VDragEvent;
import com.vaadin.shared.ui.dd.AcceptCriterion;

/**
 * Client-side accept criterion used to verify if the selected drag source is
 * valid for the proposed drop target on an id base. Valid drag sources are
 * pre-configured for the criterion. If one pre-configured value matches the
 * selected drag source, the drag source is accepted. The match mode is
 * configurable and may either be STRICT or PREFIX.
 */
@AcceptCriterion(ServerItemIdClientCriterion.class)
public final class ItemIdClientCriterion extends VAcceptCriterion {

    private static final Logger LOGGER = Logger.getLogger(ItemIdClientCriterion.class.getName());

    @Override
    // Exception squid:S1166 - Hide origin exception
    // Exception squid:S2221 - This code is trans-coded to JavaScript, hence
    // Exception semantics changes
    @SuppressWarnings({ "squid:S1166", "squid:S2221" })
    protected boolean accept(VDragEvent drag, UIDL configuration) {
        try {

            String component = drag.getTransferable().getDragSource().getWidget().getElement().getId();
            int c = configuration.getIntAttribute(COMPONENT_COUNT);
            String mode = configuration.getStringAttribute(MODE);
            for (int dragSourceIndex = 0; dragSourceIndex < c; dragSourceIndex++) {
                String requiredPid = configuration.getStringAttribute(COMPONENT + dragSourceIndex);
                if ((STRICT_MODE.equals(mode) && component.equals(requiredPid))
                        || (PREFIX_MODE.equals(mode) && component.startsWith(requiredPid))) {
                    return true;
                }

            }
        } catch (Exception e) {
            // log and continue
            LOGGER.log(Level.SEVERE, "Error verifying drop target: " + e.getLocalizedMessage());
        }
        return false;
    }
}
