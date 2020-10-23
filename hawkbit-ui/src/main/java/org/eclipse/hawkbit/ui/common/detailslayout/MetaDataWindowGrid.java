/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.Collection;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.builder.GridComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.providers.AbstractMetaDataDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.common.grid.support.DeleteSupport;
import org.eclipse.hawkbit.ui.common.grid.support.FilterSupport;
import org.eclipse.hawkbit.ui.common.grid.support.SelectionSupport;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Grid for MetaData pop up layout.
 *
 * @param <F>
 *            Generic type
 */
public class MetaDataWindowGrid<F> extends AbstractGrid<ProxyMetaData, F> implements MasterEntityAwareComponent<F> {
    private static final long serialVersionUID = 1L;

    public static final String META_DATA_KEY_ID = "metaDataKey";
    public static final String META_DATA_VALUE_ID = "metaDataValue";
    public static final String META_DATA_DELETE_BUTTON_ID = "metaDataDeleteButton";

    private final transient BooleanSupplier hasMetadataChangePermission;
    private final transient DeleteSupport<ProxyMetaData> metaDataDeleteSupport;

    /**
     * Constructor for MetaDataWindowGrid
     *
     * @param i18n
     *            VaadinMessageSource
     * @param eventBus
     *            UIEventBus
     * @param permissionChecker
     *            SpPermissionChecker
     * @param notification
     *            UINotification
     * @param dataProvider
     *            provides entity-specific metadata entities
     * @param hasMetadataChangePermission
     *            checks the permission allowing to change metadata entities
     * @param itemsDeletionCallback
     *            callback method to delete metadata entities
     *
     */
    public MetaDataWindowGrid(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker, final UINotification notification,
            final AbstractMetaDataDataProvider<?, F> dataProvider, final BooleanSupplier hasMetadataChangePermission,
            final Predicate<Collection<ProxyMetaData>> itemsDeletionCallback) {
        super(i18n, eventBus, permissionChecker);

        this.hasMetadataChangePermission = hasMetadataChangePermission;
        this.metaDataDeleteSupport = new DeleteSupport<>(this, i18n, notification, "caption.metadata",
                "caption.metadata.plur", ProxyMetaData::getKey, itemsDeletionCallback,
                UIComponentIdProvider.METADATA_DELETE_CONFIRMATION_DIALOG);

        setFilterSupport(new FilterSupport<>(dataProvider));

        // we don't need to send selection events, because details layout
        // is part of MetaData Window
        setSelectionSupport(new SelectionSupport<ProxyMetaData>(this));
        getSelectionSupport().enableSingleSelection();

        init();
    }

    @Override
    public String getGridId() {
        return UIComponentIdProvider.METADATA_WINDOW_TABLE_ID;
    }

    @Override
    public void addColumns() {
        GridComponentBuilder.addColumn(this, ProxyMetaData::getKey).setId(META_DATA_KEY_ID)
                .setCaption(i18n.getMessage("header.key"));

        GridComponentBuilder.addColumn(this, ProxyMetaData::getValue).setId(META_DATA_VALUE_ID)
                .setCaption(i18n.getMessage("header.value")).setHidden(true).setHidable(true);

        GridComponentBuilder.addDeleteColumn(this, i18n, META_DATA_DELETE_BUTTON_ID, metaDataDeleteSupport,
                UIComponentIdProvider.META_DATA_DELET_ICON, e -> hasMetadataChangePermission.getAsBoolean());
    }

    @Override
    public void masterEntityChanged(final F masterEntity) {
        getFilterSupport().setFilter(masterEntity);
        getFilterSupport().refreshFilter();
    }
}
