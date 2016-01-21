/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.decorators;

import com.vaadin.server.ThemeResource;
import com.vaadin.ui.Embedded;

/**
 * Embedded with required style.
 * 
 *
 *
 */
public final class SPUIEmbedDecorator {

    /**
     * Private Constructor.
     */
    private SPUIEmbedDecorator() {

    }

    /**
     * Decorate.
     * 
     * @param spUIEmbdValue
     *            as DTO
     * @return Embedded as UI
     */
    public static Embedded decorate(final SPUIEmbedValue spUIEmbdValue) {
        final Embedded spUIEmbd = new Embedded();
        spUIEmbd.setImmediate(spUIEmbdValue.isImmediate());
        spUIEmbd.setType(spUIEmbdValue.getType());

        if (null != spUIEmbdValue.getId()) {
            spUIEmbd.setId(spUIEmbdValue.getId());
        }

        if (null != spUIEmbdValue.getData()) {
            spUIEmbd.setData(spUIEmbdValue.getData());
        }

        if (null != spUIEmbdValue.getStyleName()) {
            spUIEmbd.setStyleName(spUIEmbdValue.getStyleName());
        }

        if (null != spUIEmbdValue.getSource()) {
            spUIEmbd.setSource(new ThemeResource(spUIEmbdValue.getSource()));
        }

        if (null != spUIEmbdValue.getMimeType()) {
            spUIEmbd.setMimeType(spUIEmbdValue.getMimeType());
        }

        if (null != spUIEmbdValue.getDescription()) {
            spUIEmbd.setDescription(spUIEmbdValue.getDescription());
        }

        return spUIEmbd;
    }

}
