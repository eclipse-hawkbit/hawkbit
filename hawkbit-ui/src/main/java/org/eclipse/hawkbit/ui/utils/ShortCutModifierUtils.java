/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.utils;

import com.vaadin.event.ShortcutAction;
import com.vaadin.server.Page;
import com.vaadin.server.WebBrowser;

/**
 * On different systems there are different modifier for short cuts. This
 * utility class handles the cross-platform functionality.
 */
public final class ShortCutModifierUtils {

    private ShortCutModifierUtils() {

    }

    /**
     * Returns the ctrl or meta modifier depending on the platform.
     * 
     * @return on mac return
     *         {@link com.vaadin.event.ShortcutAction.ModifierKey#META} other
     *         platform return
     *         {@link com.vaadin.event.ShortcutAction.ModifierKey#CTRL}
     */
    public static int getCtrlOrMetaModifier() {
        final WebBrowser webBrowser = Page.getCurrent().getWebBrowser();
        if (webBrowser.isMacOSX()) {
            return ShortcutAction.ModifierKey.META;
        }

        return ShortcutAction.ModifierKey.CTRL;
    }
}
