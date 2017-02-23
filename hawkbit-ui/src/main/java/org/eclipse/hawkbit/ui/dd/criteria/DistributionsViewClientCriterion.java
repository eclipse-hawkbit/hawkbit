/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.dd.criteria;

import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;

/**
 * Distribution UI View for Accept criteria.
 *
 */
@SpringComponent
@UIScope
public final class DistributionsViewClientCriterion extends ServerViewClientCriterion {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 8602945302654554257L;

    private static final ServerViewComponentClientCriterion[] COMPONENT_CRITERIA = createViewComponentClientCriteria();

    /**
     * Constructor.
     */
    public DistributionsViewClientCriterion() {
        super(COMPONENT_CRITERIA);
    }

    /**
     * Configures the elements of the composite accept criterion for the
     * Distributions View.
     *
     * @return accept criterion elements
     */
    static ServerViewComponentClientCriterion[] createViewComponentClientCriteria() {
        ServerViewComponentClientCriterion[] criteria = new ServerViewComponentClientCriterion[4];

        // Distribution table acceptable components.
        criteria[0] = ServerViewComponentClientCriterion.createBuilder()
                .dragSourceIdPrefix(UIComponentIdProvider.DIST_TABLE_ID)
                .dropTargetIdPrefixes(UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID)
                .dropAreaIds(UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID).build();
        // Distribution set type acceptable components.
        criteria[1] = ServerViewComponentClientCriterion.createBuilder()
                .dragSourceIdPrefix(SPUIDefinitions.DISTRIBUTION_SET_TYPE_ID_PREFIXS)
                .dropTargetIdPrefixes(UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID)
                .dropAreaIds(UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID).build();
        // Upload software module table acceptable components.
        criteria[2] = ServerViewComponentClientCriterion.createBuilder()
                .dragSourceIdPrefix(UIComponentIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE)
                .dropTargetIdPrefixes(UIComponentIdProvider.DIST_TABLE_ID,
                        UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID)
                .dropAreaIds(UIComponentIdProvider.DIST_TABLE_ID, UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID)
                .build();
        // Software module tag acceptable components.
        criteria[3] = ServerViewComponentClientCriterion.createBuilder()
                .dragSourceIdPrefix(SPUIDefinitions.SOFTWARE_MODULE_TAG_ID_PREFIXS)
                .dropTargetIdPrefixes(UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID)
                .dropAreaIds(UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID).build();

        return criteria;
    }
}
