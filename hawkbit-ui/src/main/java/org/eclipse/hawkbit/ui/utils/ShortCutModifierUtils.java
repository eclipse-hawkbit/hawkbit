/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
