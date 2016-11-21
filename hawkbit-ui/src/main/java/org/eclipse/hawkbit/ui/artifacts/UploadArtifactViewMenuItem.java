/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts;

import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.ui.management.AbstractDashboardMenuItemNotification;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;

/**
 * Display artifacts upload view menu item.
 *
 *
 */
@Component
@Order(500)
public class UploadArtifactViewMenuItem extends AbstractDashboardMenuItemNotification {

    private static final long serialVersionUID = 4096851897640769726L;

    @Override
    public String getViewName() {
        return UploadArtifactView.VIEW_NAME;
    }

    @Override
    public Resource getDashboardIcon() {
        return FontAwesome.UPLOAD;
    }

    @Override
    public String getDashboardCaption() {
        return "Upload";
    }

    @Override
    public String getDashboardCaptionLong() {
        return "Upload Management";
    }

    @Override
    public List<String> getPermissions() {
        return Arrays.asList(SpPermission.CREATE_REPOSITORY, SpPermission.READ_REPOSITORY);
    }
}
