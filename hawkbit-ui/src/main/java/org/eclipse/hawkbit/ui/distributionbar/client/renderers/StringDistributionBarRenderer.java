package org.eclipse.hawkbit.ui.distributionbar.client.renderers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.vaadin.alump.distributionbar.gwt.client.GwtDistributionBar;

import com.google.gwt.core.client.GWT;
import com.vaadin.client.renderers.WidgetRenderer;
import com.vaadin.client.widget.grid.RendererCellReference;

public class StringDistributionBarRenderer extends WidgetRenderer<String, GwtDistributionBar> {

    @Override
    public GwtDistributionBar createWidget() {
        return GWT.create(GwtDistributionBar.class);
    }

    @Override
    public void render(RendererCellReference cell, String input, GwtDistributionBar widget) {
        if (null != input) {
            widget.setNumberOfParts(2);
            widget.addStyleName("status-bar");
            Map<String, Long> map = formatData(input);
            if (!map.isEmpty()) {
                if (isNoTargets(map.values())) {
                    setBarPartSize(widget, "SCHEDULED".toLowerCase(), 0, 0);
                    setBarPartSize(widget, "FINISHED".toLowerCase(), 0, 1);

                } else {
                    widget.setNumberOfParts(getNumberOfParts(map.values()));
                    int index = 0;
                    for (Entry<String, Long> entryVal : map.entrySet()) {
                        Long count = entryVal.getValue();
                        if (count > 0) {
                            setBarPartSize(widget, entryVal.getKey().toLowerCase(), count.intValue(), index);
                            index++;
                        }
                    }
                }
            }
            widget.updateParts();
        }
    }

    private Map<String, Long> formatData(String input) {
        Map<String, Long> details = new HashMap<>();
        String[] tempData = input.split(",");
        for (String statusWithCount : tempData) {
            String[] statusWithCountList = statusWithCount.split(":");
            details.put(statusWithCountList[0], new Long(statusWithCountList[1]));
        }
        return details;
    }

    private int getNumberOfParts(Collection<Long> values) {
        int count = 0;
        for (Long val : values) {
            if (val != 0) {
                count++;
            }
        }
        return count;
    }

    private boolean isNoTargets(Collection<Long> values) {
        for (Long count : values) {
            if (count != 0) {
                return false;
            }
        }
        return true;
    }


    private void setBarPartSize(final GwtDistributionBar bar, final String statusName, final int count,
            final int index) {
        bar.setPartSize(index, count);
        bar.setPartTooltip(index, statusName);
        bar.setPartStyleName(index, index, "status-bar-part-" + statusName);
    }

}