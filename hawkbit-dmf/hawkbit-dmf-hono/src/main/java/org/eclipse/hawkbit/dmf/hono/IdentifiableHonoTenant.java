package org.eclipse.hawkbit.dmf.hono;

class IdentifiableHonoTenant {
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
