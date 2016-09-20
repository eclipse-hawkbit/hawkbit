/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.event;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.hawkbit.ui.common.AbstractAcceptCriteria;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;

import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Component;

/**
 * Upload UI View for Accept criteria.
 * 
 */
@SpringComponent
@ViewScope
public class UploadViewAcceptCriteria extends AbstractAcceptCriteria {

    private static final long serialVersionUID = 5158811326115667378L;

    private static final Map<String, List<String>> DROP_CONFIGS = createDropConfigurations();

    private static final Map<String, Object> DROP_HINTS_CONFIGS = createDropHintConfigurations();

    @Override
    protected String getComponentId(final Component component) {
        String id = component.getId();
        if (id != null && id.startsWith(SPUIComponentIdProvider.UPLOAD_TYPE_BUTTON_PREFIX)) {
            id = SPUIComponentIdProvider.UPLOAD_TYPE_BUTTON_PREFIX;
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

    private static Map<String, List<String>> createDropConfigurations() {
        final Map<String, List<String>> config = Maps.newHashMapWithExpectedSize(1);
        // Delete drop area droppable components
        config.put(SPUIComponentIdProvider.DELETE_BUTTON_WRAPPER_ID,
                Arrays.asList(SPUIComponentIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE,
                        SPUIComponentIdProvider.UPLOAD_TYPE_BUTTON_PREFIX));

        return config;
    }

    private static Map<String, Object> createDropHintConfigurations() {
        final Map<String, Object> config = Maps.newHashMapWithExpectedSize(2);
        config.put(SPUIComponentIdProvider.UPLOAD_TYPE_BUTTON_PREFIX, UploadArtifactUIEvent.SOFTWARE_TYPE_DRAG_START);
        config.put(SPUIComponentIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE, UploadArtifactUIEvent.SOFTWARE_DRAG_START);
        return config;
    }
}
