/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
