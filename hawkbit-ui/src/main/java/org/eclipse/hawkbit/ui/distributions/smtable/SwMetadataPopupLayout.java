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
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.VerticalLayout;

/**
 * Pop up layout to display software module metadata.
 */
public class SwMetadataPopupLayout extends AbstractMetadataPopupLayout<SoftwareModule, SoftwareModuleMetadata> {

    private static final long serialVersionUID = 1L;

    protected static final String TARGET_VISIBLE = "targetVisible";

    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final transient EntityFactory entityFactory;
    private CheckBox targetVisibleField;

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
                .createMetaData(entityFactory.softwareModuleMetadata().create(entity.getId()).key(key).value(value)
                        .targetVisible(targetVisibleField.getValue()));
        setSelectedEntity(swMetadata.getSoftwareModule());
        return swMetadata;
    }

    @Override
    protected SoftwareModuleMetadata updateMetadata(final SoftwareModule entity, final String key, final String value) {
        final SoftwareModuleMetadata swMetadata = softwareModuleManagement
                .updateMetaData(entityFactory.softwareModuleMetadata().update(entity.getId(), key).value(value)
                        .targetVisible(targetVisibleField.getValue()));
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
    protected Grid createMetadataGrid() {
        final Grid metadataGrid = super.createMetadataGrid();
        metadataGrid.getContainerDataSource().addContainerProperty(TARGET_VISIBLE, Boolean.class, Boolean.FALSE);
        metadataGrid.getColumn(TARGET_VISIBLE).setHeaderCaption(i18n.getMessage("metadata.targetvisible"));
        metadataGrid.getColumn(TARGET_VISIBLE).setHidden(true);
        return metadataGrid;
    }

    @Override
    protected void deleteMetadata(final SoftwareModule entity, final String key) {
        softwareModuleManagement.deleteMetaData(entity.getId(), key);
    }

    @Override
    protected boolean hasCreatePermission() {
        return permChecker.hasCreateRepositoryPermission();
    }

    @Override
    protected boolean hasUpdatePermission() {
        return permChecker.hasUpdateRepositoryPermission();
    }

    private CheckBox createTargetVisibleField() {
        final CheckBox checkBox = new CheckBox();
        checkBox.setId(UIComponentIdProvider.METADATA_TARGET_VISIBLE_ID);
        checkBox.setCaption(i18n.getMessage("metadata.targetvisible"));
        checkBox.addValueChangeListener(this::onCheckBoxChange);

        return checkBox;
    }

    // Exception for squid:S1172 - parameter defined by Vaadin
    @SuppressWarnings("squid:S1172")
    private void onCheckBoxChange(final ValueChangeEvent event) {
        if (hasCreatePermission() || hasUpdatePermission()) {
            if (!getValueTextArea().getValue().isEmpty() && !getKeyTextField().getValue().isEmpty()) {
                getMetadataWindow().setSaveButtonEnabled(true);
            } else {
                getMetadataWindow().setSaveButtonEnabled(false);
            }
        }

    }

    @Override
    protected void createComponents() {
        super.createComponents();
        targetVisibleField = createTargetVisibleField();
    }

    @Override
    protected VerticalLayout createMetadataFieldsLayout() {

        final VerticalLayout metadataFieldsLayout = super.createMetadataFieldsLayout();
        metadataFieldsLayout.addComponent(targetVisibleField);
        return metadataFieldsLayout;
    }

    @Override
    protected Item popualateKeyValue(final Object metadataCompositeKey) {
        final Item item = super.popualateKeyValue(metadataCompositeKey);

        if (item != null) {
            targetVisibleField.setValue((Boolean) item.getItemProperty(TARGET_VISIBLE).getValue());
            if (hasUpdatePermission()) {
                targetVisibleField.setEnabled(true);
            }
        }

        return item;
    }

    @Override
    protected Item updateItemInGrid(final String key) {
        final Item item = super.updateItemInGrid(key);
        item.getItemProperty(TARGET_VISIBLE).setValue(targetVisibleField.getValue());

        return item;
    }

    @Override
    protected Item addItemToGrid(final SoftwareModuleMetadata metaData) {
        final Item item = super.addItemToGrid(metaData);
        item.getItemProperty(TARGET_VISIBLE).setValue(metaData.isTargetVisible());
        return item;
    }

    @Override
    protected void enableEditing() {
        super.enableEditing();
        targetVisibleField.setEnabled(true);
    }

    @Override
    protected void clearFields() {
        super.clearFields();
        targetVisibleField.clear();
    }

    @Override
    protected void disableEditing() {
        super.disableEditing();
        targetVisibleField.setEnabled(false);
    }
}
