/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.filterlayout;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;

/**
 *
 *
 */
public abstract class AbstractFilterMultiButtonClick extends AbstractFilterButtonClickBehaviour {

    protected final Set<Button> alreadyClickedButtons = new HashSet<>();

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.layouts.SPFilterButtonClick#
     * processFilterButtonClick(com.vaadin.ui. Button.ClickEvent)
     */
    @Override
    public void processFilterButtonClick(final ClickEvent event) {
        final Button clickedButton = (Button) event.getComponent();
        if (isButtonUnClicked(clickedButton)) {
            /* If same button clicked */
            clickedButton.removeStyleName(SPUIStyleDefinitions.SP_FILTER_BTN_CLICKED_STYLE);
            alreadyClickedButtons.remove(clickedButton);
            filterUnClicked(clickedButton);
        } else {
            clickedButton.addStyleName(SPUIStyleDefinitions.SP_FILTER_BTN_CLICKED_STYLE);
            alreadyClickedButtons.add(clickedButton);
            filterClicked(clickedButton);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.layouts.SPFilterButtonClickBehaviour#
     * setDefaultClickedButton(com.vaadin .ui.Button)
     */
    @Override
    protected void setDefaultClickedButton(final Button button) {
        if (button != null) {
            alreadyClickedButtons.add(button);
            button.addStyleName(SPUIStyleDefinitions.SP_FILTER_BTN_CLICKED_STYLE);
        }
    }

    /**
     * @param clickedButton
     * @return
     */
    private boolean isButtonUnClicked(final Button clickedButton) {
        return !alreadyClickedButtons.isEmpty() && alreadyClickedButtons.contains(clickedButton);
    }
}
