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
import org.eclipse.hawkbit.ui.menu.DashboardMenuItem;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;

/**
 * 
 * Menu item for target filter management view.
 * 
 *
 */
@Component
@Order(300)
public class FilterManagementViewMenuItem implements DashboardMenuItem {

    private static final long serialVersionUID = -1272853053031512243L;

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
        return "Target Filters";
    }

    @Override
    public String getDashboardCaptionLong() {
        return "Target Filter Management";
    }

    @Override
    public List<String> getPermissions() {
        return Arrays.asList(SpPermission.CREATE_TARGET, SpPermission.READ_TARGET);
    }

}
