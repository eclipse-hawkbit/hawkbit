package org.eclipse.hawkbit.ui.customrenderers.client.renderers;

import com.google.gwt.user.client.ui.Button;
import com.vaadin.client.renderers.ButtonRenderer;
import com.vaadin.client.widget.grid.RendererCellReference;

public class HtmlButtonRenderer extends ButtonRenderer {
    @Override
    public void render(RendererCellReference cell, String text, Button button) {
        if (text != null) {
            button.setHTML(text);
        }
        applystyles(button);
        // this is to allow the button to disappear, if the text is null
        button.setVisible(text != null);
        button.getElement().setId("rollout.action.button.id");
        button.getElement().setTitle("action");
    }

    private void applystyles(Button button) {
        button.setStylePrimaryName("v-button");
        button.addStyleName("tiny v-button-tiny");
        button.addStyleName("borderless v-button-borderless");
        button.addStyleName("icon-only v-button-icon-only");
        button.addStyleName("button-no-border v-button-button-no-border");
    }
}
