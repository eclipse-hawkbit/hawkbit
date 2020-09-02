/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import org.eclipse.hawkbit.ui.common.grid.header.AbstractGridHeader;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.Component;

/**
 * Header for ActionHistoryStatus.
 */
public class ActionStatusGridHeader extends AbstractGridHeader {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for ActionStatusGridHeader
     *
     * @param i18n
     *            VaadinMessageSource
     */
    public ActionStatusGridHeader(final VaadinMessageSource i18n) {
        super(i18n, null, null);

        buildHeader();
    }

    @Override
    protected Component getHeaderCaption() {
        return SPUIComponentProvider.generateCaptionLabel(i18n, "caption.action.states");
    }
}
