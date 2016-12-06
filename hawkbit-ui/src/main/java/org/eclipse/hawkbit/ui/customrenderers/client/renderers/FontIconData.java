/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.customrenderers.client.renderers;

import java.io.Serializable;

/**
 * Data class that transports the icon meta-data to client-side renderer.
 */
public class FontIconData implements Serializable {

    /** serialVersionUID. */
    private static final long serialVersionUID = 5823318280700107049L;
    private String fontIconHtml;
    private String style;
    private String title;
    private String id;
    private boolean disabled;

    /**
     * Gets the html representing the icon.
     *
     * @return the html representing the icon
     */
    public String getFontIconHtml() {
        return fontIconHtml;
    }

    /**
     * Sets the html representing the icon.
     *
     * @param fontIconHtml
     *            html representing the icon
     */
    public void setFontIconHtml(String fontIconHtml) {
        this.fontIconHtml = fontIconHtml;
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
     * Sets the style.
     *
     * @param style
     *            icon style
     */
    public void setStyle(String style) {
        this.style = style;
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
     * Sets the title shown as tooltip.
     *
     * @param title
     *            shown as tooltip.
     */
    public void setTitle(String title) {
        this.title = title;
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
     * Sets the id for direct access.
     *
     * @param id
     *            for direct access.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the disabled-state of the icon.
     *
     * @return the disabled-state of the icon.
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Sets the disabled-state of the icon.
     *
     * @param disabled
     *            disabled-state of the icon.
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
