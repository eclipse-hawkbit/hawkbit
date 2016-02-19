package org.eclipse.hawkbit.ui.distributionbar.client.renderers;

import com.google.gwt.user.client.ui.Button;
import com.vaadin.client.renderers.ButtonRenderer;
import com.vaadin.client.widget.grid.RendererCellReference;

public class HtmlButtonRenderer extends ButtonRenderer {
    @Override
    public void render(RendererCellReference cell, String text, Button button) {
        if (text != null) {
            button.setHTML(text);
        }
        button.setStylePrimaryName("v-button");
        button.addStyleName(
                "tiny v-button-tiny borderless v-button-borderless icon-only v-button-icon-only button-no-border v-button-button-no-border");
        // this is to allow the button to disappear, if the text is null
        button.setVisible(text != null);
        button.getElement().setId("rollout.action.button.id");
        button.getElement().setTitle("action");
    }
}
