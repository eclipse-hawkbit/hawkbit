/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Link;

/**
 * 
 * Helper class that provides the help icon layout in the right upper corner of
 * a popup window
 *
 */
public class PopupWindowHelp extends HorizontalLayout {

    private static final long serialVersionUID = 5377482512908004826L;

    private Link linkToHelp;

    /**
     * Provides a horizontalLayout with a help icon in the right corner. The
     * layout is shown by default.
     * 
     * @param link
     *            link to the help page in the wiki or on github
     */
    public PopupWindowHelp(final String link) {
        createLinkToHelp(link);
        init();
    }

    /**
     * Provides a horizontalLayout with a help icon in the right corner. The
     * layout is shown by default. The visibility of the layout can be set. If
     * visibility is false the layout is not shown and the space is not
     * reserved.
     * 
     * @param link
     *            link to the help page in the wiki or on github
     * @param visible
     *            false == layout is not shown
     */
    public PopupWindowHelp(final String link, final boolean visible) {
        createLinkToHelp(link);
        init();
        setVisible(visible);
    }

    public void init() {
        setSizeFull();
        addComponent(linkToHelp);
        setComponentAlignment(linkToHelp, Alignment.MIDDLE_RIGHT);
        addStyleName("window-style");
    }

    private void createLinkToHelp(final String link) {
        linkToHelp = SPUIComponentProvider.getHelpLink(link);
    }

}
