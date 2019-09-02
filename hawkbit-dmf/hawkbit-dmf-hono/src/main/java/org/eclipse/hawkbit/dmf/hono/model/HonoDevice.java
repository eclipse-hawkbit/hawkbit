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
