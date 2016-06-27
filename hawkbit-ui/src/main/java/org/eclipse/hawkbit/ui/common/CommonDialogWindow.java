/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.ui.artifacts.smtable.SoftwareModuleAddUpdateWindow;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleBorderWithIcon;
import org.eclipse.hawkbit.ui.layouts.AbstractCreateUpdateTagLayout;
import org.eclipse.hawkbit.ui.management.targettable.TargetAddUpdateWindowLayout;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Superclass for pop-up-windows including a minimize and close icon in the
 * upper right corner and a save and cancel button at the bottom.
 */
public class CommonDialogWindow extends Window implements Serializable {

    private static final long serialVersionUID = -1321949234316858703L;

    private final VerticalLayout mainLayout = new VerticalLayout();

    private final String caption;

    private final Component content;

    private final String helpLink;

    private Button saveButton;

    private Button cancelButton;

    private HorizontalLayout buttonsLayout;

    protected ValueChangeListener buttonEnableListener;

    private final ClickListener saveButtonClickListener;

    private final ClickListener cancelButtonClickListener;

    private Map<String, Boolean> requiredFields;

    private Map<String, Boolean> editedFields;

    private final I18N i18n;

    /**
     * Constructor.
     * 
     * @param caption
     *            the caption
     * @param content
     *            the content
     * @param helpLink
     *            the helpLinks
     * @param saveButtonClickListener
     *            the saveButtonClickListener
     * @param cancelButtonClickListener
     *            the cancelButtonClickListener
     */
    public CommonDialogWindow(final String caption, final Component content, final String helpLink,
            final ClickListener saveButtonClickListener, final ClickListener cancelButtonClickListener,
            final Map<String, Boolean> requiredFields, final Map<String, Boolean> editedFields, final I18N i18n) {
        checkNotNull(saveButtonClickListener);
        checkNotNull(cancelButtonClickListener);
        this.caption = caption;
        this.content = content;
        this.helpLink = helpLink;
        this.saveButtonClickListener = saveButtonClickListener;
        this.cancelButtonClickListener = cancelButtonClickListener;
        this.requiredFields = requiredFields;
        if (requiredFields == null) {
            this.requiredFields = Collections.emptyMap();
        }

        this.editedFields = editedFields;
        if (editedFields == null) {
            this.editedFields = Collections.emptyMap();
        }
        this.i18n = i18n;
        init();
    }

    /**
     * Checks if all mandatory fields are filled, and if there are changes in
     * the current field. If yes, the save button will be enabled.
     * 
     * @param event
     *            TextChangeEvent
     * @param originalValue
     *            original Value of the current field
     */
    public void checkMandatoryEditedTextField(final TextChangeEvent event, final String originalValue) {
        final Component component = event.getComponent();
        if (!(component instanceof AbstractComponent)) {
            throw new IllegalStateException("Only AbstractComponent are allowed");
        }

        if (requiredFields.containsKey(component.getId())) {
            final boolean isTextChangeNotEmpty = StringUtils.isNotBlank(event.getText());
            setRequiredFieldChangeValue((AbstractComponent) component, isTextChangeNotEmpty);
        }
        checkChanges(component.getId(), event.getText(), originalValue);
        checkSaveButtonEnabled();
    }

    /**
     * Checks if all mandatory fields are filled, and if there are changes in
     * the current field. If yes, the save button will be enabled.
     * 
     * @param event
     *            ValueChangeEvent
     * @param component
     *            current Component
     * @param originalValue
     *            original Value of the current field
     */
    public void checkMandatoryEditedValue(final ValueChangeEvent event, final AbstractComponent component,
            final String originalValue) {
        final boolean isChangedValueNotNull = event.getProperty().getValue() != null;
        if (requiredFields.containsKey(component.getId())) {
            setRequiredFieldChangeValue(component, isChangedValueNotNull);
        }
        if (event.getProperty().getValue() != null) {
            checkChanges(component.getId(), event.getProperty().getValue().toString(), originalValue);
        } else {
            checkChanges(component.getId(), null, originalValue);
        }
        checkSaveButtonEnabled();
    }

