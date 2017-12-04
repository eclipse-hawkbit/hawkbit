/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.AbstractMetadataPopupLayout;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Pop up layout to display software module metadata.
 */
public class SwMetadataPopupLayout extends AbstractMetadataPopupLayout<SoftwareModule, SoftwareModuleMetadata> {

    private static final long serialVersionUID = -1252090014161012563L;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final transient EntityFactory entityFactory;

    public SwMetadataPopupLayout(final VaadinMessageSource i18n, final UINotification uiNotification,
            final UIEventBus eventBus, final SoftwareModuleManagement softwareManagement,
            final EntityFactory entityFactory, final SpPermissionChecker permChecker) {
        super(i18n, uiNotification, eventBus, permChecker);
        this.softwareModuleManagement = softwareManagement;
        this.entityFactory = entityFactory;
    }

    @Override
    protected boolean checkForDuplicate(final SoftwareModule entity, final String value) {
        return softwareModuleManagement.getMetaDataBySoftwareModuleId(entity.getId(), value).isPresent();
    }

    @Override
    protected SoftwareModuleMetadata createMetadata(final SoftwareModule entity, final String key, final String value) {
        final SoftwareModuleMetadata swMetadata = softwareModuleManagement
                .createMetaData(entityFactory.softwareModuleMetadata().create(entity.getId()).key(key).value(value));
        setSelectedEntity(swMetadata.getSoftwareModule());
        return swMetadata;
    }

    @Override
    protected SoftwareModuleMetadata updateMetadata(final SoftwareModule entity, final String key, final String value) {
        final SoftwareModuleMetadata swMetadata = softwareModuleManagement
                .updateMetaData(entityFactory.softwareModuleMetadata().update(entity.getId(), key).value(value));
        setSelectedEntity(swMetadata.getSoftwareModule());
        return swMetadata;
    }

    @Override
    protected List<SoftwareModuleMetadata> getMetadataList() {
        return Collections.unmodifiableList(softwareModuleManagement
                .findMetaDataBySoftwareModuleId(new PageRequest(0, MAX_METADATA_QUERY), getSelectedEntity().getId())
                .getContent());
    }

    @Override
    protected void deleteMetadata(final SoftwareModule entity, final String key) {
        softwareModuleManagement.deleteMetaData(entity.getId(), key);
    }

    @Override
    protected boolean hasCreatePermission() {
        return permChecker.hasCreateDistributionPermission();
    }

    @Override
    protected boolean hasUpdatePermission() {
        return permChecker.hasUpdateDistributionPermission();
    }
}
