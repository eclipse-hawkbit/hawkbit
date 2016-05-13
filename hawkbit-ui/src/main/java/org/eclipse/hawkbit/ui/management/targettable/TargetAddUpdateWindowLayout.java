/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.hawkbit.repository.jpa.TargetManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.management.event.DragEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Add and Update Target.
 *
 *
 *
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

    private TextField controllerIDTextField;
    private TextField nameTextField;
    private TextArea descTextArea;
    private Label madatoryLabel;
    private Button saveTarget;
    private Button discardTarget;
    private boolean editTarget = Boolean.FALSE;
    private String controllerId;
    private VerticalLayout mainLayout;
    private Window addTargetWindow;

    private String oldTargetName;
    private String oldTargetDesc;

    /**
     * Initialize the Add Update Window Component for Target.
     */
    public void init() {
        /* create components */
        createRequiredComponents();
        /* display components in layout */
        buildLayout();
        /* register all listeners related to the Window */
        addListeners();
        setCompositionRoot(mainLayout);

    }

    private void createRequiredComponents() {
        /* Textfield for controller Id */
        controllerIDTextField = SPUIComponentProvider.getTextField("", ValoTheme.TEXTFIELD_TINY, true, null,
                i18n.get("prompt.target.id"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        controllerIDTextField.setId(SPUIComponetIdProvider.TARGET_ADD_CONTROLLER_ID);

        /* Textfield for target name */
        nameTextField = SPUIComponentProvider.getTextField("", ValoTheme.TEXTFIELD_TINY, false, null,
                i18n.get("textfield.name"), true, SPUILabelDefinitions.TEXT_FIELD_MAX_LENGTH);
        nameTextField.setId(SPUIComponetIdProvider.TARGET_ADD_NAME);

        /* Textarea for target description */
        descTextArea = SPUIComponentProvider.getTextArea("text-area-style", ValoTheme.TEXTFIELD_TINY, false, null,
                i18n.get("textfield.description"), SPUILabelDefinitions.TEXT_AREA_MAX_LENGTH);
        descTextArea.setId(SPUIComponetIdProvider.TARGET_ADD_DESC);
        descTextArea.setNullRepresentation(HawkbitCommonUtil.SP_STRING_EMPTY);

        /* Label for mandatory symbol */
        madatoryLabel = new Label(i18n.get("label.mandatory.field"));
        madatoryLabel.setStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR + " " + ValoTheme.LABEL_SMALL);

        /* save or update button */
        saveTarget = SPUIComponentProvider.getButton(SPUIDefinitions.NEW_TARGET_SAVE, "", "", "", true,
                FontAwesome.SAVE, SPUIButtonStyleSmallNoBorder.class);
        saveTarget.addClickListener(event -> saveTargetListner());

        /* close button */
        discardTarget = SPUIComponentProvider.getButton(SPUIDefinitions.NEW_TARGET_DISCARD, "", "", "", true,
                FontAwesome.TIMES, SPUIButtonStyleSmallNoBorder.class);
        discardTarget.addClickListener(event -> discardTargetListner());
    }

    private void buildLayout() {
        /* action button layout (save & dicard) */
        final HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSizeFull();
        buttonsLayout.addComponents(saveTarget, discardTarget);
        buttonsLayout.setComponentAlignment(saveTarget, Alignment.BOTTOM_LEFT);
        buttonsLayout.setComponentAlignment(discardTarget, Alignment.BOTTOM_RIGHT);
        buttonsLayout.addStyleName("window-style");
        /*
         * The main layout of the window contains mandatory info, textboxes
         * (controller Id, name & description) and action buttons layout
         */
        mainLayout = new VerticalLayout();
        mainLayout.setSpacing(Boolean.TRUE);
        mainLayout.addStyleName("lay-color");
        mainLayout.setSizeUndefined();
        mainLayout.addComponent(madatoryLabel);
        mainLayout.setComponentAlignment(madatoryLabel, Alignment.MIDDLE_LEFT);
        if (Boolean.TRUE.equals(editTarget)) {
            madatoryLabel.setVisible(Boolean.FALSE);
        }
        mainLayout.addComponents(madatoryLabel, controllerIDTextField, nameTextField, descTextArea, buttonsLayout);
        nameTextField.focus();
    }

    private void addListeners() {

        addTargetNameChangeListner();
        addTargetDescChangeListner();

    }

    private void addTargetNameChangeListner() {
        nameTextField.addTextChangeListener(new TextChangeListener() {

            /**
             *
             */
            private static final long serialVersionUID = 1761855781481115921L;

            @Override
            public void textChange(final TextChangeEvent event) {
                if (event.getText().equals(oldTargetName) && descTextArea.getValue().equals(oldTargetDesc)) {
                    saveTarget.setEnabled(false);
                } else {
                    saveTarget.setEnabled(true);
                }

            }
        });

    }

    private void addTargetDescChangeListner() {
        descTextArea.addTextChangeListener(new TextChangeListener() {

            /**
             *
             */
            private static final long serialVersionUID = 5770734934988115068L;

            @Override
            public void textChange(final TextChangeEvent event) {
                if (event.getText().equals(oldTargetDesc) && nameTextField.getValue().equals(oldTargetName)) {
                    saveTarget.setEnabled(false);
                } else {
                    saveTarget.setEnabled(true);
                }

            }
        });
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
            Target newTarget = new Target(newControlllerId);
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
        addTargetWindow = SPUIComponentProvider.getWindow(i18n.get("caption.add.new.target"), null,
                SPUIDefinitions.CREATE_UPDATE_WINDOW);
        addTargetWindow.setContent(this);
        return addTargetWindow;
    }

    /**
     * clear all fields of Target Edit Window.
     */
    public void resetComponents() {
        nameTextField.clear();
        nameTextField.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        controllerIDTextField.setEnabled(true);
        controllerIDTextField.setReadOnly(false);
        controllerIDTextField.removeStyleName(SPUIStyleDefinitions.SP_TEXTFIELD_ERROR);
        controllerIDTextField.clear();
        descTextArea.clear();
        editTarget = Boolean.FALSE;
    }

    private void closeThisWindow() {
        editTarget = Boolean.FALSE;
        addTargetWindow.close();
        UI.getCurrent().removeWindow(addTargetWindow);
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
            uINotification.displayValidationError(i18n.get("message.target.duplicate.check"));
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
        controllerIDTextField.setReadOnly(Boolean.TRUE);
        nameTextField.setValue(target.getName());
        if (target.getDescription() != null) {
            descTextArea.setValue(target.getDescription());
        }
        saveTarget.setEnabled(Boolean.FALSE);

        oldTargetDesc = descTextArea.getValue();
        oldTargetName = nameTextField.getValue();
        addTargetWindow.addStyleName("target-update-window");
    }

    public VerticalLayout getMainLayout() {
        return mainLayout;
    }

    public void setMainLayout(final VerticalLayout mainLayout) {
        this.mainLayout = mainLayout;
    }

}
