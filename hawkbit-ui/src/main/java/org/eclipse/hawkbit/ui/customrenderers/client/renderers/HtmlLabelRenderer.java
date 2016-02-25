package org.eclipse.hawkbit.ui.customrenderers.client.renderers;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.vaadin.client.renderers.WidgetRenderer;
import com.vaadin.client.ui.VLabel;
import com.vaadin.client.widget.grid.RendererCellReference;

public class HtmlLabelRenderer extends WidgetRenderer<String, VLabel> {

    @Override
    public VLabel createWidget() {
        return GWT.create(VLabel.class);
    }

    @Override
    public void render(RendererCellReference cell, String input, VLabel label) {
        Map<String, String> map = formatInput(input);
        String value = map.containsKey("value") ? map.get("value") : null;
        String style = map.containsKey("style") ? map.get("style") : null;
        String id = map.containsKey("id") ? map.get("id") : null;

        if (value != null) {
            label.setHTML("<span>&#x" + Integer.toHexString(Integer.parseInt(value)) + ";</span>");
        }
        else{
            label.setHTML("<span></span>");
        }
        applyStyle(label, style);
        label.getElement().setId(id);
    }

    private void applyStyle(VLabel label, String style) {
        label.setStylePrimaryName("v-label");
        label.setStyleName("small v-label-small");
        label.addStyleName("font-icon v-label-font-icon");
        if (style != null) {
            label.addStyleName(style + " v-label-" + style);
        }
    }

    private Map<String, String> formatInput(String input) {
        Map<String, String> details = new HashMap<>();
        String[] tempData = input.split(",");
        for (String statusWithCount : tempData) {
            String[] statusWithCountList = statusWithCount.split(":");
            details.put(statusWithCountList[0], statusWithCountList[1]);
        }
        return details;
    }

}
