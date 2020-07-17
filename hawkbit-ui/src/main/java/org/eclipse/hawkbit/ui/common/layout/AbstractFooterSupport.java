package org.eclipse.hawkbit.ui.common.layout;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;

/**
 * If footer support is enabled, the footer is placed below the component
 */
public abstract class AbstractFooterSupport {

    /**
     * Creates a sub-layout for the footer.
     *
     * @return the footer sub-layout.
     */
    public Layout createFooterMessageComponent() {
        final HorizontalLayout footerLayout = new HorizontalLayout();
        footerLayout.setMargin(false);
        footerLayout.setSpacing(false);
        footerLayout.setWidth(100, Unit.PERCENTAGE);

        footerLayout.addComponent(getFooterMessageLabel());

        return footerLayout;
    }

    /**
     * Get the count message label.
     *
     * @return count message
     */
    protected abstract Label getFooterMessageLabel();
}