    /**
     * Checks if all mandatory fields are filled, and if there are changes in
     * the current field. (Boolean) If yes, the save button will be enabled.
     * 
     * @param event
     *            ValueChangeEvent
     * @param component
     *            current Component
     * @param originalValue
     *            original Boolean Value of the current field
     */
    public void checkMandatoryEditedValueBoolean(final ValueChangeEvent event, final AbstractComponent component,
            final Boolean originalValue) {
        final boolean isChangedValueNotNull = event.getProperty().getValue() != null;
        if (requiredFields.containsKey(component.getId())) {
            setRequiredFieldChangeValue(component, isChangedValueNotNull);
        }
        final Boolean changed = (Boolean) event.getProperty().getValue();
        editedFields.put(component.getId(), BooleanUtils.compare(changed, originalValue) != 0);
        checkSaveButtonEnabled();
    }

    /**
     * Updates the map of required fields if a value is set. (e.g. on Update
     * when editing a component)
     * 
     * @param event
     *            ValueChangeEvent
     * @param component
     *            current Component
     */
    public void setRequiredFieldWhenUpdate(final ValueChangeEvent event, final AbstractComponent component) {
        final boolean isChangedValueNotNull = event.getProperty().getValue() != null;
        setRequiredFieldChangeValue(component, isChangedValueNotNull);
    }

    /**
     * * Updates the map of required fields if a value is set. (e.g. on Update
     * when editing a component)
     * 
     * @param fieldId
     *            Id of the current component
     * @param filled
     *            Boolean if field is filled
     */
    public void updateRequiredFields(final String fieldId, final Boolean filled) {
        requiredFields.put(fieldId, filled);
        checkSaveButtonEnabled();
    }

    /**
     * Checks if Color is changed
     * 
     * @param fieldId
     *            Id of the current component
     * @param newColor
     *            new Color
     * @param oldColor
     *            old Color
     */
    public void checkColorChange(final String fieldId, final Color newColor, final Color oldColor) {
        editedFields.put(fieldId, !newColor.equals(oldColor));
        checkSaveButtonEnabled();
    }

    /**
     * Updates the map of fields which can be edited.
     * 
     * @param fieldId
     *            Id of the current component
     * @param hasTextValueChanged
     *            Boolean if value has changed
     */
    public void updateEditedFields(final String fieldId, final Boolean hasTextValueChanged) {
        editedFields.put(fieldId, hasTextValueChanged);
        checkSaveButtonEnabled();
    }

    /**
     * Resets the map of mandatory and edited Fields and disable the save button
     */
    public void reset() {
        saveButton.setEnabled(false);
        resetFields();
    }

    private void setRequiredFieldChangeValue(final AbstractComponent component, final boolean isTextChangeNotEmpty) {
        requiredFields.put(component.getId(), isTextChangeNotEmpty);
    }

    /**
     * Checks the mandatory fields in the pop-up-window content. If all
     * mandatory fields are filled the save button is enabled. Otherwise the
     * save button is disabled.
     */
    private void checkSaveButtonEnabled() {
        saveButton.setEnabled(!requiredFields.containsValue(Boolean.FALSE) && editedFields.containsValue(Boolean.TRUE));
    }

    private void checkChanges(final String fieldName, final String newText, final String oldText) {
        final boolean hasTextValueChanged = (StringUtils.isNotBlank(newText) && !newText.equals(oldText))
                || (StringUtils.isNotBlank(oldText) && !oldText.equals(newText));
        editedFields.put(fieldName, hasTextValueChanged);
    }

    private void resetFields() {
        for (final Map.Entry<String, Boolean> entry : requiredFields.entrySet()) {
            entry.setValue(Boolean.FALSE);
        }

        for (final Map.Entry<String, Boolean> entry : editedFields.entrySet()) {
            entry.setValue(Boolean.FALSE);
        }
    }

