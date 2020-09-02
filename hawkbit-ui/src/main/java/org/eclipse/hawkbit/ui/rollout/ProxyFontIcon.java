/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import com.cronutils.utils.StringUtils;
import com.vaadin.server.FontIcon;

/**
 * Helper class which holds the details of font icon to be displayed as
 * label/button in grid:
 * <p>
 * <code>RolloutListGrid</code> / <code>RolloutGroupListGrid</code> /
 * <code>RolloutGroupTargetsListGrid</code> / <code>ActionHistoryGrid</code>
 */
public class ProxyFontIcon implements FontIcon {
    private static final long serialVersionUID = 1L;

    private final FontIcon icon;
    private final String style;
    private final String description;
    private final String color;

    /**
     * Constructor to create icon metadata object.
     *
     * @param icon
     *            the font representing the icon
     */
    public ProxyFontIcon(final FontIcon icon) {
        this(icon, "");
    }

    /**
     * Constructor to create icon metadata object.
     *
     * @param icon
     *            the font representing the icon
     * @param style
     *            the style
     */
    public ProxyFontIcon(final FontIcon icon, final String style) {
        this(icon, style, "");
    }

    /**
     * Constructor to create icon metadata object.
     *
     * @param icon
     *            the font representing the icon
     * @param style
     *            the style
     * @param description
     *            the description shown as tooltip
     */
    public ProxyFontIcon(final FontIcon icon, final String style, final String description) {
        this(icon, style, description, "");
    }

    /**
     * Constructor to create icon metadata object.
     *
     * @param icon
     *            the font representing the icon
     * @param style
     *            the style
     * @param description
     *            the description shown as tooltip
     * @param color
     *            the color of the icon
     */
    public ProxyFontIcon(final FontIcon icon, final String style, final String description, final String color) {
        this.icon = icon;
        this.style = style;
        this.description = description;
        this.color = color;
    }

    /**
     * Gets the style.
     *
     * @return the style
     */
    public String getStyle() {
        return style;
    }

    /**
     * Gets the description shown as tooltip.
     *
     * @return the description shown as tooltip.
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String getMIMEType() {
        return icon.getMIMEType();
    }

    @Override
    public String getFontFamily() {
        return icon.getFontFamily();
    }

    @Override
    public int getCodepoint() {
        return icon.getCodepoint();
    }

    @Override
    public String getHtml() {
        String html = "";
        if (icon != null) {
            html = icon.getHtml();
        }
        if (!StringUtils.isEmpty(color)) {
            html = "<span style=\"color:" + color + " !important;\">" + html + "</span>";
        }
        return html;
    }
}
