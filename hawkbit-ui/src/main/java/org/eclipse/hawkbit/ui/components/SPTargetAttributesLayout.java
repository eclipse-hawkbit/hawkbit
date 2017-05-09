/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import java.util.Map;

import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Attributes Vertical layout for Target.
 */
public class SPTargetAttributesLayout {
    private final VerticalLayout targetAttributesLayout;

    /**
     * Parametric constructor.
     *
     * @param controllerAttibs
     *            controller attributes
     *
     */
    SPTargetAttributesLayout(final Map<String, String> controllerAttibs) {
        targetAttributesLayout = new VerticalLayout();
        targetAttributesLayout.setSpacing(true);
        targetAttributesLayout.setMargin(true);
        decorate(controllerAttibs);
    }

    /**
     * Custom Decorate.
     *
     * @param controllerAttibs
     */
    private void decorate(final Map<String, String> controllerAttibs) {
        final VaadinMessageSource i18n = SpringContextHelper.getBean(VaadinMessageSource.class);
        final Label title = new Label(i18n.getMessage("label.target.controller.attrs"), ContentMode.HTML);
        title.addStyleName(SPUIDefinitions.TEXT_STYLE);
        targetAttributesLayout.addComponent(title);
        if (HawkbitCommonUtil.isNotNullOrEmpty(controllerAttibs)) {
            for (final Map.Entry<String, String> entry : controllerAttibs.entrySet()) {
                targetAttributesLayout.addComponent(
                        SPUIComponentProvider.createNameValueLabel(entry.getKey() + ": ", entry.getValue()));
            }
        }
    }

    /**
     * GET Target Attributes Layout.
     *
     * @return VerticalLayout as UI
     */
    public VerticalLayout getTargetAttributesLayout() {
        return targetAttributesLayout;
    }
}
