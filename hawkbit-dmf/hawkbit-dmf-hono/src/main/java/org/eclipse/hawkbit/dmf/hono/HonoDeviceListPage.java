package org.eclipse.hawkbit.dmf.hono;

import java.util.List;

public class HonoDeviceListPage {
    private long total;
    private List<IdentifiableHonoDevice> items;

    public long getTotal() {
        return total;
    }

    public List<IdentifiableHonoDevice> getItems() {
        return items;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public void setItems(List<IdentifiableHonoDevice> items) {
        this.items = items;
    }
}
