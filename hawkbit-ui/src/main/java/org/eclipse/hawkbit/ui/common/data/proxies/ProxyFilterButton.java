/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
