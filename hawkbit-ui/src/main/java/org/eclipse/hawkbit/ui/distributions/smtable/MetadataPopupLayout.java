package org.eclipse.hawkbit.ui.distributions.smtable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SwMetadataCompositeKey;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.SelectionEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SpringComponent
@ViewScope
public class MetadataPopupLayout extends HorizontalLayout {

    private static final String VALUE = "value";

    private static final String KEY = "key";

    private static final long serialVersionUID = 2266705497029143361L;

    @Autowired
    private I18N i18n;

    @Autowired
    private SoftwareManagement softwareManagement;

    @Autowired
    private UINotification uiNotification;

    private TextField keyTextField;

    private TextArea valueTextArea;

    private Button saveButton;

    private Button discardButton;

    private Button addIcon;

    private Grid metaDataGrid;

    private Label headerCaption;

    private transient Long swModuleId;

    @PostConstruct
    private void init() {
        createComponents();
        buildLayout();
    }

    public void setUpDetails(final Long swId) {
        if (swId != null) {
            swModuleId = swId;
            metaDataGrid.getContainerDataSource().removeAllItems();
            populateGrid(swId);
            metaDataGrid.select(metaDataGrid.getContainerDataSource().getIdByIndex(0));
            metaDataGrid.scrollToStart();
        }
    }

    private void buildLayout() {
        final HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.addStyleName(SPUIStyleDefinitions.WIDGET_TITLE);
        headerLayout.setSpacing(false);
        headerLayout.setMargin(false);
        headerLayout.setSizeFull();

        headerLayout.addComponent(headerCaption);
        headerLayout.addComponents(addIcon);
        headerLayout.setComponentAlignment(addIcon, Alignment.MIDDLE_RIGHT);
        headerLayout.setExpandRatio(headerCaption, 1.0F);

        final VerticalLayout tableLayout = new VerticalLayout();
        tableLayout.addComponent(headerLayout);
        tableLayout.addComponent(metaDataGrid);
        tableLayout.setWidth("400px");
        metaDataGrid.setHeight("420px");
        tableLayout.addStyleName("table-layout");

        VerticalLayout metadataFieldsLayout = new VerticalLayout();
        HorizontalLayout iconLayout = new HorizontalLayout();
        iconLayout.addComponent(keyTextField);
        iconLayout.addComponent(saveButton);
        iconLayout.addComponent(discardButton);
        iconLayout.setSizeFull();

        metadataFieldsLayout.addComponent(iconLayout);
        metadataFieldsLayout.addComponent(valueTextArea);
        metadataFieldsLayout.setSpacing(true);
        metadataFieldsLayout.setExpandRatio(valueTextArea, 1.0F);
        addComponent(tableLayout);
        addComponent(metadataFieldsLayout);
        setSpacing(true);
    }

    private Button createSaveButton() {
        saveButton = SPUIComponentProvider.getButton(SPUIComponetIdProvider.METADTA_SAVE_ICON_ID,
                i18n.get("button.save"), null, null, false, FontAwesome.SAVE, SPUIButtonStyleSmallNoBorder.class);
        saveButton.addClickListener(event -> onSave());
        saveButton.setEnabled(false);
        return saveButton;
    }

    private Button createDiscardButton() {
        discardButton = SPUIComponentProvider.getButton(SPUIComponetIdProvider.METADTA_DISCARD_ICON_ID,
                i18n.get("button.save"), null, null, false, FontAwesome.UNDO, SPUIButtonStyleSmallNoBorder.class);
        discardButton.addClickListener(event -> onDiscard());
        discardButton.setEnabled(false);
        return discardButton;
    }

    private void onDiscard() {
        if (metaDataGrid.getSelectedRow() == null) {
            keyTextField.clear();
            valueTextArea.clear();
        } else {
            Object itemSelected = metaDataGrid.getSelectedRow();
            popualateKeyValue((SwMetadataCompositeKey) itemSelected);
        }
    }

