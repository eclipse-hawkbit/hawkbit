package org.eclipse.hawkbit.app;
/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.hawkbit.ui.HawkbitUI;
import org.eclipse.hawkbit.ui.UIEventProvider;
import org.eclipse.hawkbit.ui.push.DelayedEventBusPushStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.eventbus.EventBus;
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
 * easiest way to get an hawkBit UI running is to extend the {@link HawkbitUI}
 * and to annotated it with {@link SpringUI} as in this example.
 *
 */
@SpringUI
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET)
@Title("hawkBit Theme example")
@Theme(value = "exampletheme")
public class MyUI extends HawkbitUI {

    private static final long serialVersionUID = 1L;

    @Autowired
    public MyUI(final ScheduledExecutorService executorService, final EventBus systemEventBus,
            final org.vaadin.spring.events.EventBus.SessionEventBus eventBus, final UIEventProvider provider) {
        super(new DelayedEventBusPushStrategy(executorService, eventBus, systemEventBus, provider));
    }
}
