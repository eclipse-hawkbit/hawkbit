/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import org.eclipse.hawkbit.ui.artifacts.smtable.ProxyBaseSoftwareModuleItem;

/**
 * Proxy for software module to display details in Software modules table.
 * 
 *
 *
 *
 */
public class ProxyBaseSwModuleItem extends ProxyBaseSoftwareModuleItem {

    private static final long serialVersionUID = -1555306616599140635L;

    private String colour;

    private Long typeId;

    public String getColour() {
        return colour;
    }

    public void setColour(final String colour) {
        this.colour = colour;
    }

    public Long getTypeId() {
        return typeId;
    }

    public void setTypeId(final Long typeId) {
        this.typeId = typeId;
    }

}
