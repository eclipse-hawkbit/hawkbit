/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Add and Update Target.
 */
@SpringComponent
@VaadinSessionScope
public class TargetAddUpdateWindowLayout extends CustomComponent {
    private static final long serialVersionUID = -6659290471705262389L;

    @Autowired
    private I18N i18n;

    @Autowired
    private transient TargetManagement targetManagement;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient UINotification uINotification;

    @Autowired
    private transient EntityFactory entityFactory;

    private TextField controllerIDTextField;
    private TextField nameTextField;
    private TextArea descTextArea;
    private boolean editTarget = Boolean.FALSE;
    private String controllerId;
    private FormLayout formLayout;
    private CommonDialogWindow window;

    private String originalTargetName;
    private String originalTargetDesc;
    private String originalControllerId;

    /**
     * Initialize the Add Update Window Component for Target.
     */
    public void init() {
        createRequiredComponents();
        buildLayout();
        setCompositionRoot(formLayout);
    }

    private void createRequiredComponents() {
        /* Textfield for controller Id */
        controllerIDTextField = SPUIComponentProvider.getTextField(i18n.get("prompt.target.id"), "",
                ValoTheme.TEXTFIELD_TINY, true, null, i18n.get("prompt.target.id"), true,
                SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        controllerIDTextField.setId(SPUIComponentIdProvider.TARGET_ADD_CONTROLLER_ID);
        controllerIDTextField
                .addTextChangeListener(event -> window.checkMandatoryEditedTextField(event, originalControllerId));
        controllerIDTextField
                .addValueChangeListener(event -> window.setRequiredFieldWhenUpdate(event, controllerIDTextField));

        /* Textfield for target name */
        nameTextField = SPUIComponentProvider.getTextField(i18n.get("textfield.name"), "", ValoTheme.TEXTFIELD_TINY,
                false, null, i18n.get("textfield.name"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        nameTextField.setId(SPUIComponentIdProvider.TARGET_ADD_NAME);
        nameTextField.addTextChangeListener(event -> window.checkMandatoryEditedTextField(event, originalTargetName));

        /* Textarea for target description */
        descTextArea = SPUIComponentProvider.getTextArea(i18n.get("textfield.description"), "text-area-style",
                ValoTheme.TEXTFIELD_TINY, false, null, i18n.get("textfield.description"),
                SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);
        descTextArea.setId(SPUIComponentIdProvider.TARGET_ADD_DESC);
        descTextArea.setNullRepresentation(HawkbitCommonUtil.SP_STRING_EMPTY);
        descTextArea.addTextChangeListener(event -> window.checkMandatoryEditedTextField(event, originalTargetDesc));
    }

    private void buildLayout() {

        /*
         * The main layout of the window contains mandatory info, textboxes
         * (controller Id, name & description) and action buttons layout
         */
        setSizeUndefined();
        formLayout = new FormLayout();
        formLayout.addComponent(controllerIDTextField);
        formLayout.addComponent(nameTextField);
        formLayout.addComponent(descTextArea);

        controllerIDTextField.focus();
    }

    /**
     * Update the Target if modified.
     */
    public void updateTarget() {

        final String newName = HawkbitCommonUtil.trimAndNullIfEmpty(nameTextField.getValue());
        final String newDesc = HawkbitCommonUtil.trimAndNullIfEmpty(descTextArea.getValue());
        /* get latest entity */
        final Target latestTarget = targetManagement.findTargetByControllerIDWithDetails(controllerId);
        /* update new name & desc */
        setTargetValues(latestTarget, newName, newDesc);
        /* save updated entity */
        targetManagement.updateTarget(latestTarget);
        /* display success msg */
        uINotification.displaySuccess(i18n.get("message.update.success", new Object[] { latestTarget.getName() }));
        // publishing through event bus
        eventBus.publish(this, new TargetTableEvent(BaseEntityEventType.UPDATED_ENTITY, latestTarget));

        /* close the window */
        closeThisWindow();
        /* update details in table */
    }

    private void saveTargetListner() {
        if (editTarget) {
            updateTarget();
        } else {
            addNewTarget();
        }
    }

    private void discardTargetListner() {
        closeThisWindow();
    }

    private void addNewTarget() {
        final String newControlllerId = HawkbitCommonUtil.trimAndNullIfEmpty(controllerIDTextField.getValue());
        if (mandatoryCheck(newControlllerId) && duplicateCheck(newControlllerId)) {
            final String newName = HawkbitCommonUtil.trimAndNullIfEmpty(nameTextField.getValue());
            final String newDesc = HawkbitCommonUtil.trimAndNullIfEmpty(descTextArea.getValue());

            /* create new target entity */
            Target newTarget = entityFactory.generateTarget(newControlllerId);
            /* set values to the new target entity */
            setTargetValues(newTarget, newName, newDesc);
            /* save new target */
            newTarget = targetManagement.createTarget(newTarget);
            final TargetTable targetTable = SpringContextHelper.getBean(TargetTable.class);
            final Set<TargetIdName> s = new HashSet<>();
            s.add(newTarget.getTargetIdName());
            targetTable.setValue(s);

            /* display success msg */
            uINotification.displaySuccess(i18n.get("message.save.success", new Object[] { newTarget.getName() }));
            /* close the window */
            closeThisWindow();
        }
    }

    public Window getWindow() {
        eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
        window = SPUIComponentProvider.getWindow(i18n.get("caption.add.new.target"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW, this, event -> saveTargetListner(),
                event -> discardTargetListner(), null, getMandatoryFields(), getEditedFields(), i18n);
        return window;
    }

    private Map<String, Boolean> getMandatoryFields() {
        final Map<String, Boolean> requiredFields = new HashMap<>();
        final Iterator<Component> iterate = formLayout.iterator();
        while (iterate.hasNext()) {
            final Component c = iterate.next();
            if (c instanceof AbstractField && ((AbstractField) c).isRequired()) {
                requiredFields.put(c.getId(), Boolean.FALSE);
            }
        }
        return requiredFields;
    }

    private Map<String, Boolean> getEditedFields() {
        final Map<String, Boolean> editedFields = new HashMap<>();
        editedFields.put(controllerIDTextField.getId(), Boolean.FALSE);
        editedFields.put(nameTextField.getId(), Boolean.FALSE);
        editedFields.put(descTextArea.getId(), Boolean.FALSE);
        return editedFields;
    }

    /**
     * clear all fields of Target Edit Window.
     */
    public void resetComponents() {
        nameTextField.clear();
        nameTextField.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        controllerIDTextField.setEnabled(Boolean.TRUE);
        controllerIDTextField.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        controllerIDTextField.clear();
        descTextArea.clear();
        editTarget = Boolean.FALSE;

        originalControllerId = null;
        originalTargetDesc = null;
        originalTargetName = null;

        if (window != null) {
            window.reset();
        }
    }

    private void closeThisWindow() {
        editTarget = Boolean.FALSE;
        window.close();
        UI.getCurrent().removeWindow(window);
    }

    private void setTargetValues(final Target target, final String name, final String description) {
        target.setName(name == null ? target.getControllerId() : name);
        target.setDescription(description);
    }

    private boolean mandatoryCheck(final String newControlllerId) {
        if (newControlllerId == null) {
            controllerIDTextField.addStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
            uINotification.displayValidationError("Mandatory details are missing");
            return false;
        } else {
            return true;
        }
    }

    private boolean duplicateCheck(final String newControlllerId) {
        final Target existingTarget = targetManagement.findTargetByControllerID(newControlllerId.trim());
        if (existingTarget != null) {
            uINotification.displayValidationError(
                    i18n.get("message.target.duplicate.check", new Object[] { newControlllerId }));
            return false;
        } else {
            return true;
        }
    }

    /**
     * @param controllerId
     */
    public void populateValuesOfTarget(final String controllerId) {
        resetComponents();
        this.controllerId = controllerId;
        editTarget = Boolean.TRUE;
        final Target target = targetManagement.findTargetByControllerID(controllerId);
        controllerIDTextField.setValue(target.getControllerId());
        controllerIDTextField.setEnabled(Boolean.FALSE);
        nameTextField.setValue(target.getName());
        if (target.getDescription() != null) {
            descTextArea.setValue(target.getDescription());
        }

        originalTargetDesc = descTextArea.getValue();
        originalTargetName = nameTextField.getValue();
        originalControllerId = controllerIDTextField.getValue();
        window.addStyleName("target-update-window");
    }

    public FormLayout getFormLayout() {
        return formLayout;
    }

}
