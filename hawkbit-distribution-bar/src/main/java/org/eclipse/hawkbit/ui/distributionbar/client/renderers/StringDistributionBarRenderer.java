package org.eclipse.hawkbit.ui.distributionbar.client.renderers;

import java.util.HashMap;
import java.util.Map;

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
            Map<String, Long> map = formatData(input);
            if (!map.isEmpty()) {
                final Long notStartedTargetsCount = map.keySet().contains("NOTSTARTED") ? map.get("NOTSTARTED") : 0L;
                final Long runningTargetsCount = map.keySet().contains("RUNNING") ? map.get("RUNNING") : 0L;
                final Long scheduledTargetsCount = map.keySet().contains("SCHEDULED") ? map.get("SCHEDULED") : 0L;
                final Long errorTargetsCount = map.keySet().contains("ERROR") ? map.get("ERROR") : 0L;
                final Long finishedTargetsCount = map.keySet().contains("FINISHED") ? map.get("FINISHED") : 0L;
                final Long cancelledTargetsCount = map.keySet().contains("CANCELLED") ? map.get("CANCELLED") : 0L;
                if (isNoTargets(errorTargetsCount, notStartedTargetsCount, runningTargetsCount, scheduledTargetsCount,
                        finishedTargetsCount, cancelledTargetsCount)) {
                    setBarPartSize(widget, "SCHEDULED".toLowerCase(), 0, 0);
                    setBarPartSize(widget, "FINISHED".toString().toLowerCase(), 0, 1);

                } else {
                    widget.setNumberOfParts(6);
                    setBarPartSize(widget, "NOTSTARTED".toString().toLowerCase(), notStartedTargetsCount.intValue(), 0);
                    setBarPartSize(widget, "SCHEDULED".toString().toLowerCase(), scheduledTargetsCount.intValue(), 1);
                    setBarPartSize(widget, "RUNNING".toLowerCase(), scheduledTargetsCount.intValue(), 2);
                    setBarPartSize(widget, "ERROR".toLowerCase(), errorTargetsCount.intValue(), 3);
                    setBarPartSize(widget, "FINISHED".toLowerCase(), finishedTargetsCount.intValue(), 4);
                    setBarPartSize(widget, "CANCELLED".toLowerCase(), cancelledTargetsCount.intValue(), 5);
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

    private static boolean isNoTargets(Long errorTargetsCount, Long notStartedTargetsCount, Long runningTargetsCount,
            Long scheduledTargetsCount, Long finishedTargetsCount, Long cancelledTargetsCount) {
        if (errorTargetsCount == 0 && notStartedTargetsCount == 0 && runningTargetsCount == 0
                && scheduledTargetsCount == 0 && finishedTargetsCount == 0 && cancelledTargetsCount == 0) {
            return true;
        }
        return false;
    }

    public void setBarPartSize(final GwtDistributionBar bar, final String statusName, final int count,
            final int index) {
        bar.setPartSize(index, count);
        bar.setPartTooltip(index, statusName);
        // check thi:::
        bar.setPartStyleName(index, index, "status-bar-part-" + statusName);
    }

}