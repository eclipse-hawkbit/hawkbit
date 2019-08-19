package org.eclipse.hawkbit.dmf.hono;

public class IdentifiableHonoDevice {
    private String id;
    private HonoDevice device;

    public String getId() {
        return id;
    }

    public HonoDevice getDevice() {
        return device;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDevice(HonoDevice device) {
        this.device = device;
    }
}
