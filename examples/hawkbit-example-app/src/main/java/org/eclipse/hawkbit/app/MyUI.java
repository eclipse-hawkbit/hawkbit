/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.app;

import org.eclipse.hawkbit.ui.HawkbitUI;
import org.eclipse.hawkbit.ui.push.DelayedEventBusPushStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.Push;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.spring.annotation.SpringUI;

/**
 * Example hawkBit UI implementation.
 * 
 * A {@link SpringUI} annotated class must be present in the classpath. The
 * easiest way to get an hawkBit UI running is to extend the {@link HawkbitUI}
 * and to annotated it with {@link SpringUI} as in this example.
 *
 */
@SpringUI
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET)
public class MyUI extends HawkbitUI {

    private static final long serialVersionUID = 1L;

    @Autowired
    public MyUI(final EventBus systemEventBus, final org.vaadin.spring.events.EventBus.SessionEventBus eventBus) {
        super(new DelayedEventBusPushStrategy(eventBus, systemEventBus));
    }
}
