/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

/**
 * Proxy for filter buttons layouts (e.g. tags, types).
 */
public class ProxyFilterButton extends ProxyNamedEntity {
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_COLOR = "#2c9720";

    private String colour;

    /**
     * Constructor to set default colour of button
     */
    public ProxyFilterButton() {
        this.colour = DEFAULT_COLOR;
    }

    /**
     * Gets the button colour
     *
     * @return colour
     */
    public String getColour() {
        return colour;
    }

    /**
     * Sets the colour
     *
     * @param colour
     *          Button color
     */
    public void setColour(final String colour) {
        this.colour = colour;
    }

}
