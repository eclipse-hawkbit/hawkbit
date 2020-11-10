/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.providers.SmMetaDataDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
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
public class SmMetaDataWindowLayout extends AbstractMetaDataWindowLayout<Long> {
    private static final long serialVersionUID = 1L;

    private final transient SoftwareModuleManagement smManagement;

    private final MetaDataWindowGrid<Long> smMetaDataWindowGrid;

    private final transient SmMetaDataAddUpdateWindowLayout smMetaDataAddUpdateWindowLayout;
    private final transient AddMetaDataWindowController addSmMetaDataWindowController;
    private final transient UpdateMetaDataWindowController updateSmMetaDataWindowController;

    /**
     * Constructor for AbstractTagWindowLayout
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param smManagement
     *            SoftwareModuleManagement
     */
    public SmMetaDataWindowLayout(final CommonUiDependencies uiDependencies,
            final SoftwareModuleManagement smManagement) {
        super(uiDependencies);

        this.smManagement = smManagement;

        this.smMetaDataWindowGrid = new MetaDataWindowGrid<>(uiDependencies, new SmMetaDataDataProvider(smManagement),
                this::hasMetadataChangePermission,  this::deleteMetaData);

        this.smMetaDataAddUpdateWindowLayout = new SmMetaDataAddUpdateWindowLayout(i18n,this::hasMetadataChangePermission);
        this.addSmMetaDataWindowController = new AddMetaDataWindowController(uiDependencies,
                smMetaDataAddUpdateWindowLayout, this::createMetaData, this::isDuplicate);
        this.updateSmMetaDataWindowController = new UpdateMetaDataWindowController(uiDependencies,
                smMetaDataAddUpdateWindowLayout, this::updateMetaData, this::isDuplicate);

        buildLayout();
        addGridSelectionListener();
    }

    @Override
    protected String getEntityType() {
        return i18n.getMessage("caption.software.module");
    }

    @Override
    protected MetaData doCreateMetaData(final ProxyMetaData entity) {
        return smManagement
                .createMetaData(uiDependencies.getEntityFactory().softwareModuleMetadata().create(masterEntityFilter)
                        .key(entity.getKey()).value(entity.getValue()).targetVisible(entity.isVisibleForTargets()));
    }

    @Override
    protected MetaData doUpdateMetaData(final ProxyMetaData entity) {
        return smManagement.updateMetaData(
                uiDependencies.getEntityFactory().softwareModuleMetadata().update(masterEntityFilter, entity.getKey())
                        .value(entity.getValue()).targetVisible(entity.isVisibleForTargets()));
    }

    @Override
    protected void doDeleteMetaDataByKey(final String metaDataKey) {
        smManagement.deleteMetaData(masterEntityFilter, metaDataKey);
    }

    private boolean isDuplicate(final String metaDataKey) {
        return smManagement.getMetaDataBySoftwareModuleId(masterEntityFilter, metaDataKey).isPresent();
    }

    @Override
    protected MetaDataWindowGrid<Long> getMetaDataWindowGrid() {
        return smMetaDataWindowGrid;
    }

    /**
     * @return add widow controller for software module
     */
    @Override
    public AddMetaDataWindowController getAddMetaDataWindowController() {
        return addSmMetaDataWindowController;
    }

    /**
     * @return update widow controller for software module
     */
    @Override
    public UpdateMetaDataWindowController getUpdateMetaDataWindowController() {
        return updateSmMetaDataWindowController;
    }

    /**
     * @return add and update widow layout for software module
     */
    @Override
    public MetaDataAddUpdateWindowLayout getMetaDataAddUpdateWindowLayout() {
        return smMetaDataAddUpdateWindowLayout;
    }

    @Override
    protected void publishEntityModifiedEvent() {
        eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_UPDATED, ProxySoftwareModule.class, masterEntityFilter));
    }
}
