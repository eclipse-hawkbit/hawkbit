/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
