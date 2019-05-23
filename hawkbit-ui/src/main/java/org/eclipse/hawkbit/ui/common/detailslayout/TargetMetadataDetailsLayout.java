/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.management.targettable.TargetMetadataPopupLayout;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;

import com.vaadin.ui.UI;

/**
 * Target Metadata details layout.
 *
 */
public class TargetMetadataDetailsLayout extends AbstractMetadataDetailsLayout {

    private static final long serialVersionUID = 1L;

    private final transient TargetManagement targetManagement;

    private final TargetMetadataPopupLayout targetMetadataPopupLayout;

    private Long selectedTargetId;

    /**
     * Initialize the layout.
     * 
     * @param i18n
     *            the i18n service
     * @param targetManagement
     *            the target management service
     * @param targetMetadataPopupLayout
     *            the target metadata popup layout
     */
    public TargetMetadataDetailsLayout(final VaadinMessageSource i18n, final TargetManagement targetManagement,
            final TargetMetadataPopupLayout targetMetadataPopupLayout) {
        super(i18n);
        this.targetManagement = targetManagement;
        this.targetMetadataPopupLayout = targetMetadataPopupLayout;

    }

    /**
     * Populate target metadata.
     *
     * @param target
     */
    public void populateMetadata(final Target target) {
        removeAllItems();
        if (target == null) {
            return;
        }
        selectedTargetId = target.getId();
        targetManagement.findMetaDataByControllerId(PageRequest.of(0, MAX_METADATA_QUERY), target.getControllerId())
                .getContent().forEach(this::setMetadataProperties);
    }

    @Override
    protected String getDetailLinkId(final String name) {
        return new StringBuilder(UIComponentIdProvider.TARGET_METADATA_DETAIL_LINK).append('.').append(name).toString();
    }

    @Override
    protected void showMetadataDetails(final String metadataKey) {
        targetManagement.get(selectedTargetId).ifPresent(
                target -> UI.getCurrent().addWindow(targetMetadataPopupLayout.getWindow(target, metadataKey)));

    }

}
