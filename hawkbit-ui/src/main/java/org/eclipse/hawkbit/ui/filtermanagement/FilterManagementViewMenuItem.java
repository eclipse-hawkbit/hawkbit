/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.ui.management.AbstractDashboardMenuItemNotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;

/**
 * Menu item for target filter management view.
 */
@SpringComponent
@UIScope
@Order(300)
public class FilterManagementViewMenuItem extends AbstractDashboardMenuItemNotification {

    private static final long serialVersionUID = 1L;

    @Autowired
    FilterManagementViewMenuItem(final VaadinMessageSource i18n) {
        super(i18n);
    }

    @Override
    public String getViewName() {
        return FilterManagementView.VIEW_NAME;
    }

    @Override
    public Resource getDashboardIcon() {
        return FontAwesome.FILTER;
    }

    @Override
    public String getDashboardCaption() {
        return getI18n().getMessage("dashboard.filters.caption");
    }

    @Override
    public String getDashboardCaptionLong() {
        return getI18n().getMessage("dashboard.filters.caption-long");
    }

    @Override
    public List<String> getPermissions() {
        return Arrays.asList(SpPermission.READ_TARGET);
    }

}
