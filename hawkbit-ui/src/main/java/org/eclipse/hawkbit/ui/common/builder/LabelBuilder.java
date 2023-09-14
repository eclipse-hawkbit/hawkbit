/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.builder;

import org.springframework.util.StringUtils;

import com.vaadin.ui.Label;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Label Builder.
 *
 */
public class LabelBuilder {

    private String caption;

    private String name;

    private String id;

    private boolean visible = true;

    /**
     * Add name to label
     *
     * @param name
     *            the value to set
     * @return builder
     */
    public LabelBuilder name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * Add caption to label
     *
     * @param caption
     *            the caption to set
     * @return builder
     */
    public LabelBuilder caption(final String caption) {
        this.caption = caption;
        return this;
    }

    /**
     * Add id to label
     *
     * @param id
     *            the id to set
     * @return builder
     */
    public LabelBuilder id(final String id) {
        this.id = id;
        return this;
    }

    /**
     * Toggle label visibility
     *
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
        label.setWidth("-1px");
        label.setHeight("-1px");
        if (StringUtils.hasText(caption)) {
            label.setCaption(caption);
        }

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
