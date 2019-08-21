package org.eclipse.hawkbit.dmf.hono;

import com.fasterxml.jackson.databind.JsonNode;

class HonoDevice {
    private boolean enabled;
    private JsonNode ext;

    boolean isEnabled() {
        return enabled;
    }

    Object getExt() {
        return ext;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    void setExt(JsonNode ext) {
        this.ext = ext;
    }
}
