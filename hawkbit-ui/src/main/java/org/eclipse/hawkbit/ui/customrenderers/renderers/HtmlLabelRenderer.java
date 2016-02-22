package org.eclipse.hawkbit.ui.customrenderers.renderers;

import com.vaadin.ui.Grid.AbstractRenderer;

public class HtmlLabelRenderer extends  AbstractRenderer<String> {

    private static final long serialVersionUID = -7675588068526774915L;
    /**
     * Creates a new text renderer
     */
    public HtmlLabelRenderer() {
        super(String.class, null);
    }
}
