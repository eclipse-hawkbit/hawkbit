/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import java.util.Collections;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.providers.DsMetaDataDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
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
public class DsMetaDataWindowLayout extends AbstractMetaDataWindowLayout<Long> {
    private static final long serialVersionUID = 1L;

    private final transient DistributionSetManagement dsManagement;

    private final MetaDataWindowGrid<Long> dsMetaDataWindowGrid;

    private final transient MetaDataAddUpdateWindowLayout metaDataAddUpdateWindowLayout;
    private final transient AddMetaDataWindowController addDsMetaDataWindowController;
    private final transient UpdateMetaDataWindowController updateDsMetaDataWindowController;

    /**
     * Constructor for AbstractTagWindowLayout
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param dsManagement
     *            DistributionSetManagement
     */
    public DsMetaDataWindowLayout(final CommonUiDependencies uiDependencies,
            final DistributionSetManagement dsManagement) {
        super(uiDependencies);

        this.dsManagement = dsManagement;

        this.dsMetaDataWindowGrid = new MetaDataWindowGrid<>(uiDependencies, new DsMetaDataDataProvider(dsManagement),
                this::hasMetadataChangePermission,this::deleteMetaData);

        this.metaDataAddUpdateWindowLayout = new MetaDataAddUpdateWindowLayout(i18n,this::hasMetadataChangePermission);
        this.addDsMetaDataWindowController = new AddMetaDataWindowController(uiDependencies,
                metaDataAddUpdateWindowLayout, this::createMetaData, this::isDuplicate);
        this.updateDsMetaDataWindowController = new UpdateMetaDataWindowController(uiDependencies,
                metaDataAddUpdateWindowLayout, this::updateMetaData, this::isDuplicate);

        buildLayout();
        addGridSelectionListener();
    }

    @Override
    protected String getEntityType() {
        return i18n.getMessage("caption.distribution");
    }

    @Override
    protected MetaData doCreateMetaData(final ProxyMetaData entity) {
        return dsManagement
                .createMetaData(masterEntityFilter,
                        Collections.singletonList(entityFactory.generateDsMetadata(entity.getKey(), entity.getValue())))
                .get(0);
    }

    @Override
    protected MetaData doUpdateMetaData(final ProxyMetaData entity) {
        return dsManagement.updateMetaData(masterEntityFilter,
                entityFactory.generateDsMetadata(entity.getKey(), entity.getValue()));
    }

    @Override
    protected void doDeleteMetaDataByKey(final String metaDataKey) {
        dsManagement.deleteMetaData(masterEntityFilter, metaDataKey);
    }

    private boolean isDuplicate(final String metaDataKey) {
        return dsManagement.getMetaDataByDistributionSetId(masterEntityFilter, metaDataKey).isPresent();
    }

    @Override
    protected MetaDataWindowGrid<Long> getMetaDataWindowGrid() {
        return dsMetaDataWindowGrid;
    }

    @Override
    public AddMetaDataWindowController getAddMetaDataWindowController() {
        return addDsMetaDataWindowController;
    }

    @Override
    public UpdateMetaDataWindowController getUpdateMetaDataWindowController() {
        return updateDsMetaDataWindowController;
    }

    @Override
    public MetaDataAddUpdateWindowLayout getMetaDataAddUpdateWindowLayout() {
        return metaDataAddUpdateWindowLayout;
    }

    @Override
    protected void publishEntityModifiedEvent() {
        eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_UPDATED, ProxyDistributionSet.class, masterEntityFilter));
    }
}
