/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.simple.components;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

public class SpinnerIcon extends Icon {

    public SpinnerIcon() {
        super(VaadinIcon.SPINNER_THIRD);
        getStyle().set("animation", "v-spin 1s linear infinite");
    }
}
