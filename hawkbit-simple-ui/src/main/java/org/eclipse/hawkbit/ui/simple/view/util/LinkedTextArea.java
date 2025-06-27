/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.simple.view.util;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.textfield.TextArea;

public class LinkedTextArea extends TextArea implements ClickNotifier<LinkedTextArea> {

    String query;

    public LinkedTextArea(String title, String queryPrefix) {
        super(title);
        super.setReadOnly(true);
        this.addClickListener((event) -> {
            if (query != null) {
                UI.getCurrent().navigate(queryPrefix + query);
            }
        });
    }

    public void setValueWithLink(String value, String query) {
        super.setValue(value);
        this.query = query;
    }
}
