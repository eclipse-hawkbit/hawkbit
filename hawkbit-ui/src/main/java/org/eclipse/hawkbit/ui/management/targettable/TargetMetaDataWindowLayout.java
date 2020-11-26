/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.Collections;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.providers.TargetMetaDataDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.detailslayout.AbstractMetaDataWindowLayout;
import org.eclipse.hawkbit.ui.common.detailslayout.AddMetaDataWindowController;
import org.eclipse.hawkbit.ui.common.detailslayout.MetaDataAddUpdateWindowLayout;
import org.eclipse.hawkbit.ui.common.detailslayout.MetaDataWindowGrid;
import org.eclipse.hawkbit.ui.common.detailslayout.UpdateMetaDataWindowController;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;

/**
 * Class for metadata add/update window layout.
 */
public class TargetMetaDataWindowLayout extends AbstractMetaDataWindowLayout<String> {
    private static final long serialVersionUID = 1L;

    private final transient TargetManagement targetManagement;
    private final transient MetaDataAddUpdateWindowLayout metaDataAddUpdateWindowLayout;
    private final transient AddMetaDataWindowController addTargetMetaDataWindowController;
    private final transient UpdateMetaDataWindowController updateTargetMetaDataWindowController;

    private final MetaDataWindowGrid<String> targetMetaDataWindowGrid;

    /**
     * Constructor for TargetMetaDataWindowLayout
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetManagement
     *            TargetManagement
     */
    public TargetMetaDataWindowLayout(final CommonUiDependencies uiDependencies,
            final TargetManagement targetManagement) {
        super(uiDependencies);

        this.targetManagement = targetManagement;

        this.targetMetaDataWindowGrid = new MetaDataWindowGrid<>(uiDependencies,
                new TargetMetaDataDataProvider(targetManagement), this::hasMetadataChangePermission,this::deleteMetaData);

        this.metaDataAddUpdateWindowLayout = new MetaDataAddUpdateWindowLayout(i18n,this::hasMetadataChangePermission);
        this.addTargetMetaDataWindowController = new AddMetaDataWindowController(uiDependencies,
                metaDataAddUpdateWindowLayout, this::createMetaData, this::isDuplicate);
        this.updateTargetMetaDataWindowController = new UpdateMetaDataWindowController(uiDependencies,
                metaDataAddUpdateWindowLayout, this::updateMetaData, this::isDuplicate);

        buildLayout();
        addGridSelectionListener();
    }

    @Override
    protected String getEntityType() {
        return i18n.getMessage("caption.target");
    }

    @Override
    protected MetaData doCreateMetaData(final ProxyMetaData entity) {
        return targetManagement
                .createMetaData(masterEntityFilter, Collections
                        .singletonList(entityFactory.generateTargetMetadata(entity.getKey(), entity.getValue())))
                .get(0);
    }

    @Override
    protected MetaData doUpdateMetaData(final ProxyMetaData entity) {
        return targetManagement.updateMetadata(masterEntityFilter,
                entityFactory.generateTargetMetadata(entity.getKey(), entity.getValue()));
    }

    @Override
    protected void doDeleteMetaDataByKey(final String metaDataKey) {
        targetManagement.deleteMetaData(masterEntityFilter, metaDataKey);
    }

    private boolean isDuplicate(final String metaDataKey) {
        return targetManagement.getMetaDataByControllerId(masterEntityFilter, metaDataKey).isPresent();
    }

    @Override
    protected MetaDataWindowGrid<String> getMetaDataWindowGrid() {
        return targetMetaDataWindowGrid;
    }

    @Override
    public AddMetaDataWindowController getAddMetaDataWindowController() {
        return addTargetMetaDataWindowController;
    }

    @Override
    public UpdateMetaDataWindowController getUpdateMetaDataWindowController() {
        return updateTargetMetaDataWindowController;
    }

    @Override
    public MetaDataAddUpdateWindowLayout getMetaDataAddUpdateWindowLayout() {
        return metaDataAddUpdateWindowLayout;
    }

    @Override
    protected void publishEntityModifiedEvent() {
        targetManagement.getByControllerID(masterEntityFilter).map(Target::getId).ifPresent(
                targetId -> eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                        EntityModifiedEventType.ENTITY_UPDATED, ProxyTarget.class, targetId)));
    }
}
