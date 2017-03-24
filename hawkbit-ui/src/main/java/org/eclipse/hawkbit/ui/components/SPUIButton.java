/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import com.vaadin.server.Resource;
import com.vaadin.ui.Button;

/**
 * Basic button for SPUI. Any commonality can be decorated.
 */
public class SPUIButton extends Button {
    private static final long serialVersionUID = -7327726430436273739L;

    SPUIButton(final String id, final String buttonName, final String buttonDesc) {
        super(buttonName);
        setDescription(buttonDesc);
        setImmediate(false);
        if (null != id) {
            setId(id);
        }
    }

    /**
     * Toggle Icon on action.
     *
     * @param icon
     *            as Resource
     */
    public void toggleIcon(final Resource icon) {
        setIcon(icon);
    }
}
