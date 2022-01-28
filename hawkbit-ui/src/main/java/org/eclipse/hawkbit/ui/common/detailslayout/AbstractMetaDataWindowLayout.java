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
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyMetaData;
import org.eclipse.hawkbit.ui.common.layout.AbstractGridComponentLayout;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.ComponentContainer;
import com.vaadin.ui.HorizontalLayout;

/**
 * Class for metadata add/update window layout.
 *
 * @param <F>
 *            Generic type
 */
public abstract class AbstractMetaDataWindowLayout<F> extends HorizontalLayout {
    private static final long serialVersionUID = 1L;

    protected final VaadinMessageSource i18n;
    protected final UINotification uiNotification;
    protected final SpPermissionChecker permChecker;
    protected final transient UIEventBus eventBus;
    protected final transient EntityFactory entityFactory;
    protected final transient CommonUiDependencies uiDependencies;

    private final MetadataWindowGridHeader metadataWindowGridHeader;

    private transient Consumer<SaveDialogCloseListener> saveCallback;

    protected transient F masterEntityFilter;

    /**
     * Constructor for AbstractTagWindowLayout
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    protected AbstractMetaDataWindowLayout(final CommonUiDependencies uiDependencies) {
        this.uiDependencies = uiDependencies;
        this.i18n = uiDependencies.getI18n();
        this.eventBus = uiDependencies.getEventBus();
        this.uiNotification = uiDependencies.getUiNotification();
        this.permChecker = uiDependencies.getPermChecker();
        this.entityFactory = uiDependencies.getEntityFactory();

        this.metadataWindowGridHeader = new MetadataWindowGridHeader(uiDependencies, this::hasMetadataChangePermission,
                this::showAddMetaDataLayout);
    }

    // can be overriden in child classes for entity-specific permission
    protected boolean hasMetadataChangePermission() {
        return permChecker.hasUpdateRepositoryPermission();
    }

    protected MetaData createMetaData(final ProxyMetaData entity) {
        final MetaData newMetaData = doCreateMetaData(entity);

        entity.setEntityId(newMetaData.getEntityId());
        onMetaDataModified(entity);

        return newMetaData;
    }

    protected abstract MetaData doCreateMetaData(final ProxyMetaData entity);

    private void onMetaDataModified(final ProxyMetaData metaData) {
        publishEntityModifiedEvent();

        getMetaDataWindowGrid().refreshAll();
        getMetaDataWindowGrid().select(metaData);

        resetSaveButton();
    }

    protected abstract void publishEntityModifiedEvent();

    protected abstract MetaDataWindowGrid<F> getMetaDataWindowGrid();

    private void resetSaveButton() {
        // used to disable save button after setting the initial bean values
        getMetaDataAddUpdateWindowLayout().getValidationCallback()
                .ifPresent(validationCallback -> validationCallback.accept(false));
    }

    protected MetaData updateMetaData(final ProxyMetaData entity) {
        final MetaData updatedMetaData = doUpdateMetaData(entity);

        onMetaDataModified(entity);

        return updatedMetaData;
    }

    protected abstract MetaData doUpdateMetaData(final ProxyMetaData entity);

    protected boolean deleteMetaData(final Collection<ProxyMetaData> metaDataToDelete) {
        if (!StringUtils.isEmpty(masterEntityFilter) && !CollectionUtils.isEmpty(metaDataToDelete)) {
            // as of now we only allow deletion of single metadata entry
            final String metaDataKey = metaDataToDelete.iterator().next().getKey();
            doDeleteMetaDataByKey(metaDataKey);

            publishEntityModifiedEvent();

            getMetaDataWindowGrid().refreshAll();

            return true;
        } else {
            uiNotification.displayValidationError(i18n.getMessage("message.error.deleteMetaData", getEntityType()));

            return false;
        }
    }

    protected abstract String getEntityType();

    protected abstract void doDeleteMetaDataByKey(final String metaDataKey);

    private void showAddMetaDataLayout() {
        getMetaDataWindowGrid().deselectAll();
        getAddMetaDataWindowController().populateWithData(null);
        saveCallback.accept(getAddMetaDataWindowController().getSaveDialogCloseListener());

        resetSaveButton();
    }

    /**
     * @return add meta data window controller
     */
    public abstract AddMetaDataWindowController getAddMetaDataWindowController();

    /**
     * @return Meta data add and update window
     */
    public abstract MetaDataAddUpdateWindowLayout getMetaDataAddUpdateWindowLayout();

    /**
     * Validation listener for input values
     *
     * @param validationCallback
     *            ValidationCallback for event listener
     */
    public void addValidationListener(final Consumer<Boolean> validationCallback) {
        getMetaDataAddUpdateWindowLayout().addValidationListener(validationCallback);
    }

    /**
     * Set master entity filter
     *
     * @param masterEntityFilter
     *            Generic type
     * @param metaData
     *            ProxyMetaData
     */
    public void setMasterEntityFilter(final F masterEntityFilter, final ProxyMetaData metaData) {
        this.masterEntityFilter = masterEntityFilter;

        getMetaDataWindowGrid().masterEntityChanged(masterEntityFilter);

        if (metaData == null) {
            if (!getMetaDataWindowGrid().getSelectionSupport().selectFirstRow()) {
                showAddMetaDataLayout();
            }
        } else {
            getMetaDataWindowGrid().select(metaData);
        }
    }

    /**
     * Set save call back listener
     *
     * @param saveCallback
     *            SaveDialogCloseListener
     */
    public void setSaveCallback(final Consumer<SaveDialogCloseListener> saveCallback) {
        this.saveCallback = saveCallback;
    }

    protected void buildLayout() {
        setSpacing(true);
        setMargin(false);
        setSizeFull();

        final MetaDataWindowGridLayout metaDataWindowGridLayout = new MetaDataWindowGridLayout(metadataWindowGridHeader,
                getMetaDataWindowGrid());
        final ComponentContainer addUpdateWindowLayout = getMetaDataAddUpdateWindowLayout().getRootComponent();

        addComponent(metaDataWindowGridLayout);
        addComponent(addUpdateWindowLayout);

        setExpandRatio(metaDataWindowGridLayout, 0.5F);
        setExpandRatio(addUpdateWindowLayout, 0.5F);
    }

    protected void addGridSelectionListener() {
        getMetaDataWindowGrid().addSelectionListener(event -> {
            final Optional<ProxyMetaData> selectedEntity = event.getFirstSelectedItem();
            selectedEntity.ifPresent(this::showEditMetaDataLayout);

            if (!selectedEntity.isPresent()) {
                showAddMetaDataLayout();
            }
        });
    }

    private void showEditMetaDataLayout(final ProxyMetaData proxyEntity) {
        getUpdateMetaDataWindowController().populateWithData(proxyEntity);
        saveCallback.accept(getUpdateMetaDataWindowController().getSaveDialogCloseListener());

        resetSaveButton();
    }

    /**
     * @return UpdateMetaDataWindowController
     */
    public abstract UpdateMetaDataWindowController getUpdateMetaDataWindowController();

    private static class MetaDataWindowGridLayout extends AbstractGridComponentLayout {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor for MetaDataWindowGridLayout
         *
         * @param metadataWindowGridHeader
         *            MetadataWindowGridHeader
         * @param metaDataWindowGrid
         *            MetaDataWindowGrid
         */
        public MetaDataWindowGridLayout(final MetadataWindowGridHeader metadataWindowGridHeader,
                final MetaDataWindowGrid<?> metaDataWindowGrid) {
            super.buildLayout(metadataWindowGridHeader, metaDataWindowGrid);
        }
    }
}
