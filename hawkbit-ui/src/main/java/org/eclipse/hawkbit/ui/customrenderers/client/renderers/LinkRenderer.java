package org.eclipse.hawkbit.ui.customrenderers.client.renderers;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.Button;
import com.vaadin.client.renderers.ButtonRenderer;
import com.vaadin.client.widget.grid.RendererCellReference;

public class LinkRenderer extends ButtonRenderer {
    @Override
    public void render(RendererCellReference cell, String text, Button button) {
        Map<String, String> nameStatusMap = formatNameStatusData(text);
        final String targetName = nameStatusMap.get("name");
        final String targetStatus = nameStatusMap.get("status");
        button.setText(targetName);
        applystyle(button);
        // this is to allow the button to disappear, if the text is null
        button.setVisible(targetName != null);
        button.getElement().setId(new StringBuilder("link").append(".").append(targetName).toString());

        if (null != targetStatus && targetStatus.equalsIgnoreCase("CREATING")) {
            button.getElement().setAttribute("enabled", "false");
        } else {
            button.getElement().setAttribute("enabled", "true");
            button.addStyleName("link v-button-link");
        }

    }

    private void applystyle(Button button) {
        button.setStylePrimaryName("v-button");
        button.setStyleName("borderless v-button-borderless");
        button.addStyleName("small v-button-small");
        button.addStyleName("on-focus-no-border v-button-on-focus-no-border");
    }

    private Map<String, String> formatNameStatusData(String input) {
        Map<String, String> details = new HashMap<>();
        String[] tempData = input.split(",");
        for (String statusWithCount : tempData) {
            String[] statusWithCountList = statusWithCount.split(":");
            details.put(statusWithCountList[0], statusWithCountList[1]);
        }
        return details;
    }
}
