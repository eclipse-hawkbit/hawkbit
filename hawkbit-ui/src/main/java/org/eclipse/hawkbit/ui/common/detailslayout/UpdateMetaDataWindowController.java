/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.function.Function;

import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Controller for update meta data window
 */
public class UpdateMetaDataWindowController extends AbstractEntityWindowController<ProxyMetaData, ProxyMetaData> {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateMetaDataWindowController.class);

    private final MetaDataAddUpdateWindowLayout layout;
    private final Function<ProxyMetaData, MetaData> updateMetaDataCallback;

    /**
     * Constructor for UpdateMetaDataWindowController
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param layout
     *            MetaDataAddUpdateWindowLayout
     * @param updateMetaDataCallback
     *            Update meta data call back function for event listener
     *
     */
    public UpdateMetaDataWindowController(final UIConfiguration uiConfig, final MetaDataAddUpdateWindowLayout layout,
            final Function<ProxyMetaData, MetaData> updateMetaDataCallback) {
        super(uiConfig);

        this.layout = layout;
        this.updateMetaDataCallback = updateMetaDataCallback;
    }

    @Override
    protected ProxyMetaData buildEntityFromProxy(final ProxyMetaData proxyEntity) {
        final ProxyMetaData metaData = new ProxyMetaData();

        metaData.setKey(proxyEntity.getKey());
        metaData.setValue(proxyEntity.getValue());
        metaData.setEntityId(proxyEntity.getEntityId());
        metaData.setVisibleForTargets(proxyEntity.isVisibleForTargets());

        return metaData;
    }

    @Override
    public EntityWindowLayout<ProxyMetaData> getLayout() {
        return layout;
    }

    @Override
    protected void adaptLayout(final ProxyMetaData proxyEntity) {
        layout.disableMetadataKey();
    }

    @Override
    protected void persistEntity(final ProxyMetaData entity) {
        try {
            final MetaData updatedMetaData = updateMetaDataCallback.apply(entity);
            displaySuccess("message.metadata.updated", updatedMetaData.getKey());
        } catch (final EntityNotFoundException | EntityReadOnlyException e) {
            LOG.trace("Update of meta data failed in UI: {}", e.getMessage());
            final String entityType = getI18n().getMessage("caption.metadata");
            displayWarning("message.key.deleted.or.notAllowed", entityType, entity.getKey());
        }
    }

    @Override
    protected boolean isEntityValid(final ProxyMetaData entity) {
        if (!StringUtils.hasText(entity.getKey())) {
            displayValidationError("message.key.missing");
            return false;
        }

        if (!StringUtils.hasText(entity.getValue())) {
            displayValidationError("message.value.missing");
            return false;
        }

        return true;
    }

    @Override
    protected boolean closeWindowAfterSave() {
        return false;
    }
}
