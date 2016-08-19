/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.builder;

import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Label Builder.
 *
 */
public class LabelBuilder {

    private String name;

    private String id;

    private boolean visible = true;

    /**
     * @param name
     *            the name to set
     * @return builder
     */
    public LabelBuilder name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @param id
     *            the id to set
     * @return builder
     */
    public LabelBuilder id(final String id) {
        this.id = id;
        return this;
    }

    /**
     * @param visible
     *            the visible to set
     * @return builder
     */
    public LabelBuilder visible(final boolean visible) {
        this.visible = visible;
        return this;
    }

    /**
     * Build caption label.
     * 
     * @return Label
     */
    public Label buildCaptionLabel() {
        final Label label = createLabel();
        label.setValue(name);
        label.addStyleName("header-caption");
        return label;
    }

    /**
     * Build label.
     * 
     * @return Label
     */
    public Label buildLabel() {
        final Label label = createLabel();
        label.setImmediate(false);
        label.setWidth("-1px");
        label.setHeight("-1px");

        return label;
    }

    private Label createLabel() {
        final Label label = new Label(name);
        label.setVisible(visible);
        final StringBuilder style = new StringBuilder(ValoTheme.LABEL_SMALL);
        style.append(' ');
        style.append(ValoTheme.LABEL_BOLD);
        label.addStyleName(style.toString());
        if (id != null) {
            label.setId(id);
        }

        return label;
    }

}
