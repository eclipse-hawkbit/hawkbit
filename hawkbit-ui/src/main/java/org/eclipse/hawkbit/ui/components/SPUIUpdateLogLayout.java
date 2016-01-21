/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.components;

import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;

import com.vaadin.ui.VerticalLayout;

/**
 * ChangeLog Vertical layout for Target and Distribution.
 *
 *
 *
 */
public class SPUIUpdateLogLayout {
    private final VerticalLayout changeLogLayout;

    /**
     * Parametric constructor.
     * 
     * @param changeLogLayout
     *            as layout
     * @param lastModifiedAt
     *            as Date
     * @param lastModifiedBy
     *            as string
     * @param createdAt
     *            as date
     * @param createdBy
     *            as string
     */
    public SPUIUpdateLogLayout(final VerticalLayout changeLogLayout, final Long lastModifiedAt,
            final String lastModifiedBy, final Long createdAt, final String createdBy) {
        this.changeLogLayout = changeLogLayout;
        changeLogLayout.setSpacing(true);
        changeLogLayout.setMargin(true);
        decorate(lastModifiedAt, lastModifiedBy, createdAt, createdBy);
    }

    /**
     * Decorate.
     * 
     * @param lastModifiedAt
     * @param lastModifiedBy
     * @param createdAt
     * @param createdBy
     */
    private void decorate(final Long lastModifiedAt, final String lastModifiedBy, final Long createdAt,
            final String createdBy) {
        final I18N i18n = SpringContextHelper.getBean(I18N.class);
        changeLogLayout.addComponent(SPUIComponentProvider.createNameValueLabel(i18n.get("label.created.at"),
                createdAt == null ? "" : SPDateTimeUtil.getFormattedDate(createdAt)));

        changeLogLayout.addComponent(SPUIComponentProvider.createNameValueLabel(i18n.get("label.created.by"),
                createdBy == null ? "" : createdBy));

        if (null != lastModifiedAt) {
            changeLogLayout.addComponent(SPUIComponentProvider.createNameValueLabel(i18n.get("label.modified.date"),
                    SPDateTimeUtil.getFormattedDate(lastModifiedAt)));

            changeLogLayout.addComponent(SPUIComponentProvider.createNameValueLabel(i18n.get("label.modified.by"),
                    lastModifiedBy == null ? "" : lastModifiedBy));
        }

    }

    /**
     * GET Change LogLayout.
     * 
     * @return VerticalLayout as UI
     */
    public VerticalLayout getChangeLogLayout() {
        return changeLogLayout;
    }
}
