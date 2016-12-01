/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.tenantconfiguration;

import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.ui.management.AbstractDashboardMenuItemNotification;
import org.springframework.core.annotation.Order;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;

/**
 * Menu item for system configuration view.
 */
@UIScope
@SpringComponent
@Order(700)
public class TenantConfigurationDashboardViewMenuItem extends AbstractDashboardMenuItemNotification {

    private static final long serialVersionUID = 348659206461499664L;

    @Override
    public String getViewName() {
        return TenantConfigurationDashboardView.VIEW_NAME;
    }

    @Override
    public Resource getDashboardIcon() {
        return FontAwesome.COG;
    }

    @Override
    public String getDashboardCaption() {
        return "System Config";
    }

    @Override
    public String getDashboardCaptionLong() {
        return "System Configuration";
    }

    @Override
    public List<String> getPermissions() {
        return Arrays.asList(SpPermission.TENANT_CONFIGURATION);
    }

}
