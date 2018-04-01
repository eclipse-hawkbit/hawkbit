/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import java.util.List;
import java.util.Set;

import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextAreaBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlButtonRenderer;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.SelectionEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;

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

    protected static final String VALUE = "value";

    protected static final String KEY = "key";

    protected static final int MAX_METADATA_QUERY = 500;

    protected VaadinMessageSource i18n;

    private final UINotification uiNotification;

    protected transient EventBus.UIEventBus eventBus;

    private TextField keyTextField;

    private TextArea valueTextArea;

    private Button addIcon;

    private Grid metaDataGrid;

    private Label headerCaption;

    private CommonDialogWindow metadataWindow;

    private E selectedEntity;

    private HorizontalLayout mainLayout;
    protected SpPermissionChecker permChecker;

    protected AbstractMetadataPopupLayout(final VaadinMessageSource i18n, final UINotification uiNotification,
            final UIEventBus eventBus, final SpPermissionChecker permChecker) {
        this.i18n = i18n;
        this.uiNotification = uiNotification;
        this.eventBus = eventBus;
        this.permChecker = permChecker;

        createComponents();
        buildLayout();
    }

    /**
     * Save the metadata and never close the window after saving.
     */
    private final class SaveOnDialogCloseListener implements SaveDialogCloseListener {
        @Override
        public void saveOrUpdate() {
            onSave();
        }

        @Override
        public boolean canWindowClose() {
            return false;
        }

        @Override
        public boolean canWindowSaveOrUpdate() {
            return true;
        }

    }

    /**
     * Returns metadata popup.
     * 
     * @param entity
     *            entity for which metadata data is displayed
     * @param metaDatakey
     *            metadata key to be selected
     * @return @link{CommonDialogWindow}
     */
    public CommonDialogWindow getWindow(final E entity, final String metaDatakey) {
        selectedEntity = entity;
        final String nameVersion = HawkbitCommonUtil.getFormattedNameVersion(entity.getName(), entity.getVersion());

        metadataWindow = new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW)
                .caption(getMetadataCaption(nameVersion)).content(this).cancelButtonClickListener(event -> onCancel())
                .id(UIComponentIdProvider.METADATA_POPUP_ID).layout(mainLayout).i18n(i18n)
                .saveDialogCloseListener(new SaveOnDialogCloseListener()).buildCommonDialogWindow();

        metadataWindow.setHeight(550, Unit.PIXELS);
        metadataWindow.setWidth(800, Unit.PIXELS);
        metadataWindow.getMainLayout().setSizeFull();
        metadataWindow.getButtonsLayout().setHeight("45px");
        setUpDetails(entity.getId(), metaDatakey);
        return metadataWindow;
    }

    public E getSelectedEntity() {
        return selectedEntity;
    }

    public void setSelectedEntity(final E selectedEntity) {
        this.selectedEntity = selectedEntity;
    }

    protected abstract boolean checkForDuplicate(E entity, String value);

    protected abstract M createMetadata(E entity, String key, String value);

    protected abstract M updateMetadata(E entity, String key, String value);

    protected abstract List<M> getMetadataList();

    protected abstract void deleteMetadata(E entity, String key);

    protected abstract boolean hasCreatePermission();

    protected abstract boolean hasUpdatePermission();

    protected void createComponents() {
        keyTextField = createKeyTextField();
        valueTextArea = createValueTextField();
        metaDataGrid = createMetadataGrid();
        addIcon = createAddIcon();
        headerCaption = createHeaderCaption();
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

        final VerticalLayout metadataFieldsLayout = createMetadataFieldsLayout();

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

    protected VerticalLayout createMetadataFieldsLayout() {
        final VerticalLayout metadataFieldsLayout = new VerticalLayout();
        metadataFieldsLayout.setSizeFull();
        metadataFieldsLayout.setHeight("100%");
        metadataFieldsLayout.addComponent(keyTextField);
        metadataFieldsLayout.addComponent(valueTextArea);
        metadataFieldsLayout.setSpacing(true);
        metadataFieldsLayout.setExpandRatio(valueTextArea, 1F);
        return metadataFieldsLayout;
    }

    private TextField createKeyTextField() {
        final TextField keyField = new TextFieldBuilder().caption(i18n.getMessage("textfield.key")).required(true)
                .prompt(i18n.getMessage("textfield.key")).immediate(true)
                .id(UIComponentIdProvider.METADATA_KEY_FIELD_ID).maxLengthAllowed(MetaData.KEY_MAX_SIZE)
                .validator(new EmptyStringValidator(i18n)).buildTextComponent();
        keyField.addTextChangeListener(this::onKeyChange);
        keyField.setTextChangeEventMode(TextChangeEventMode.EAGER);
        keyField.setWidth("100%");
        return keyField;
    }

    private TextArea createValueTextField() {
        valueTextArea = new TextAreaBuilder().caption(i18n.getMessage("textfield.value")).required(true)
                .validator(new EmptyStringValidator(i18n)).prompt(i18n.getMessage("textfield.value")).immediate(true)
                .id(UIComponentIdProvider.METADATA_VALUE_ID).maxLengthAllowed(MetaData.VALUE_MAX_SIZE)
                .buildTextComponent();
        valueTextArea.setNullRepresentation("");
        valueTextArea.setSizeFull();
        valueTextArea.setHeight(100, Unit.PERCENTAGE);
        valueTextArea.addTextChangeListener(this::onValueChange);
        valueTextArea.setTextChangeEventMode(TextChangeEventMode.EAGER);
        return valueTextArea;
    }

    protected Grid createMetadataGrid() {
        final Grid metadataGrid = new Grid();
        metadataGrid.addStyleName(SPUIStyleDefinitions.METADATA_GRID);
        metadataGrid.setImmediate(true);
        metadataGrid.setHeight("100%");
        metadataGrid.setWidth("100%");
        metadataGrid.setId(UIComponentIdProvider.METDATA_TABLE_ID);
        metadataGrid.setSelectionMode(SelectionMode.SINGLE);
        metadataGrid.setColumnReorderingAllowed(true);
        metadataGrid.setContainerDataSource(getMetadataContainer());
        metadataGrid.getColumn(KEY).setHeaderCaption(i18n.getMessage("header.key"));
        metadataGrid.getColumn(VALUE).setHeaderCaption(i18n.getMessage("header.value"));
        metadataGrid.getColumn(VALUE).setHidden(true);
        metadataGrid.addSelectionListener(this::onRowClick);
        metadataGrid.getColumn(DELETE_BUTTON).setHeaderCaption("");
        metadataGrid.getColumn(DELETE_BUTTON).setRenderer(new HtmlButtonRenderer(this::onDelete));
        metadataGrid.getColumn(DELETE_BUTTON).setWidth(50);
        metadataGrid.getColumn(KEY).setExpandRatio(1);
        return metadataGrid;
    }

    private void onDelete(final RendererClickEvent event) {
        final Item item = metaDataGrid.getContainerDataSource().getItem(event.getItemId());
        final String key = (String) item.getItemProperty(KEY).getValue();

        final ConfirmationDialog confirmDialog = new ConfirmationDialog(
                i18n.getMessage("caption.metadata.delete.action.confirmbox"),
                i18n.getMessage("message.confirm.delete.metadata", key), i18n.getMessage("button.ok"),
                i18n.getMessage("button.cancel"), ok -> {
                    if (ok) {
                        handleOkDeleteMetadata(event, key);
                    }
                });
        UI.getCurrent().addWindow(confirmDialog.getWindow());
        confirmDialog.getWindow().bringToFront();
    }

    private void handleOkDeleteMetadata(final RendererClickEvent event, final String key) {
        deleteMetadata(getSelectedEntity(), key);
        uiNotification.displaySuccess(i18n.getMessage("message.metadata.deleted.successfully", key));
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
            resetFields();
        }
    }

    private void resetFields() {
        clearFields();
        metaDataGrid.select(null);
        if (hasCreatePermission()) {
            enableEditing();
            addIcon.setEnabled(false);
        }
    }

    private Button createAddIcon() {
        addIcon = SPUIComponentProvider.getButton(UIComponentIdProvider.METADTA_ADD_ICON_ID,
                i18n.getMessage("button.save"), null, null, false, FontAwesome.PLUS,
                SPUIButtonStyleSmallNoBorder.class);
        addIcon.addClickListener(event -> onAdd());
        return addIcon;
    }

    private Label createHeaderCaption() {
        return new LabelBuilder().name(i18n.getMessage("caption.metadata")).buildCaptionLabel();
    }

    private static IndexedContainer getMetadataContainer() {
        final IndexedContainer swcontactContainer = new IndexedContainer();
        swcontactContainer.addContainerProperty(KEY, String.class, "");
        swcontactContainer.addContainerProperty(VALUE, String.class, "");
        swcontactContainer.addContainerProperty(DELETE_BUTTON, String.class, FontAwesome.TRASH_O.getHtml());
        return swcontactContainer;
    }

    protected Item popualateKeyValue(final Object metadataCompositeKey) {
        if (metadataCompositeKey != null) {
            final Item item = metaDataGrid.getContainerDataSource().getItem(metadataCompositeKey);
            keyTextField.setValue((String) item.getItemProperty(KEY).getValue());
            valueTextArea.setValue((String) item.getItemProperty(VALUE).getValue());
            keyTextField.setEnabled(false);
            if (hasUpdatePermission()) {
                valueTextArea.setEnabled(true);
            }
            return item;
        }

        return null;
    }

    private void populateGrid() {
        final List<M> metadataList = getMetadataList();
        for (final M metaData : metadataList) {
            addItemToGrid(metaData);
        }
    }

    protected Item addItemToGrid(final M metaData) {
        final IndexedContainer metadataContainer = (IndexedContainer) metaDataGrid.getContainerDataSource();
        final Item item = metadataContainer.addItem(metaData.getKey());
        item.getItemProperty(VALUE).setValue(metaData.getValue());
        item.getItemProperty(KEY).setValue(metaData.getKey());
        return item;
    }

    protected Item updateItemInGrid(final String key) {
        final IndexedContainer metadataContainer = (IndexedContainer) metaDataGrid.getContainerDataSource();
        final Item item = metadataContainer.getItem(key);
        item.getItemProperty(VALUE).setValue(valueTextArea.getValue());
        return item;
    }

    private void onAdd() {
        metaDataGrid.deselect(metaDataGrid.getSelectedRow());
        clearFields();
        enableEditing();
        addIcon.setEnabled(true);
    }

    protected void clearFields() {
        valueTextArea.clear();
        keyTextField.clear();
    }

    protected void onSave() {
        final String key = keyTextField.getValue();
        final String value = valueTextArea.getValue();
        if (mandatoryCheck()) {
            final E entity = selectedEntity;
            if (metaDataGrid.getSelectedRow() == null) {
                if (!duplicateCheck(entity)) {
                    final M metadata = createMetadata(entity, key, value);
                    uiNotification.displaySuccess(i18n.getMessage("message.metadata.saved", metadata.getKey()));
                    addItemToGrid(metadata);
                    metaDataGrid.scrollToEnd();
                    metaDataGrid.select(metadata.getKey());
                    addIcon.setEnabled(true);
                    metadataWindow.setSaveButtonEnabled(false);
                    if (!hasUpdatePermission()) {
                        valueTextArea.setEnabled(false);
                    }
                }
            } else {
                final M metadata = updateMetadata(entity, key, value);
                uiNotification.displaySuccess(i18n.getMessage("message.metadata.updated", metadata.getKey()));
                updateItemInGrid(metadata.getKey());
                metaDataGrid.select(metadata.getKey());
                addIcon.setEnabled(true);
                metadataWindow.setSaveButtonEnabled(false);
            }
        }
    }

    private boolean mandatoryCheck() {
        if (keyTextField.getValue().isEmpty()) {
            uiNotification.displayValidationError(i18n.getMessage("message.key.missing"));
            return false;
        }
        if (valueTextArea.getValue().isEmpty()) {
            uiNotification.displayValidationError(i18n.getMessage("message.value.missing"));
            return false;
        }
        return true;
    }

    private boolean duplicateCheck(final E entity) {
        if (!checkForDuplicate(entity, keyTextField.getValue())) {
            return false;
        }

        uiNotification
                .displayValidationError(i18n.getMessage("message.metadata.duplicate.check", keyTextField.getValue()));
        return true;
    }

    private String getMetadataCaption(final String nameVersionStr) {
        final StringBuilder caption = new StringBuilder();
        caption.append(HawkbitCommonUtil.DIV_DESCRIPTION_START + i18n.getMessage("caption.metadata.popup") + " "
                + HawkbitCommonUtil.getBoldHTMLText(nameVersionStr));
        caption.append(HawkbitCommonUtil.DIV_DESCRIPTION_END);
        return caption.toString();
    }

    private void onCancel() {
        metadataWindow.close();
        UI.getCurrent().removeWindow(metadataWindow);
    }

    private void onKeyChange(final TextChangeEvent event) {
        if (hasCreatePermission() || hasUpdatePermission()) {
            if (!valueTextArea.getValue().isEmpty() && !event.getText().isEmpty()) {
                metadataWindow.setSaveButtonEnabled(true);
            } else {
                metadataWindow.setSaveButtonEnabled(false);
            }
        }
    }

    protected void onRowClick(final SelectionEvent event) {
        final Set<Object> itemsSelected = event.getSelected();
        if (!itemsSelected.isEmpty()) {
            popualateKeyValue(itemsSelected.iterator().next());
            addIcon.setEnabled(true);
        } else {
            clearFields();
            if (hasCreatePermission()) {
                enableEditing();
                addIcon.setEnabled(false);
            } else {
                keyTextField.setEnabled(false);
                valueTextArea.setEnabled(false);
            }
        }
        metadataWindow.setSaveButtonEnabled(false);
    }

    protected void enableEditing() {
        keyTextField.setEnabled(true);
        valueTextArea.setEnabled(true);
    }

    private void onValueChange(final TextChangeEvent event) {
        if (hasCreatePermission() || hasUpdatePermission()) {
            if (!keyTextField.getValue().isEmpty() && !event.getText().isEmpty()) {
                metadataWindow.setSaveButtonEnabled(true);
            } else {
                metadataWindow.setSaveButtonEnabled(false);
            }
        }
    }

    private void setUpDetails(final Long swId, final String metaDatakey) {
        resetDetails();
        metadataWindow.clearOriginalValues();
        if (swId != null) {
            metaDataGrid.getContainerDataSource().removeAllItems();
            populateGrid();
            metaDataGrid.getSelectionModel().reset();
            if (!metaDataGrid.getContainerDataSource().getItemIds().isEmpty()) {
                if (metaDatakey == null) {
                    metaDataGrid.select(metaDataGrid.getContainerDataSource().getIdByIndex(0));
                } else {
                    metaDataGrid.select(metaDatakey);
                }
            } else if (hasCreatePermission()) {
                enableEditing();
                addIcon.setEnabled(false);
            }
        }
    }

    private void resetDetails() {
        clearFields();
        disableEditing();
        metadataWindow.setSaveButtonEnabled(false);
        addIcon.setEnabled(true);
    }

    protected void disableEditing() {
        keyTextField.setEnabled(false);
        valueTextArea.setEnabled(false);
    }

    protected TextArea getValueTextArea() {
        return valueTextArea;
    }

    protected TextField getKeyTextField() {
        return keyTextField;
    }

    protected CommonDialogWindow getMetadataWindow() {
        return metadataWindow;
    }

}
