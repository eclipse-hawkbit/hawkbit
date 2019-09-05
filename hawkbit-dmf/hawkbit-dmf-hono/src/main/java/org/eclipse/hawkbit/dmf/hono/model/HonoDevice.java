/**
 * Copyright (c) 2019 Kiwigrid GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.dmf.hono.model;

import com.fasterxml.jackson.databind.JsonNode;

public class HonoDevice {
    private boolean enabled;
    private JsonNode ext;

    public boolean isEnabled() {
        return enabled;
    }

    public Object getExt() {
        return ext;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setExt(JsonNode ext) {
        this.ext = ext;
    }
}
