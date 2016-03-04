/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.customrenderers.renderers;

import com.vaadin.ui.renderers.ButtonRenderer;

public class LinkRenderer extends ButtonRenderer {
    private static final long serialVersionUID = -1242995370043404892L;
    public LinkRenderer() {
        super();
    }
    public LinkRenderer(RendererClickListener listener) {
        super(listener);
    }
    
}