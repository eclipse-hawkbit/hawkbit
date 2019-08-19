package org.eclipse.hawkbit.dmf.hono;

import com.fasterxml.jackson.databind.JsonNode;

class HonoDevice {
    private String id;
    private String tenant;
    private boolean enabled;
    private JsonNode ext;

    String getId() {
        return id;
    }

    String getTenant() {
        return tenant;
    }

    boolean isEnabled() {
        return enabled;
    }

    Object getExt() {
        return ext;
    }

    void setId(String id) {
        this.id = id;
    }

    void setTenant(String tenant) {
        this.tenant = tenant;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    void setExt(JsonNode ext) {
        this.ext = ext;
    }
}
