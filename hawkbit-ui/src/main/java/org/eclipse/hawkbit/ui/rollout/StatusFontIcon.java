/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import java.io.Serializable;

import com.vaadin.server.FontAwesome;

/**
 * Helper class which holds the details of font icon to be displayed as
 * label/button in grid:
 * <p>
 * <code>RolloutListGrid</code> / <code>RolloutGroupListGrid</code> /
 * <code>RolloutGroupTargetsListGrid</code> / <code>ActionHistoryGrid</code>
 */
public class StatusFontIcon implements Serializable {
    /** serialVersionUID. */
    private static final long serialVersionUID = 1L;
    private FontAwesome fontIcon;
    private String style;
    private String title;
    private String id;
    private boolean disabled;

    /**
     * NOTE: This constructor is used for (de-)serialization only!!!
     */
    public StatusFontIcon() {
        // empty
    }

    /**
     * Constructor to create icon metadata object.
     *
     * @param fontIcon
     *            the font representing the icon
     * @param style
     *            the style
     */
    public StatusFontIcon(final FontAwesome fontIcon, final String style) {
        this(fontIcon, style, "", "", false);
    }

    /**
     * Constructor to create icon metadata object.
     *
     * @param fontIcon
     *            the font representing the icon
     * @param style
     *            the style
     * @param title
     *            the title shown as tooltip
     */
    public StatusFontIcon(final FontAwesome fontIcon, final String style, final String title) {
        this(fontIcon, style, title, "", false);
    }

    /**
     * Constructor to create icon metadata object.
     *
     * @param fontIcon
     *            the font representing the icon
     * @param style
     *            the style
     * @param title
     *            the title shown as tooltip
     * @param id
     *            the id for direct access
     */
    public StatusFontIcon(final FontAwesome fontIcon, final String style, final String title, final String id) {
        this(fontIcon, style, title, id, false);
    }

    /**
     * Constructor to create icon metadata object.
     *
     * @param fontIcon
     *            the font representing the icon
     * @param style
     *            the style
     * @param title
     *            the title shown as tooltip
     * @param id
     *            the id for direct access
     * @param disabled
     *            disabled-state of the icon
     */
    public StatusFontIcon(final FontAwesome fontIcon, final String style, final String title, final String id,
            final boolean disabled) {
        this.fontIcon = fontIcon;
        this.style = style;
        this.title = title;
        this.id = id;
        this.disabled = disabled;
    }

    /**
     * Gets the font representing the icon.
     *
     * @return the font representing the icon
     */
    public FontAwesome getFontIcon() {
        return fontIcon;
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
     * Gets the title shown as tooltip.
     *
     * @return the title shown as tooltip.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the id for direct access.
     *
     * @return the id for direct access.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the disabled-state of the icon.
     *
     * @return the disabled-state of the icon.
     */
    public boolean isDisabled() {
        return disabled;
    }
}
