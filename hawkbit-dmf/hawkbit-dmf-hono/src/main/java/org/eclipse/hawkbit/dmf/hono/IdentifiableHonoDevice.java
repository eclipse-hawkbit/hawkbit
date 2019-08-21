package org.eclipse.hawkbit.dmf.hono;

public class IdentifiableHonoDevice {
    private String id;
    private String tenant;
    private HonoDevice device;

    public String getId() {
        return id;
    }

    String getTenant() {
        return tenant;
    }

    public HonoDevice getDevice() {
        return device;
    }

    public void setId(String id) {
        this.id = id;
    }

    void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public void setDevice(HonoDevice device) {
        this.device = device;
    }
}
