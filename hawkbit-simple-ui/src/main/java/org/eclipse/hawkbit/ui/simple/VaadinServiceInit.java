/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.ui.simple;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.annotation.SpringComponent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringComponent
public class VaadinServiceInit implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        // cache zoneId of client as soon as possible
        event.getSource().addUIInitListener(uiEvent ->
                uiEvent.getUI().getPage().retrieveExtendedClientDetails(details -> {}));
    }
}
