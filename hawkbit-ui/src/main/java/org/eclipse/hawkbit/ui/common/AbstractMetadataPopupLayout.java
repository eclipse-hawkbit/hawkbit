/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlButtonRenderer;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleBorderWithIcon;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.decorators.SPUIWindowDecorator;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.SelectionEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window.WindowModeChangeEvent;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.themes.ValoTheme;

/**
 * 
 * Abstract pop up layout
 *
 * @param <E>
 *            E id the entity for which metadata is displayed
 * @param <M>
 *            M is the metadata
 * 
 */
public abstract class AbstractMetadataPopupLayout<E extends NamedVersionedEntity, M extends MetaData>
        extends CustomComponent {

    private static final String DELETE_BUTTON = "DELETE_BUTTON";

    private static final long serialVersionUID = -1491218218453167613L;

    private static final String VALUE = "value";

    private static final String KEY = "key";

    @Autowired
    protected I18N i18n;

    @Autowired
    private UINotification uiNotification;

    @Autowired
    protected transient EventBus.SessionEventBus eventBus;

    private TextField keyTextField;

    private TextArea valueTextArea;

    private Button addIcon;

    private Button discardButton;

    private Grid metaDataGrid;

    private Label headerCaption;

    private CommonDialogWindow metadataWindow;

    private E selectedEntity;

    private HorizontalLayout mainLayout;

    @PostConstruct
    private void init() {
        createComponents();
        buildLayout();

    }

    /**
     * Returns metadata popup.
     * 
     * @param entity
     *            entity for which metadata data is displayed
     * @param metaData
     *            metadata to be selected
     * @return @link{CommonDialogWindow}
     */
    public CommonDialogWindow getWindow(final E entity, final M metaData) {
        selectedEntity = entity;
        final String nameVersion = HawkbitCommonUtil.getFormattedNameVersion(entity.getName(), entity.getVersion());
        metadataWindow = SPUIWindowDecorator.getWindow(getMetadataCaption(nameVersion), null,
                SPUIDefinitions.CUSTOM_METADATA_WINDOW, this, event -> onSave(), event -> onCancel(), null, mainLayout,
                i18n);
        metadataWindow.setId(SPUIComponentIdProvider.METADATA_POPUP_ID);
        metadataWindow.setHeight(550, Unit.PIXELS);
        metadataWindow.setWidth(800, Unit.PIXELS);
        metadataWindow.getMainLayout().setSizeFull();
        metadataWindow.setResizable(true);
        metadataWindow.getButtonsLayout().setHeight("45px");
        metadataWindow.addWindowModeChangeListener(event -> onResize(event));
        ((HorizontalLayout) metadataWindow.getButtonsLayout()).addComponent(discardButton, 1);
        ((HorizontalLayout) metadataWindow.getButtonsLayout()).setComponentAlignment(discardButton,
                Alignment.MIDDLE_RIGHT);
        setUpDetails(entity.getId(), metaData);
        return metadataWindow;
    }

    public E getSelectedEntity() {
        return selectedEntity;
    }

    public void setSelectedEntity(final E selectedEntity) {
        this.selectedEntity = selectedEntity;
    }

    protected abstract void checkForDuplicate(E entity, String value);

    protected abstract M createMetadata(E entity, String key, String value);

    protected abstract M updateMetadata(E entity, String key, String value);

    protected abstract List<M> getMetadataList();

    protected abstract void deleteMetadata(E entity, String key, String value);

    protected abstract boolean hasCreatePermission();

    protected abstract boolean hasUpdatePermission();

    private void createComponents() {
        keyTextField = createKeyTextField();
        valueTextArea = createValueTextField();
        metaDataGrid = createMetadataGrid();
        addIcon = createAddIcon();
        headerCaption = createHeaderCaption();
        discardButton = createDiscardButton();
    }

    private void buildLayout() {
        final HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.addStyleName(SPUIStyleDefinitions.WIDGET_TITLE);
        headerLayout.setSpacing(false);
        headerLayout.setMargin(false);
        headerLayout.setSizeFull();
        headerLayout.addComponent(headerCaption);
        if (hasCreatePermission()) {
            headerLayout.addComponents(addIcon);
            headerLayout.setComponentAlignment(addIcon, Alignment.MIDDLE_RIGHT);
        }
        headerLayout.setExpandRatio(headerCaption, 1.0F);
        

        final HorizontalLayout headerWrapperLayout = new HorizontalLayout();
        headerWrapperLayout.addStyleName("bordered-layout" + " " + "no-border-bottom" + " " + "metadata-table-margin");
        headerWrapperLayout.addComponent(headerLayout);
        headerWrapperLayout.setWidth("100%");
        headerLayout.setHeight("30px");

        final VerticalLayout tableLayout = new VerticalLayout();
        tableLayout.setSizeFull();
        tableLayout.setHeight("100%");
        tableLayout.addComponent(headerWrapperLayout);
        tableLayout.addComponent(metaDataGrid);
        tableLayout.addStyleName("table-layout");
        tableLayout.setExpandRatio(metaDataGrid, 1.0F);

        final VerticalLayout metadataFieldsLayout = new VerticalLayout();
        metadataFieldsLayout.setSizeFull();
        metadataFieldsLayout.setHeight("100%");
        metadataFieldsLayout.addComponent(keyTextField);
        metadataFieldsLayout.addComponent(valueTextArea);
        metadataFieldsLayout.setSpacing(true);
        metadataFieldsLayout.setExpandRatio(valueTextArea, 1F);

        mainLayout = new HorizontalLayout();
        mainLayout.addComponent(tableLayout);
        mainLayout.addComponent(metadataFieldsLayout);
        mainLayout.setExpandRatio(tableLayout, 0.5F);
        mainLayout.setExpandRatio(metadataFieldsLayout, 0.5F);
        mainLayout.setSizeFull();
        mainLayout.setSpacing(true);
        setCompositionRoot(mainLayout);
        setSizeFull();
    }

    private TextField createKeyTextField() {
        final TextField keyField = SPUIComponentProvider.getTextField(i18n.get("textfield.key"), "",
                ValoTheme.TEXTFIELD_TINY, true, "", i18n.get("textfield.key"), true, 128);
        keyField.setId(SPUIComponentIdProvider.METADATA_KEY_FIELD_ID);
        keyField.addTextChangeListener(event -> onKeyChange(event));
        keyField.setTextChangeEventMode(TextChangeEventMode.EAGER);
        keyField.setWidth("100%");
        return keyField;
    }

    private TextArea createValueTextField() {
        valueTextArea = SPUIComponentProvider.getTextArea(i18n.get("textfield.value"), null, ValoTheme.TEXTAREA_TINY,
                true, null, i18n.get("textfield.value"), 4000);
        valueTextArea.setId(SPUIComponentIdProvider.METADATA_VALUE_ID);
        valueTextArea.setNullRepresentation("");
        valueTextArea.setSizeFull();
        valueTextArea.setHeight(100, Unit.PERCENTAGE);
        valueTextArea.addTextChangeListener(event -> onValueChange(event));
        valueTextArea.setTextChangeEventMode(TextChangeEventMode.EAGER);
        return valueTextArea;
    }

    private Grid createMetadataGrid() {
        final Grid metadataGrid = new Grid();
        metadataGrid.addStyleName(SPUIStyleDefinitions.METADATA_GRID);
        metadataGrid.setImmediate(true);
        metadataGrid.setHeight("100%");
        metadataGrid.setWidth("100%");
        metadataGrid.setId(SPUIComponentIdProvider.METDATA_TABLE_ID);
        metadataGrid.setSelectionMode(SelectionMode.SINGLE);
        metadataGrid.setColumnReorderingAllowed(true);
        metadataGrid.setContainerDataSource(getMetadataContainer());
        metadataGrid.getColumn(KEY).setHeaderCaption(i18n.get("header.key"));
        metadataGrid.getColumn(VALUE).setHeaderCaption(i18n.get("header.value"));
        metadataGrid.getColumn(VALUE).setHidden(true);
        metadataGrid.addSelectionListener(event -> onRowClick(event));
        metadataGrid.getColumn(DELETE_BUTTON).setHeaderCaption("");
        metadataGrid.getColumn(DELETE_BUTTON).setRenderer(new HtmlButtonRenderer(event -> onDelete(event)));
        metadataGrid.getColumn(DELETE_BUTTON).setWidth(50);
        metadataGrid.getColumn(KEY).setExpandRatio(1);
        return metadataGrid;
    }

    private void onDelete(final RendererClickEvent event) {
        final Item item = metaDataGrid.getContainerDataSource().getItem(event.getItemId());
        final String key = (String) item.getItemProperty(KEY).getValue();
        final String value = (String) item.getItemProperty(VALUE).getValue();

        final ConfirmationDialog confirmDialog = new ConfirmationDialog(
                i18n.get("caption.metadata.delete.action.confirmbox"), i18n.get("message.confirm.delete.metadata", key),
                i18n.get("button.ok"), i18n.get("button.cancel"), ok -> {
                    if (ok) {
                        deleteMetadata(getSelectedEntity(), key, value);
                        uiNotification.displaySuccess(i18n.get("message.metadata.deleted.successfully", key));
                        final Object selectedRow = metaDataGrid.getSelectedRow();
                        metaDataGrid.getContainerDataSource().removeItem(event.getItemId());
                        // force grid to refresh
                        metaDataGrid.clearSortOrder();
                        if (!metaDataGrid.getContainerDataSource().getItemIds().isEmpty()) {
                            if (selectedRow != null) {
                                if (selectedRow.equals(event.getItemId())) {
                                    metaDataGrid.select(metaDataGrid.getContainerDataSource().getIdByIndex(0));
                                } else {
                                    metaDataGrid.select(selectedRow);
                                }
                            }
                        } else {
                            keyTextField.clear();
                            valueTextArea.clear();
                            metaDataGrid.select(null);
                            if (hasCreatePermission()) {
                                keyTextField.setEnabled(true);
                                valueTextArea.setEnabled(true);
                                addIcon.setEnabled(false);
                            }
                        }
                    }
                });
        UI.getCurrent().addWindow(confirmDialog.getWindow());
        confirmDialog.getWindow().bringToFront();
    }

    private Button createAddIcon() {
        addIcon = SPUIComponentProvider.getButton(SPUIComponentIdProvider.METADTA_ADD_ICON_ID, i18n.get("button.save"),
                null, null, false, FontAwesome.PLUS, SPUIButtonStyleSmallNoBorder.class);
        addIcon.addClickListener(event -> onAdd(event));
        return addIcon;
    }

    private Label createHeaderCaption() {
        final Label captionLabel = SPUIComponentProvider.getLabel(i18n.get("caption.metadata"),
                SPUILabelDefinitions.SP_WIDGET_CAPTION);
        return captionLabel;
    }

    private IndexedContainer getMetadataContainer() {
        final IndexedContainer swcontactContainer = new IndexedContainer();
        swcontactContainer.addContainerProperty(KEY, String.class, "");
        swcontactContainer.addContainerProperty(VALUE, String.class, "");
        swcontactContainer.addContainerProperty(DELETE_BUTTON, String.class, FontAwesome.TRASH_O.getHtml());
        return swcontactContainer;
    }

    private void popualateKeyValue(final Object metadataCompositeKey) {
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

    private void populateGrid() {
        final List<M> metadataList = getMetadataList();
        for (final M metaData : metadataList) {
            addItemToGrid(metaData.getKey(), metaData.getValue());
        }
    }

    private void addItemToGrid(final String key, final String value) {
        final IndexedContainer metadataContainer = (IndexedContainer) metaDataGrid.getContainerDataSource();
        final Item item = metadataContainer.addItem(key);
        item.getItemProperty(VALUE).setValue(value);
        item.getItemProperty(KEY).setValue(key);
    }

    private void updateItemInGrid(final String key) {
        final IndexedContainer metadataContainer = (IndexedContainer) metaDataGrid.getContainerDataSource();
        final Item item = metadataContainer.getItem(key);
        item.getItemProperty(VALUE).setValue(valueTextArea.getValue());
    }

    private void onAdd(final ClickEvent event) {
        metaDataGrid.deselect(metaDataGrid.getSelectedRow());
        valueTextArea.clear();
        keyTextField.clear();
        keyTextField.setEnabled(true);
        valueTextArea.setEnabled(true);
        addIcon.setEnabled(true);
    }

    private void onSave() {
        final String key = keyTextField.getValue();
        final String value = valueTextArea.getValue();
        if (mandatoryCheck()) {
            final E entity = selectedEntity;
            if (metaDataGrid.getSelectedRow() == null) {
                if (!duplicateCheck(entity)) {
                    final M metadata = createMetadata(entity, key, value);
                    uiNotification.displaySuccess(i18n.get("message.metadata.saved", metadata.getKey()));
                    addItemToGrid(metadata.getKey(), metadata.getValue());
                    metaDataGrid.scrollToEnd();
                    metaDataGrid.select(metadata.getKey());
                    addIcon.setEnabled(true);
                    metadataWindow.setSaveButtonEnabled(false);
                    setDiscardButtonEnabled(false);
                    if (!hasUpdatePermission()) {
                        valueTextArea.setEnabled(false);
                    }
                }
            } else {
                final M metadata = updateMetadata(entity, key, value);
                uiNotification.displaySuccess(i18n.get("message.metadata.updated", metadata.getKey()));
                updateItemInGrid(metadata.getKey());
                metaDataGrid.select(metadata.getKey());
                addIcon.setEnabled(true);
                metadataWindow.setSaveButtonEnabled(false);
                setDiscardButtonEnabled(false);
            }
        }
    }

    private boolean mandatoryCheck() {
        if (keyTextField.getValue().isEmpty()) {
            uiNotification.displayValidationError(i18n.get("message.key.missing"));
            return false;
        }
        if (valueTextArea.getValue().isEmpty()) {
            uiNotification.displayValidationError(i18n.get("message.value.missing"));
            return false;
        }
        return true;
    }

    private boolean duplicateCheck(final E entity) {
        try {
            checkForDuplicate(entity, keyTextField.getValue());
        } catch (final EntityNotFoundException exception) {
            return false;
        }
        uiNotification.displayValidationError(i18n.get("message.metadata.duplicate.check", keyTextField.getValue()));
        return true;
    }

    private String getMetadataCaption(final String nameVersionStr) {
        final StringBuilder caption = new StringBuilder();
        caption.append(HawkbitCommonUtil.DIV_DESCRIPTION + i18n.get("caption.metadata.popup") + " "
                + HawkbitCommonUtil.getBoldHTMLText(nameVersionStr));
        caption.append(HawkbitCommonUtil.DIV_CLOSE);
        return caption.toString();
    }

    private void onDiscard() {
        if (metaDataGrid.getSelectedRow() == null) {
            keyTextField.clear();
            valueTextArea.clear();
        } else {
            final Object itemSelected = metaDataGrid.getSelectedRow();
            popualateKeyValue(itemSelected);
        }
        metadataWindow.setSaveButtonEnabled(false);
        setDiscardButtonEnabled(false);
    }

    private void onCancel() {
        metadataWindow.close();
        UI.getCurrent().removeWindow(metadataWindow);
    }

    private void onKeyChange(final TextChangeEvent event) {
        if (hasCreatePermission() || hasUpdatePermission()) {
            if (!valueTextArea.getValue().isEmpty() && !event.getText().isEmpty()) {
                metadataWindow.setSaveButtonEnabled(true);
                setDiscardButtonEnabled(true);
            } else {
                metadataWindow.setSaveButtonEnabled(false);
                setDiscardButtonEnabled(false);
            }
        }
    }

    private void onRowClick(final SelectionEvent event) {
        final Set<Object> itemsSelected = event.getSelected();
        if (!itemsSelected.isEmpty()) {
            final Object itemSelected = itemsSelected.stream().findFirst().isPresent()
                    ? itemsSelected.stream().findFirst().get() : null;
            popualateKeyValue(itemSelected);
            addIcon.setEnabled(true);
        } else {
            keyTextField.clear();
            valueTextArea.clear();
            if (hasCreatePermission()) {
                keyTextField.setEnabled(true);
                valueTextArea.setEnabled(true);
                addIcon.setEnabled(false);
            } else {
                keyTextField.setEnabled(false);
                valueTextArea.setEnabled(false);
            }
        }
        metadataWindow.setSaveButtonEnabled(false);
        setDiscardButtonEnabled(false);
    }

    private void onValueChange(final TextChangeEvent event) {
        if (hasCreatePermission() || hasUpdatePermission()) {
            if (!keyTextField.getValue().isEmpty() && !event.getText().isEmpty()) {
                metadataWindow.setSaveButtonEnabled(true);
                setDiscardButtonEnabled(true);
            } else {
                metadataWindow.setSaveButtonEnabled(false);
                setDiscardButtonEnabled(false);
            }
        }
    }

    private void onResize(final WindowModeChangeEvent event) {
        if (event.getWindowMode() == WindowMode.MAXIMIZED) {
            metaDataGrid.getColumn(DELETE_BUTTON).setWidth(70);
        } else {
            metaDataGrid.getColumn(DELETE_BUTTON).setWidth(50);
        }
        // Repopulating the grid (forcing for repaint)- workaround as grid size
        // is not getting adjusted
        final Map<String, String> keyValueDetails = new LinkedHashMap<>();
        for (final Object key : metaDataGrid.getContainerDataSource().getItemIds()) {
            final Item item = metaDataGrid.getContainerDataSource().getItem(key);
            final String value = (String) item.getItemProperty(VALUE).getValue();
            keyValueDetails.put((String) key, value);
        }
        metaDataGrid.getContainerDataSource().removeAllItems();
        for (final Entry<String, String> entry : keyValueDetails.entrySet()) {
            addItemToGrid(entry.getKey(), entry.getValue());
        }
    }

    private Button createDiscardButton() {
        final Button discardButton = SPUIComponentProvider.getButton(SPUIComponentIdProvider.CANCEL_BUTTON,
                i18n.get("button.discard"), "", "", true, FontAwesome.UNDO, SPUIButtonStyleBorderWithIcon.class);
        discardButton.setSizeUndefined();
        discardButton.addStyleName("default-color");
        discardButton.addClickListener(event -> onDiscard());
        return discardButton;
    }

    private void setUpDetails(final Long swId, final M metaData) {
        resetDetails();
        if (swId != null) {
            metaDataGrid.getContainerDataSource().removeAllItems();
            populateGrid();
            metaDataGrid.getSelectionModel().reset();
            if (!metaDataGrid.getContainerDataSource().getItemIds().isEmpty()) {
                if (metaData == null) {
                    metaDataGrid.select(metaDataGrid.getContainerDataSource().getIdByIndex(0));
                } else {
                    metaDataGrid.select(metaData.getKey());
                }
            } else if (hasCreatePermission()) {
                keyTextField.setEnabled(true);
                valueTextArea.setEnabled(true);
                addIcon.setEnabled(false);
            }
        }
    }

    private void resetDetails() {
        keyTextField.clear();
        valueTextArea.clear();
        keyTextField.setEnabled(false);
        valueTextArea.setEnabled(false);
        metadataWindow.setSaveButtonEnabled(false);
        setDiscardButtonEnabled(false);
        addIcon.setEnabled(true);
    }

    private void setDiscardButtonEnabled(final Boolean enable) {
        discardButton.setEnabled(enable);
    }

}