/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.filterlayout;

import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;

/**
 * Abstract Single button click behaviour of filter buttons layout.
 *
 *
 *
 *
 */
public abstract class AbstractFilterSingleButtonClick extends AbstractFilterButtonClickBehaviour {

    private static final long serialVersionUID = 478874092615793581L;

    private Button alreadyClickedButton;

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.ui.layouts.SPFilterButtonClick#
     * processFilterButtonClick(com.vaadin.ui. Button.ClickEvent)
     */
    @Override
    protected void processFilterButtonClick(final ClickEvent event) {
        final Button clickedButton = (Button) event.getComponent();
        if (isButtonUnClicked(clickedButton)) {
            /* If same button clicked */
            clickedButton.removeStyleName(SPUIStyleDefinitions.SP_FILTER_BTN_CLICKED_STYLE);
            this.alreadyClickedButton = null;
            filterUnClicked(clickedButton);
        } else if (this.alreadyClickedButton != null) {
            /* If button clicked and some other button is already clicked */
            this.alreadyClickedButton.removeStyleName(SPUIStyleDefinitions.SP_FILTER_BTN_CLICKED_STYLE);
            clickedButton.addStyleName(SPUIStyleDefinitions.SP_FILTER_BTN_CLICKED_STYLE);
            this.alreadyClickedButton = clickedButton;
            filterClicked(clickedButton);
        } else {
            /* If button clicked and not other button is clicked currently */
            clickedButton.addStyleName(SPUIStyleDefinitions.SP_FILTER_BTN_CLICKED_STYLE);
            this.alreadyClickedButton = clickedButton;
            filterClicked(clickedButton);
        }
    }

    /**
     * @param clickedButton
     * @return
     */
    private boolean isButtonUnClicked(final Button clickedButton) {
        return this.alreadyClickedButton != null && this.alreadyClickedButton.equals(clickedButton);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.hawkbit.server.ui.layouts.SPFilterButtonClickBehaviour#
     * setDefaultClickedButton(com.vaadin .ui.Button)
     */
    @Override
    protected void setDefaultClickedButton(final Button button) {
        this.alreadyClickedButton = button;
        if (button != null) {
            button.addStyleName(SPUIStyleDefinitions.SP_FILTER_BTN_CLICKED_STYLE);
        }
    }

    /**
     * @return the alreadyClickedButton
     */
    public Button getAlreadyClickedButton() {
        return this.alreadyClickedButton;
    }

    /**
     * @param alreadyClickedButton
     *            the alreadyClickedButton to set
     */
    public void setAlreadyClickedButton(final Button alreadyClickedButton) {
        this.alreadyClickedButton = alreadyClickedButton;
    }

}
