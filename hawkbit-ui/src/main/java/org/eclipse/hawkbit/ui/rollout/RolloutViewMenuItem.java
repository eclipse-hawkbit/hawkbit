/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

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
 * Menu item for rollout .
 * 
 *
 *
 */
@SpringComponent
@UIScope
@Order(200)
public class RolloutViewMenuItem extends AbstractDashboardMenuItemNotification {

    private static final long serialVersionUID = 6112540239655168995L;

    @Override
    public String getViewName() {
        return RolloutView.VIEW_NAME;
    }

    @Override
    public Resource getDashboardIcon() {
        return FontAwesome.TASKS;
    }

    @Override
    public String getDashboardCaption() {
        return "Rollout";
    }

    @Override
    public String getDashboardCaptionLong() {
        return "Rollout Management";
    }

    @Override
    public List<String> getPermissions() {
        return Arrays.asList(SpPermission.ROLLOUT_MANAGEMENT);
    }
}
