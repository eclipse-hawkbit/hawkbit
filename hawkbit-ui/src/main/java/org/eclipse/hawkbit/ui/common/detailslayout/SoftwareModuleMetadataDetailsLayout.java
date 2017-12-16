/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.List;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.ui.distributions.smtable.SwMetadataPopupLayout;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.CollectionUtils;

import com.vaadin.ui.UI;

/**
 * SoftwareModule Metadata details layout.
 *
 */
public class SoftwareModuleMetadataDetailsLayout extends AbstractMetadataDetailsLayout {

    private static final long serialVersionUID = 1L;

    private transient SoftwareModuleManagement softwareModuleManagement;

    private final SwMetadataPopupLayout swMetadataPopupLayout;

    private Long selectedSWModuleId;

    /**
     * Initialize the layout.
     * 
     * @param i18n
     *            the i18n service
     * @param softwareManagement
     *            the software management service
     * @param swMetadataPopupLayout
     *            the software module metadata popup layout
     */
    public SoftwareModuleMetadataDetailsLayout(final VaadinMessageSource i18n,
            final SoftwareModuleManagement softwareManagement, final SwMetadataPopupLayout swMetadataPopupLayout) {
        super(i18n);
        this.softwareModuleManagement = softwareManagement;
        this.swMetadataPopupLayout = swMetadataPopupLayout;
    }

    /**
     * Populate software module metadata table.
     * 
     * @param swModule
     */
    public void populateSMMetadata(final SoftwareModule swModule) {
        removeAllItems();
        if (null == swModule) {
            return;
        }
        selectedSWModuleId = swModule.getId();
        final List<SoftwareModuleMetadata> swMetadataList = softwareModuleManagement
                .findMetaDataBySoftwareModuleId(new PageRequest(0, MAX_METADATA_QUERY), selectedSWModuleId)
                .getContent();
        if (!CollectionUtils.isEmpty(swMetadataList)) {
            swMetadataList.forEach(this::setMetadataProperties);
        }
    }

    @Override
    protected void showMetadataDetails(final String metadataKey) {
        softwareModuleManagement.get(selectedSWModuleId).ifPresent(
                swmodule -> UI.getCurrent().addWindow(swMetadataPopupLayout.getWindow(swmodule, metadataKey)));
    }

    @Override
    protected String getDetailLinkId(final String name) {
        return new StringBuilder(UIComponentIdProvider.SW_METADATA_DETAIL_LINK).append('.').append(name).toString();
    }

}
