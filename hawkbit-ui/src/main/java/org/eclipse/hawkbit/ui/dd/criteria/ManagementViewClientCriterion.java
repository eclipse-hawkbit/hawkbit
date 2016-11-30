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
 * Management UI View for Accept criteria.
 *
 */
@SpringComponent
@UIScope
public final class ManagementViewClientCriterion extends ServerViewClientCriterion {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 927303094996626180L;

    private static final ServerViewComponentClientCriterion[] COMPONENT_CRITERIA = createViewComponentClientCriteria();

    /**
     * Constructor.
     */
    public ManagementViewClientCriterion() {
        super(COMPONENT_CRITERIA);
    }

    /**
     * Configures the elements of the composite accept criterion for the
     * Management View.
     *
     * @return accept criterion elements
     */
    static ServerViewComponentClientCriterion[] createViewComponentClientCriteria() {
        ServerViewComponentClientCriterion[] criteria = new ServerViewComponentClientCriterion[4];

        // Target table acceptable components.
        criteria[0] = ServerViewComponentClientCriterion.createBuilder()
                .dragSourceIdPrefix(UIComponentIdProvider.TARGET_TABLE_ID)
                .dropTargetIdPrefixes(SPUIDefinitions.TARGET_TAG_ID_PREFIXS, UIComponentIdProvider.DIST_TABLE_ID,
                        UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID)
                .dropAreaIds(UIComponentIdProvider.TARGET_TAG_DROP_AREA_ID, UIComponentIdProvider.DIST_TABLE_ID,
                        UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID)
                .build();
        // Target Tag acceptable components.
        criteria[1] = ServerViewComponentClientCriterion.createBuilder()
                .dragSourceIdPrefix(SPUIDefinitions.TARGET_TAG_ID_PREFIXS)
                .dropTargetIdPrefixes(UIComponentIdProvider.TARGET_TABLE_ID, UIComponentIdProvider.DIST_TABLE_ID,
                        UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID)
                .dropAreaIds(UIComponentIdProvider.TARGET_TABLE_ID, UIComponentIdProvider.DIST_TABLE_ID,
                        UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID)
                .build();
        // Distribution table acceptable components.
        criteria[2] = ServerViewComponentClientCriterion.createBuilder()
                .dragSourceIdPrefix(UIComponentIdProvider.DIST_TABLE_ID)
                .dropTargetIdPrefixes(UIComponentIdProvider.TARGET_TABLE_ID,
                        UIComponentIdProvider.TARGET_DROP_FILTER_ICON, SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS,
                        UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID)
                .dropAreaIds(UIComponentIdProvider.TARGET_TABLE_ID, UIComponentIdProvider.TARGET_DROP_FILTER_ICON,
                        UIComponentIdProvider.DISTRIBUTION_TAG_TABLE_ID, UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID)
                .build();
        // Distribution tag acceptable components.
        criteria[3] = ServerViewComponentClientCriterion.createBuilder()
                .dragSourceIdPrefix(SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS)
                .dropTargetIdPrefixes(UIComponentIdProvider.DIST_TABLE_ID,
                        UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID)
                .dropAreaIds(UIComponentIdProvider.DIST_TABLE_ID, UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID)
                .build();

        return criteria;
    }
}
