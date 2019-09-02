package org.eclipse.hawkbit.dmf.hono.model;

public class IdentifiableHonoTenant {
    private String id;
    private HonoTenant tenant;

    public String getId() {
        return id;
    }

    public HonoTenant getTenant() {
        return tenant;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTenant(HonoTenant tenant) {
        this.tenant = tenant;
    }
}
