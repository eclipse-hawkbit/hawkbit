/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import static org.eclipse.hawkbit.ui.common.AbstractMetadataPopupLayout.KEY;
import static org.eclipse.hawkbit.ui.common.AbstractMetadataPopupLayout.VALUE;

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

import com.vaadin.data.Item;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.event.SelectionEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.VerticalLayout;

/**
 * Pop up layout to display software module metadata.
 */
public class SwMetadataPopupLayout extends AbstractMetadataPopupLayout<SoftwareModule, SoftwareModuleMetadata> {

    private static final long serialVersionUID = -1252090014161012563L;

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
        return permChecker.hasCreateRepositoryPermission();
    }

    @Override
    protected boolean hasUpdatePermission() {
        return permChecker.hasUpdateRepositoryPermission();
    }

    private CheckBox createTargetVisibleField() {
        final CheckBox checkBox = new CheckBox();
        checkBox.setCaption(i18n.getMessage("textfield.key"));
        checkBox.addValueChangeListener(this::onCheckBoxChange);

        return checkBox;
    }

    public void onCheckBoxChange(final ValueChangeEvent event) {
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
        final VerticalLayout metadataFieldsLayout = new VerticalLayout();
        metadataFieldsLayout.setSizeFull();
        metadataFieldsLayout.setHeight("100%");
        metadataFieldsLayout.addComponent(getKeyTextField());
        metadataFieldsLayout.addComponent(targetVisibleField);
        metadataFieldsLayout.addComponent(getValueTextArea());
        metadataFieldsLayout.setSpacing(true);
        metadataFieldsLayout.setExpandRatio(getKeyTextField(), 1F);
        return metadataFieldsLayout;
    }

    @Override
    protected void popualateKeyValue(final Object metadataCompositeKey) {
        super.popualateKeyValue(metadataCompositeKey);
        if (metadataCompositeKey != null) {
            final Item item = metaDataGrid.getContainerDataSource().getItem(metadataCompositeKey);
            keyTextField.setValue((String) item.getItemProperty(KEY).getValue());
            valueTextArea.setValue((String) item.getItemProperty(VALUE).getValue());
            keyTextField.setEnabled(false);
            if (hasUpdatePermission()) {
                valueTextArea.setEnabled(true);
            }
        }
    }

    @Override
    protected void clearTextFields() {
        super.clearTextFields();
        // FIXME
    }

    @Override
    protected void onAdd() {
        super.onAdd();
        // FIXME
    }

    @Override
    protected void onSave() {
        super.onSave();
        // FIXME
    }

    @Override
    protected void onRowClick(final SelectionEvent event) {
        super.onRowClick(event);
        // FIXME
    }

    @Override
    protected void setUpDetails(final Long swId, final String metaDatakey) {
        super.setUpDetails(swId, metaDatakey);
        // FIXME
    }

    @Override
    protected void resetDetails() {
        super.resetDetails();
        // FIXME
    }
}
