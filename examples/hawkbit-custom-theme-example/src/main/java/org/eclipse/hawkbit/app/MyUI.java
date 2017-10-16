package org.eclipse.hawkbit.app;
/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

import org.eclipse.hawkbit.ui.AbstractHawkbitUI;
import org.eclipse.hawkbit.ui.push.EventPushStrategy;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.spring.annotation.SpringUI;

/**
 * Example hawkBit UI implementation.
 * 
 * A {@link SpringUI} annotated class must be present in the classpath. The
 * easiest way to get an hawkBit UI running is to extend the {@link AbstractHawkbitUI}
 * and to annotated it with {@link SpringUI} as in this example.
 *
 */
@SpringUI
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET)
@Title("hawkBit Theme example")
@Theme(value = "exampletheme")
public class MyUI extends AbstractHawkbitUI {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     * 
     * @param pushStrategy
     *            the push strategy
     * @param eventBus
     *            the event bus
     */
    public MyUI(final EventPushStrategy pushStrategy) {
        super(pushStrategy);
    }
}
