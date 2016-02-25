package org.eclipse.hawkbit.ui.customrenderers.client.renderers;

import com.google.gwt.user.client.ui.Button;
import com.vaadin.client.renderers.ButtonRenderer;
import com.vaadin.client.widget.grid.RendererCellReference;

public class LinkRenderer extends ButtonRenderer {
    @Override
    public void render(RendererCellReference cell, String text, Button button) {
        button.setText(text);
        applystyle(button);
        // this is to allow the button to disappear, if the text is null
        button.setVisible(text != null);
        button.getElement().setId(new StringBuilder("link").append(".").append(text).toString());
    }

    private void applystyle(Button button) {
        button.setStylePrimaryName("v-button");
        button.setStyleName("borderless v-button-borderless");
        button.addStyleName("small v-button-small");
        button.addStyleName("on-focus-no-border v-button-on-focus-no-border");
        button.addStyleName("link v-button-link");
    }
}
