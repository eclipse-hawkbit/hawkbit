package org.eclipse.hawkbit.ui.distributionbar.client.renderers;

import com.google.gwt.user.client.ui.Button;
import com.vaadin.client.renderers.ButtonRenderer;
import com.vaadin.client.widget.grid.RendererCellReference;

public class LinkRenderer extends ButtonRenderer {
    @Override
    public void render(RendererCellReference cell, String text, Button button) {
        button.setText(text);
        button.setStylePrimaryName("v-button");
        button.addStyleName(
                "borderless v-button-borderless small v-button-small on-focus-no-border v-button-on-focus-no-border link v-button-link");
        // this is to allow the button to disappear, if the text is null
        button.setVisible(text != null);
        button.getElement().setId("rollout.action.button.id");
    }
}
