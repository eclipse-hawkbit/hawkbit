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

/**
 * 
 * Renders button with provided HTML content. Used to display button with icons.
 *
 */
public class HtmlButtonRenderer extends ButtonRenderer {
    private static final long serialVersionUID = -1242995370043404892L;

<<<<<<< HEAD
    /**
     * Intialize button renderer.
     */
=======
>>>>>>> branch 'Rollout_migrate_table_to_grid_final' of https://github.com/bsinno/hawkbit
    public HtmlButtonRenderer() {
        super();
    }

<<<<<<< HEAD
    /**
     * Intialize button renderer with {@link RendererClickListener}
     * 
     * @param listener
     *            RendererClickListener
     */
=======
>>>>>>> branch 'Rollout_migrate_table_to_grid_final' of https://github.com/bsinno/hawkbit
    public HtmlButtonRenderer(RendererClickListener listener) {
        super(listener);
    }
}
