/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.ui.customrenderers.renderers.AbstractHtmlLabelConverter;

/**
 * Concrete html-label converter that handles Action.Status.
 */
public class HtmlStatusLabelConverter extends AbstractHtmlLabelConverter<Action.Status> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor that sets the appropriate adapter.
     *
     * @param adapter
     *            adapts <code>Action.Status</code> to <code>String</code>
     */
    public HtmlStatusLabelConverter(LabelAdapter<Action.Status> adapter) {
        this.addAdapter(adapter);
    }

    @Override
    public Class<Action.Status> getModelType() {
        return Action.Status.class;
    }
}
