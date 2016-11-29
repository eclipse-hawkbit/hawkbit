/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.filterlayout;

import java.io.Serializable;

import com.vaadin.ui.Button;

/**
 * Abstract button click behaviour of filter buttons layout.
 */
public abstract class AbstractFilterButtonClickBehaviour implements Serializable {

    private static final long serialVersionUID = 5486557136906648322L;

    /**
     * @param event
     */
    protected abstract void processFilterButtonClick(Button.ClickEvent event);

    /**
     * @param clickedButton
     */
    protected abstract void filterUnClicked(final Button clickedButton);

    /**
     * @param clickedButton
     */
    protected abstract void filterClicked(final Button clickedButton);

    /**
     * 
     * @param button
     */
    protected abstract void setDefaultClickedButton(final Button button);
}