    private final void init() {

        if (content instanceof AbstractOrderedLayout) {
            ((AbstractOrderedLayout) content).setSpacing(true);
            ((AbstractOrderedLayout) content).setMargin(true);
        }
        if (content instanceof GridLayout) {
            addStyleName("marginTop");
        }

        if (null != content) {
            mainLayout.addComponent(content);
        }

        createMandatoryLabel();

        final HorizontalLayout buttonLayout = createActionButtonsLayout();
        mainLayout.addComponent(buttonLayout);
        mainLayout.setComponentAlignment(buttonLayout, Alignment.TOP_CENTER);

        setCaption(caption);
        setContent(mainLayout);
        setResizable(false);
        center();
        setModal(true);
        addStyleName("fontsize");
    }

    private HorizontalLayout createActionButtonsLayout() {

        buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSizeFull();
        buttonsLayout.setSpacing(true);

        createSaveButton();

        createCancelButton();
        buttonsLayout.addStyleName("actionButtonsMargin");

        addHelpLink();

        return buttonsLayout;
    }

    private void createMandatoryLabel() {

        if (!existsMandatoryFieldsInWindowContent()) {
            return;
        }

        final Label mandatoryLabel = new Label(i18n.get("label.mandatory.field"));
        mandatoryLabel.addStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR + " " + ValoTheme.LABEL_TINY);

        if (content instanceof TargetAddUpdateWindowLayout) {
            ((TargetAddUpdateWindowLayout) content).getFormLayout().addComponent(mandatoryLabel);
        } else if (content instanceof SoftwareModuleAddUpdateWindow) {
            ((SoftwareModuleAddUpdateWindow) content).getFormLayout().addComponent(mandatoryLabel);
        } else if (content instanceof AbstractCreateUpdateTagLayout) {
            ((AbstractCreateUpdateTagLayout) content).getMainLayout().addComponent(mandatoryLabel);
        }

        mainLayout.addComponent(mandatoryLabel);
    }

    private void createCancelButton() {
        cancelButton = SPUIComponentProvider.getButton(SPUIComponentIdProvider.CANCEL_BUTTON, "Cancel", "", "", true,
                FontAwesome.TIMES, SPUIButtonStyleBorderWithIcon.class);
        cancelButton.setSizeUndefined();
        cancelButton.addStyleName("default-color");
        cancelButton.addClickListener(cancelButtonClickListener);

        buttonsLayout.addComponent(cancelButton);
        buttonsLayout.setComponentAlignment(cancelButton, Alignment.MIDDLE_LEFT);
        buttonsLayout.setExpandRatio(cancelButton, 1.0F);
    }

    private void createSaveButton() {
        saveButton = SPUIComponentProvider.getButton(SPUIComponentIdProvider.SAVE_BUTTON, "Save", "", "", true,
                FontAwesome.SAVE, SPUIButtonStyleBorderWithIcon.class);
        saveButton.setSizeUndefined();
        saveButton.addStyleName("default-color");
        saveButton.addClickListener(saveButtonClickListener);
        saveButton.setEnabled(false);
        buttonsLayout.addComponent(saveButton);
        buttonsLayout.setComponentAlignment(saveButton, Alignment.MIDDLE_RIGHT);
        buttonsLayout.setExpandRatio(saveButton, 1.0F);
    }

    private boolean existsMandatoryFieldsInWindowContent() {
        return !requiredFields.isEmpty();
    }

    private void addHelpLink() {

        if (StringUtils.isEmpty(helpLink)) {
            return;
        }
        final Link helpLinkComponent = SPUIComponentProvider.getHelpLink(helpLink);
        buttonsLayout.addComponent(helpLinkComponent);
        buttonsLayout.setComponentAlignment(helpLinkComponent, Alignment.MIDDLE_RIGHT);
    }

    public void setSaveButtonEnabled(final boolean enabled) {
        saveButton.setEnabled(enabled);
    }

    public void setCancelButtonEnabled(final boolean enabled) {
        cancelButton.setEnabled(enabled);
    }

    public HorizontalLayout getButtonsLayout() {
        return buttonsLayout;
    }

    public Map<String, Boolean> getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(final Map<String, Boolean> requiredFields) {
        this.requiredFields = requiredFields;
    }

}
