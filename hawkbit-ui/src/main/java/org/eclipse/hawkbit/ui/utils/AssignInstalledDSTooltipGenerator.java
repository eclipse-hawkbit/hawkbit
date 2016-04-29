/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ui.utils;

import static org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil.HTML_LI_CLOSE_TAG;
import static org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil.HTML_LI_OPEN_TAG;
import static org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil.HTML_UL_CLOSE_TAG;
import static org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil.HTML_UL_OPEN_TAG;

import java.util.Set;

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

import com.vaadin.data.Item;
import com.vaadin.ui.AbstractSelect.ItemDescriptionGenerator;
import com.vaadin.ui.Component;
import com.vaadin.ui.Table;

public class AssignInstalledDSTooltipGenerator implements ItemDescriptionGenerator {
    private static final long serialVersionUID = 688730421728162456L;

    private static final String ASSIGN_DIST_SET = "assignedDistributionSet";
    private static final String INSTALL_DIST_SET = "installedDistributionSet";

    @Override
    public String generateDescription(final Component source, final Object itemId, final Object propertyId) {
        final DistributionSet distributionSet;
        final Item item = ((Table) source).getItem(itemId);
        if (propertyId != null) {
            if (propertyId.equals(SPUILabelDefinitions.ASSIGNED_DISTRIBUTION_NAME_VER)) {
                distributionSet = (DistributionSet) item.getItemProperty(ASSIGN_DIST_SET).getValue();
                return getDSDetails(distributionSet);
            } else if (propertyId.equals(SPUILabelDefinitions.INSTALLED_DISTRIBUTION_NAME_VER)) {
                distributionSet = (DistributionSet) item.getItemProperty(INSTALL_DIST_SET).getValue();
                return getDSDetails(distributionSet);
            }
        }
        return null;
    }

    private String getDSDetails(final DistributionSet distributionSet) {
        if (distributionSet == null) {
            return null;
        }
        final StringBuilder swModuleNames = new StringBuilder();
        final StringBuilder swModuleVendors = new StringBuilder();
        final Set<SoftwareModule> swModules = distributionSet.getModules();
        swModules.forEach(swModule -> {
            swModuleNames.append(swModule.getName());
            swModuleNames.append(" , ");
            swModuleVendors.append(swModule.getVendor());
            swModuleVendors.append(" , ");
        });
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(HTML_UL_OPEN_TAG);
        stringBuilder.append(HTML_LI_OPEN_TAG);
        stringBuilder.append(" DistributionSet Description : ").append(distributionSet.getDescription());
        stringBuilder.append(HTML_LI_CLOSE_TAG);
        stringBuilder.append(HTML_LI_OPEN_TAG);
        stringBuilder.append(" DistributionSet Type : ").append((distributionSet.getType()).getName());
        stringBuilder.append(HTML_LI_CLOSE_TAG);
        stringBuilder.append(HTML_LI_OPEN_TAG);
        stringBuilder.append(" Required Migration step : ")
                .append(distributionSet.isRequiredMigrationStep() ? "Yes" : "No");
        stringBuilder.append(HTML_LI_CLOSE_TAG);
        stringBuilder.append(HTML_LI_OPEN_TAG);
        stringBuilder.append("SoftWare Modules : ").append(swModuleNames.toString());
        stringBuilder.append(HTML_LI_CLOSE_TAG);
        stringBuilder.append(HTML_LI_OPEN_TAG);
        stringBuilder.append("Vendor(s) : ").append(swModuleVendors.toString());
        stringBuilder.append(HTML_LI_CLOSE_TAG);
        stringBuilder.append(HTML_UL_CLOSE_TAG);
        return stringBuilder.toString();
    }
}
