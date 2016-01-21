/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.decorators;

import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Simple Decorator for the label.
 * 
 *
 * 
 *
 *
 */
public final class SPUILabelDecorator {

    /**
     * Private Constructor.
     */
    private SPUILabelDecorator() {

    }

    /**
     * Simple decorator.
     * 
     * @param name
     *            as String
     * @param type
     *            as String
     * @return Label
     */
    public static Label getDeocratedLabel(final String name, final String type) {
        final Label spUILabel = new Label(name);
        // Set style.
        final StringBuilder style = new StringBuilder(ValoTheme.LABEL_SMALL);
        style.append(' ');
        style.append(ValoTheme.LABEL_BOLD);
        spUILabel.addStyleName(style.toString());
        if (SPUILabelDefinitions.SP_WIDGET_CAPTION.equalsIgnoreCase(type)) {
            spUILabel.setValue(name);
            spUILabel.addStyleName("header-caption");
        } else if (SPUILabelDefinitions.SP_LABEL_MESSAGE.equalsIgnoreCase(type)) {
            spUILabel.setContentMode(ContentMode.HTML);
            spUILabel.addStyleName(SPUILabelDefinitions.SP_LABEL_MESSAGE_STYLE);
        } else {
            spUILabel.setImmediate(false);
            spUILabel.setWidth("-1px");
            spUILabel.setHeight("-1px");
        }

        return spUILabel;
    }

}
