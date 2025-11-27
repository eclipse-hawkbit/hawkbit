/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.ui;

import java.util.Arrays;
import java.util.Locale;

import com.vaadin.flow.i18n.DefaultI18NProvider;
import org.springframework.stereotype.Component;

@Component
public class SimpleI18NProvider extends DefaultI18NProvider {

    SimpleI18NProvider() {
        super(Arrays.stream(Locale.getAvailableLocales()).toList());
    }
}
