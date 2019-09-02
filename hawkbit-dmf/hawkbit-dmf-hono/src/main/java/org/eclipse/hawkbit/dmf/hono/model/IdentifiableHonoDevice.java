package org.eclipse.hawkbit.dmf.hono.model;

public class IdentifiableHonoDevice {
    private String id;
    private String tenant;
    private HonoDevice device;

    public String getId() {
        return id;
    }

    public String getTenant() {
        return tenant;
    }

    public HonoDevice getDevice() {
        return device;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public void setDevice(HonoDevice device) {
        this.device = device;
    }
}
