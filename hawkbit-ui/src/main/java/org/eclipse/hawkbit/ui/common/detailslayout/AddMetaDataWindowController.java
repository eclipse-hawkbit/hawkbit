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
import java.util.function.Predicate;

import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.springframework.util.StringUtils;

/**
 * Controller to add meta data window
 */
public class AddMetaDataWindowController extends AbstractEntityWindowController<ProxyMetaData, ProxyMetaData> {
    private final MetaDataAddUpdateWindowLayout layout;

    private final Function<ProxyMetaData, MetaData> createMetaDataCallback;
    private final Predicate<String> duplicateCheckCallback;

    /**
     * Constructor for AddMetaDataWindowController
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param layout
     *            MetaDataAddUpdateWindowLayout
     * @param createMetaDataCallback
     *            Create meta data callback
     * @param duplicateCheckCallback
     *            Duplicate check callback
     */
    public AddMetaDataWindowController(final UIConfiguration uiConfig, final MetaDataAddUpdateWindowLayout layout,
            final Function<ProxyMetaData, MetaData> createMetaDataCallback,
            final Predicate<String> duplicateCheckCallback) {
        super(uiConfig);
        
        this.layout = layout;

        this.createMetaDataCallback = createMetaDataCallback;
        this.duplicateCheckCallback = duplicateCheckCallback;
    }

    @Override
    protected ProxyMetaData buildEntityFromProxy(final ProxyMetaData proxyEntity) {
        // We ignore the method parameter, because we are interested in the
        // empty object, that we can populate with defaults
        return new ProxyMetaData();
    }

    @Override
    public EntityWindowLayout<ProxyMetaData> getLayout() {
        return layout;
    }

    @Override
    protected void adaptLayout(final ProxyMetaData proxyEntity) {
        layout.enableMetadataKey();
    }

    @Override
    protected void persistEntity(final ProxyMetaData entity) {
        final MetaData newMetaData = createMetaDataCallback.apply(entity);
        uiNotification.displaySuccess(i18n.getMessage("message.metadata.saved", newMetaData.getKey()));
    }

    @Override
    protected boolean isEntityValid(final ProxyMetaData entity) {
        if (!StringUtils.hasText(entity.getKey())) {
            uiNotification.displayValidationError(i18n.getMessage("message.key.missing"));
            return false;
        }

        if (!StringUtils.hasText(entity.getValue())) {
            uiNotification.displayValidationError(i18n.getMessage("message.value.missing"));
            return false;
        }

        final String trimmedKey = StringUtils.trimWhitespace(entity.getKey());
        if (duplicateCheckCallback.test(trimmedKey)) {
            uiNotification.displayValidationError(i18n.getMessage("message.metadata.duplicate.check", trimmedKey));
            return false;
        }

        return true;
    }

    @Override
    protected boolean closeWindowAfterSave() {
        return false;
    }
}
