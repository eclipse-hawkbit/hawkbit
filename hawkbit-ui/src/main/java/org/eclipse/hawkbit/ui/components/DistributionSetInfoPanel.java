/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

/**
 * Distribution set panel for Target.
 */
@SuppressWarnings("serial")
public class DistributionSetInfoPanel extends Panel {

    /**
     * Parametric constructor.
     * 
     * @param distributionSet
     *            as DistributionSet
     * @param caption
     *            as String
     * @param style1
     *            as String
     * @param style2
     *            as String
     */
    DistributionSetInfoPanel(final DistributionSet distributionSet, final String caption, final String style1,
            final String style2) {
        setImmediate(false);
        decorate(distributionSet, caption, style1, style2);
    }

    /**
     * Decorate.
     * 
     * @param distributionSet
     *            as DistributionSet
     * @param caption
     *            as String
     * @param style1
     *            as String
     * @param style2
     *            as String
     */
    private void decorate(final DistributionSet distributionSet, final String caption, final String style1,
            final String style2) {
        final VaadinMessageSource i18n = SpringContextHelper.getBean(VaadinMessageSource.class);
        final VerticalLayout layout = new VerticalLayout();
        // Display distribution set name
        layout.addComponent(SPUIComponentProvider.createNameValueLabel(i18n.getMessage("label.dist.details.name"),
                distributionSet.getName(), distributionSet.getVersion()));

        /* Module info */
        distributionSet.getModules()
                .forEach(module -> layout.addComponent(getSWModlabel(module.getType().getName(), module)));

        layout.setSizeFull();
        layout.setMargin(false);
        layout.setSpacing(false);

        setContent(layout);
        // Decorate specific
        setCaption(caption);
        addStyleName(style1);
        addStyleName(style2);
        addStyleName("small");
        setImmediate(false);
    }

    /**
     * Create Label for SW Module.
     * 
     * @param labelName
     *            as Name
     * @param swModule
     *            as Module (JVM|OS|AH)
     * @return Label as UI
     */
    private Label getSWModlabel(final String labelName, final SoftwareModule swModule) {
        return SPUIComponentProvider.createNameValueLabel(labelName + " : ", swModule.getName(), swModule.getVersion());
    }
}
