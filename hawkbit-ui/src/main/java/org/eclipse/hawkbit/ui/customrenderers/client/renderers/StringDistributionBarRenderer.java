package org.eclipse.hawkbit.ui.customrenderers.client.renderers;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.vaadin.alump.distributionbar.gwt.client.GwtDistributionBar;
import org.vaadin.alump.distributionbar.gwt.client.dom.ToolTipPresenter.TooltipClassNameProvider;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.vaadin.client.renderers.WidgetRenderer;
import com.vaadin.client.widget.grid.RendererCellReference;

public class StringDistributionBarRenderer extends WidgetRenderer<String, GwtDistributionBar> {

    private static final String STATUS_BAR_PART = "status-bar-part-";
    private static final String FINISHED = "FINISHED";
    private static final String SCHEDULED = "SCHEDULED";
    private static final String ROLLOUT_STATUS_PROGRESS_BAR_ID = "rollout.status.progress.bar.id";
    private String uiWidgetClassName;

    @Override
    public GwtDistributionBar createWidget() {
        GwtDistributionBar gwt = GWT.create(GwtDistributionBar.class);
        gwt.setTooltipClassNameProvider(new TooltipClassNameProvider() {
            @Override
            public String getClassNames() {
                return getUiWidgetClassName();
            }
        });
        return gwt;
    }

    
    @Override
    public void render(RendererCellReference cell, String input, GwtDistributionBar widget) {
        if (null != input) {
            widget.setNumberOfParts(2);
            Map<String, Long> map = formatData(input);
            if (!map.isEmpty()) {
                if (isNoTargets(map.values())) {
                    setBarPartSize(widget, SCHEDULED.toLowerCase(), 0, 0);
                    setBarPartSize(widget, FINISHED.toLowerCase(), 0, 1);

                } else {
                    setThePartDetails(widget, map);
                }
            }
            widget.getElement().setId(ROLLOUT_STATUS_PROGRESS_BAR_ID);
            widget.getElement().getStyle().setWidth(100, Unit.PCT);
            widget.getElement().getStyle().setHeight(100, Unit.PCT);
        }
        widget.updateParts();
    }

    private void setThePartDetails(GwtDistributionBar widget, Map<String, Long> map) {
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
        bar.setPartStyleName(index, index, STATUS_BAR_PART + statusName);
    }

    public String getUiWidgetClassName() {
        return uiWidgetClassName;
    }

    public void setUiWidgetClassName(String uiWidgetClassName) {
        this.uiWidgetClassName = uiWidgetClassName;
    }

}