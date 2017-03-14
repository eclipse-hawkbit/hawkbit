/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.customrenderers.renderers;

import com.vaadin.ui.Grid.AbstractRenderer;

/**
 *
 * Renders label with provided value and style.
 *
 */
public class HtmlLabelRenderer extends AbstractRenderer<String> {

    private static final long serialVersionUID = -7675588068526774915L;

    /**
     * Creates a new text renderer
     */
    public HtmlLabelRenderer() {
        super(String.class, null);
    }
}
