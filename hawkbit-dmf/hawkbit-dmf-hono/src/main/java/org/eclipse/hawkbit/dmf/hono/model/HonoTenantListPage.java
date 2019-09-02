package org.eclipse.hawkbit.dmf.hono.model;

import java.util.List;

public class HonoTenantListPage {
    private long total;
    private List<IdentifiableHonoTenant> items;

    public long getTotal() {
        return total;
    }

    public List<IdentifiableHonoTenant> getItems() {
        return items;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public void setItems(List<IdentifiableHonoTenant> items) {
        this.items = items;
    }
}
