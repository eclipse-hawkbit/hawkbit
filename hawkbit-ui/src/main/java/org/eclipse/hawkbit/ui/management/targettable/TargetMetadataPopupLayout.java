/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.AbstractMetadataPopupLayout;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Pop up layout to display target metadata.
 */
public class TargetMetadataPopupLayout extends AbstractMetadataPopupLayout<Target, MetaData> {

    private static final long serialVersionUID = 1L;

    private final transient TargetManagement targetManagement;

    private final transient EntityFactory entityFactory;

    public TargetMetadataPopupLayout(final VaadinMessageSource i18n, final UINotification uiNotification,
            final UIEventBus eventBus, final TargetManagement targetManagement, final EntityFactory entityFactory,
            final SpPermissionChecker permChecker) {
        super(i18n, uiNotification, eventBus, permChecker);
        this.targetManagement = targetManagement;
        this.entityFactory = entityFactory;
    }

    @Override
    protected boolean checkForDuplicate(final Target entity, final String value) {
        return targetManagement.getMetaDataByControllerId(entity.getControllerId(), value).isPresent();
    }

    @Override
    protected MetaData createMetadata(final Target entity, final String key, final String value) {
        final TargetMetadata metaData = targetManagement.createMetaData(entity.getControllerId(),
                Collections.singletonList(entityFactory.generateTargetMetadata(key, value))).get(0);
        setSelectedEntity(metaData.getTarget());
        return metaData;
    }

    @Override
    protected MetaData updateMetadata(final Target entity, final String key, final String value) {
        final TargetMetadata metaData = targetManagement.updateMetadata(entity.getControllerId(),
                entityFactory.generateTargetMetadata(key, value));
        setSelectedEntity(metaData.getTarget());
        return metaData;
    }

    @Override
    protected List<MetaData> getMetadataList() {
        return Collections.unmodifiableList(targetManagement
                .findMetaDataByControllerId(PageRequest.of(0, 500), getSelectedEntity().getControllerId())
                .getContent());
    }

    @Override
    protected void deleteMetadata(final Target entity, final String key) {
        targetManagement.deleteMetaData(entity.getControllerId(), key);
    }

    @Override
    protected boolean hasCreatePermission() {
        return permChecker.hasCreateRepositoryPermission();
    }

    @Override
    protected boolean hasUpdatePermission() {
        return permChecker.hasUpdateRepositoryPermission();
    }

}