    private void onSave() {
        String key = keyTextField.getValue();
        String value = valueTextArea.getValue();
        // TODO remove the extra call
        if (mandatoryCheck()) {
            SoftwareModule softwareModule = softwareManagement.findSoftwareModuleById(swModuleId);
            if (metaDataGrid.getSelectedRow() == null) {
                if (!duplicateCheck(softwareModule)) {
                    SoftwareModuleMetadata metadata = softwareManagement
                            .createSoftwareModuleMetadata(new SoftwareModuleMetadata(key, softwareModule, value));
                    uiNotification.displaySuccess(i18n.get("message.metadata.saved", metadata.getKey()));
                    addItemToGrid(metadata);
                    metaDataGrid.scrollToEnd();
                    metaDataGrid.select(metadata.getId());
                    addIcon.setEnabled(true);
                }
            } else {
                SoftwareModuleMetadata metadata = softwareManagement
                        .updateSoftwareModuleMetadata(new SoftwareModuleMetadata(key, softwareModule, value));
                uiNotification.displaySuccess(i18n.get("message.metadata.updated", metadata.getKey()));
                updateItemInGrid(metadata);
                metaDataGrid.select(metadata.getId());
                addIcon.setEnabled(true);
            }
        }
    }

    private boolean duplicateCheck(SoftwareModule softwareModule) {
        try {
            softwareManagement.findSoftwareModuleMetadata(new SwMetadataCompositeKey(softwareModule, keyTextField
                    .getValue()));
        } catch (final EntityNotFoundException exception) {
            return false;
        }
        uiNotification.displayValidationError(i18n.get("message.metadata.duplicate.check", keyTextField.getValue()));
        return true;
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

    private TextArea createValueTextField() {
        valueTextArea = SPUIComponentProvider.getTextArea(null, "mandate-value-field", ValoTheme.TEXTAREA_TINY, false,
                null, i18n.get("textfield.value"), 4000);
        valueTextArea.setId(SPUIComponetIdProvider.METADATA_VALUE_ID);
        valueTextArea.setNullRepresentation("");
        valueTextArea.setSizeFull();
        valueTextArea.setWidth("100%");
        valueTextArea.addTextChangeListener(event -> onValueChange(event));
        valueTextArea.setTextChangeEventMode(TextChangeEventMode.EAGER);
        return valueTextArea;
    }

    private TextField createKeyTextField() {
        TextField keyField = SPUIComponentProvider.getTextField(null, "", ValoTheme.TEXTFIELD_TINY, true, "",
                i18n.get("textfield.key"), true, 128);
        keyField.setId(SPUIComponetIdProvider.METADATA_KEY_FIELD_ID);
        keyField.setWidth("350px");
        keyField.addTextChangeListener(event -> onKeyChange(event));
        keyField.setTextChangeEventMode(TextChangeEventMode.EAGER);
        return keyField;
    }

    private void onKeyChange(TextChangeEvent event) {
        if (!valueTextArea.getValue().isEmpty() && !event.getText().isEmpty()) {
            saveButton.setEnabled(true);
            discardButton.setEnabled(true);
        } else {
            saveButton.setEnabled(false);
            discardButton.setEnabled(false);
        }

    }

    private void onValueChange(TextChangeEvent event) {
        if (!keyTextField.getValue().isEmpty() && !event.getText().isEmpty()) {
            saveButton.setEnabled(true);
            discardButton.setEnabled(true);
        } else {
            saveButton.setEnabled(false);
            discardButton.setEnabled(false);
        }
    }

    private void createComponents() {
        keyTextField = createKeyTextField();
        valueTextArea = createValueTextField();
        saveButton = createSaveButton();
        discardButton = createDiscardButton();
        metaDataGrid = createMetadataGrid();
        addIcon = createAddIcon();
        headerCaption = createHeaderCaption();
    }

    private Grid createMetadataGrid() {
        final Grid metadataGrid = new Grid();
        metadataGrid.setImmediate(true);
        metadataGrid.setSizeFull();

        metadataGrid.setId(SPUIComponetIdProvider.METDATA_TABLE_ID);
        metadataGrid.setSelectionMode(SelectionMode.SINGLE);
        metadataGrid.setColumnReorderingAllowed(true);
        metadataGrid.setContainerDataSource(getMetadataContainer());
        metadataGrid.getColumn(KEY).setHeaderCaption(i18n.get("header.key"));
        metadataGrid.getColumn(VALUE).setHeaderCaption(i18n.get("header.value"));
        metadataGrid.getColumn(VALUE).setHidden(true);
        metadataGrid.addSelectionListener(event -> onRowClick(event));
        // TODO set expand ratio
        return metadataGrid;
    }

    private void onRowClick(SelectionEvent event) {
        Set<Object> itemsSelected = event.getSelected();
        if (!itemsSelected.isEmpty()) {
            Object itemSelected = itemsSelected.stream().findFirst().isPresent() ? itemsSelected.stream().findFirst()
                    .get() : null;
            popualateKeyValue((SwMetadataCompositeKey) itemSelected);
            addIcon.setEnabled(true);
        } else {
            keyTextField.clear();
            valueTextArea.clear();
            keyTextField.setEnabled(true);
            addIcon.setEnabled(false);
        }
        saveButton.setEnabled(false);
        discardButton.setEnabled(false);
    }

    private void popualateKeyValue(SwMetadataCompositeKey swMetadataCompositeKey) {
        if (swMetadataCompositeKey != null) {
            Item item = metaDataGrid.getContainerDataSource().getItem(swMetadataCompositeKey);
            keyTextField.setValue(swMetadataCompositeKey.getKey());
            // TODO value to be stored in table???
            valueTextArea.setValue((String) item.getItemProperty(VALUE).getValue());
            keyTextField.setEnabled(false);
        }
    }

    private IndexedContainer getMetadataContainer() {
        final IndexedContainer swcontactContainer = new IndexedContainer();
        swcontactContainer.addContainerProperty(KEY, String.class, "");
        swcontactContainer.addContainerProperty(VALUE, String.class, "");
        return swcontactContainer;
    }

    private Button createAddIcon() {
        addIcon = SPUIComponentProvider.getButton(SPUIComponetIdProvider.METADTA_ADD_ICON_ID, i18n.get("button.save"),
                null, null, false, FontAwesome.PLUS, SPUIButtonStyleSmallNoBorder.class);
        addIcon.addClickListener(event -> onAdd(event));
        return addIcon;
    }

    private void onAdd(ClickEvent event) {
        valueTextArea.clear();
        keyTextField.clear();
        addIcon.setEnabled(true);
        metaDataGrid.deselect(metaDataGrid.getSelectedRow());
    }

    private void populateGrid(final Long swId) {
        for (final SoftwareModuleMetadata softwareModuleMetadata : softwareManagement
                .findSoftwareModuleMetadataBySoftwareModuleId(swId)) {
            addItemToGrid(softwareModuleMetadata);
        }

    }

    private void addItemToGrid(final SoftwareModuleMetadata softwareModuleMetadata) {
        final IndexedContainer metadataContainer = (IndexedContainer) metaDataGrid.getContainerDataSource();
        System.out.println("softwareModuleMetadata.getKey()::"+softwareModuleMetadata.getKey());
        System.out.println("softwareModuleMetadata.getId():::"+softwareModuleMetadata.getId());
        final Item item = metadataContainer.addItem(softwareModuleMetadata.getId());
        System.out.println("item::"+item);
        item.getItemProperty(VALUE).setValue(softwareModuleMetadata.getValue());
        System.out.println("softwareModuleMetadata.getValue():::"+softwareModuleMetadata.getValue());
        item.getItemProperty(KEY).setValue(softwareModuleMetadata.getKey());
    }

    private void updateItemInGrid(final SoftwareModuleMetadata softwareModuleMetadata) {
        final IndexedContainer metadataContainer = (IndexedContainer) metaDataGrid.getContainerDataSource();
        final Item item = metadataContainer.getItem(softwareModuleMetadata.getId());
        item.getItemProperty(VALUE).setValue(valueTextArea.getValue());
        item.getItemProperty(KEY).setValue(keyTextField.getValue());
    }

    private Label createHeaderCaption() {
        final Label captionLabel = SPUIComponentProvider.getLabel("Metadata", SPUILabelDefinitions.SP_WIDGET_CAPTION);
        return captionLabel;
    }
}
