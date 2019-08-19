package org.eclipse.hawkbit.dmf.hono;

import java.util.List;

class HonoTenantListPage {
    private long total;
    private List<IdentifiableHonoTenant> items;

    long getTotal() {
        return total;
    }

    List<IdentifiableHonoTenant> getItems() {
        return items;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public void setItems(List<IdentifiableHonoTenant> items) {
        this.items = items;
    }
}
