/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management;

import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;

/**
 * Menu item for deployment.
 */
@SpringComponent
@UIScope
@Order(100)
public class DeploymentViewMenuItem extends AbstractDashboardMenuItemNotification {
    private static final long serialVersionUID = 1L;

    @Autowired
    DeploymentViewMenuItem(final VaadinMessageSource i18n) {
        super(i18n);
    }

    @Override
    public String getViewName() {
        return DeploymentView.VIEW_NAME;
    }

    @Override
    public Resource getDashboardIcon() {
        return FontAwesome.HOME;
    }

    @Override
    public String getDashboardCaption() {
        return getI18n().getMessage("dashboard.deployment.caption");
    }

    @Override
    public String getDashboardCaptionLong() {
        return getI18n().getMessage("dashboard.deployment.caption-long");
    }

    @Override
    public List<String> getPermissions() {
        return Arrays.asList(SpPermission.READ_REPOSITORY, SpPermission.READ_TARGET);
    }

}
