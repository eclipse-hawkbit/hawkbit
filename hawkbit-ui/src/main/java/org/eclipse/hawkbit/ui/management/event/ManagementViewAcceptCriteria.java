/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.event;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.ui.common.AbstractAcceptCriteria;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Component;

/**
 * Management View for Accept criteria.
 * 
 *
 * 
 */
@SpringComponent
@ViewScope
public class ManagementViewAcceptCriteria extends AbstractAcceptCriteria {

    private static final long serialVersionUID = 1718217664674701006L;

    private static final Map<String, List<String>> DROP_CONFIGS = createDropConfigurations();

    private static final Map<String, Object> DROP_HINTS_CONFIGS = createDropHintConfigurations();

    @Override
    protected String getComponentId(final Component component) {
        String id = component.getId();
        if (isTargetTagId(component.getId())) {
            id = SPUIDefinitions.TARGET_TAG_ID_PREFIXS;
        } else if (isDistributionTagId(component.getId())) {
            id = SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS;
        }
        return id;
    }

    @Override
    protected Map<String, Object> getDropHintConfigurations() {
        return DROP_HINTS_CONFIGS;
    }

    @Override
    protected Map<String, List<String>> getDropConfigurations() {
        return DROP_CONFIGS;
    }

    private boolean isDistributionTagId(final String id) {
        return id != null && id.startsWith(SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS);
    }

    private boolean isTargetTagId(final String id) {
        return id != null && id.startsWith(SPUIDefinitions.TARGET_TAG_ID_PREFIXS);
    }

    private static Map<String, List<String>> createDropConfigurations() {
        final Map<String, List<String>> config = new HashMap<>();

        // Delete drop area acceptable components
        config.put(UIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID,
                Arrays.asList(SPUIDefinitions.TARGET_TAG_ID_PREFIXS, UIComponentIdProvider.TARGET_TABLE_ID,
                        SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS, UIComponentIdProvider.DIST_TABLE_ID));

        // Target Tag acceptable components
        config.put(SPUIDefinitions.TARGET_TAG_ID_PREFIXS, Arrays.asList(UIComponentIdProvider.TARGET_TABLE_ID));

        // Target table acceptable components
        config.put(UIComponentIdProvider.TARGET_TABLE_ID,
                Arrays.asList(SPUIDefinitions.TARGET_TAG_ID_PREFIXS, UIComponentIdProvider.DIST_TABLE_ID));

        // Target table header acceptable components
        config.put(UIComponentIdProvider.TARGET_DROP_FILTER_ICON, Arrays.asList(UIComponentIdProvider.DIST_TABLE_ID));

        // Distribution table acceptable components
        config.put(UIComponentIdProvider.DIST_TABLE_ID, Arrays.asList(SPUIDefinitions.TARGET_TAG_ID_PREFIXS,
                UIComponentIdProvider.TARGET_TABLE_ID, SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS));

        // Distribution tag acceptable components.
        config.put(SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS, Arrays.asList(UIComponentIdProvider.DIST_TABLE_ID));
        return config;
    }

    private static Map<String, Object> createDropHintConfigurations() {
        final Map<String, Object> config = new HashMap<>();
        config.put(SPUIDefinitions.TARGET_TAG_ID_PREFIXS, DragEvent.TARGET_TAG_DRAG);
        config.put(UIComponentIdProvider.TARGET_TABLE_ID, DragEvent.TARGET_DRAG);
        config.put(UIComponentIdProvider.DIST_TABLE_ID, DragEvent.DISTRIBUTION_DRAG);
        config.put(SPUIDefinitions.DISTRIBUTION_TAG_ID_PREFIXS, DragEvent.DISTRIBUTION_TAG_DRAG);
        return config;
    }
}
