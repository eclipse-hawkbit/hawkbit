/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.simulator.ui;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.navigator.SpringViewProvider;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * The vaadin simulator UI which allows to generate simulated devices and show
 * their current status and update progress.
 * 
 * @author Michael Hirsch
 *
 */
@SpringUI(path = "")
@Title("hawkBit Device Simulator")
@Theme(value = "simulator")
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET)
public class SimulatorUI extends UI {

    private static final long serialVersionUID = 1L;

    private final VerticalLayout rootLayout = new VerticalLayout();

    @Autowired
    private SpringViewProvider viewProvider;

    @Override
    protected void init(final VaadinRequest request) {

        rootLayout.setSizeFull();

        final Panel viewContainer = new Panel();
        viewContainer.setSizeFull();
        rootLayout.addComponent(viewContainer);
        rootLayout.setExpandRatio(viewContainer, 1.0F);

        final Navigator navigator = new Navigator(this, viewContainer);
        navigator.addProvider(viewProvider);

        setContent(rootLayout);
    }

}
