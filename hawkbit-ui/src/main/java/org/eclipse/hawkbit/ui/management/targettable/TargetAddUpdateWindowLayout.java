/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.builder.TextAreaBuilder;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.common.builder.WindowBuilder;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.google.common.collect.Sets;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.Window;

/**
 * Add and Update Target.
 */
public class TargetAddUpdateWindowLayout extends CustomComponent {

    private static final long serialVersionUID = -6659290471705262389L;

    private final VaadinMessageSource i18n;

    private final transient TargetManagement targetManagement;

    private final transient EventBus.UIEventBus eventBus;

    private final UINotification uINotification;

    private final transient EntityFactory entityFactory;

    private TextField controllerIDTextField;
    private TextField nameTextField;
    private TextArea descTextArea;
    private boolean editTarget;
    private String controllerId;
    private FormLayout formLayout;
    private CommonDialogWindow window;

    private final TargetTable targetTable;

    TargetAddUpdateWindowLayout(final VaadinMessageSource i18n, final TargetManagement targetManagement,
            final UIEventBus eventBus, final UINotification uINotification, final EntityFactory entityFactory,
            final TargetTable targetTable) {
        this.i18n = i18n;
        this.targetManagement = targetManagement;
        this.eventBus = eventBus;
        this.uINotification = uINotification;
        this.entityFactory = entityFactory;
        this.targetTable = targetTable;
        createRequiredComponents();
        buildLayout();
        setCompositionRoot(formLayout);
    }

    /**
     * Save or update the target.
     */
    private final class SaveOnDialogCloseListener implements SaveDialogCloseListener {
        @Override
        public void saveOrUpdate() {
            if (editTarget) {
                updateTarget();
                return;
            }
            addNewTarget();
        }

        @Override
        public boolean canWindowSaveOrUpdate() {
            return editTarget || !isDuplicate();
        }

    }

    private void createRequiredComponents() {
        controllerIDTextField = createTextField("prompt.target.id", UIComponentIdProvider.TARGET_ADD_CONTROLLER_ID);
        controllerIDTextField
                .addValidator(new RegexpValidator("[.\\S]*", i18n.getMessage("message.target.whitespace.check")));
        nameTextField = createTextField("textfield.name", UIComponentIdProvider.TARGET_ADD_NAME);
        nameTextField.setRequired(false);

        descTextArea = new TextAreaBuilder().caption(i18n.getMessage("textfield.description")).style("text-area-style")
                .prompt(i18n.getMessage("textfield.description")).immediate(true)
                .id(UIComponentIdProvider.TARGET_ADD_DESC).buildTextComponent();
        descTextArea.setNullRepresentation("");
    }

    private TextField createTextField(final String in18Key, final String id) {
        return new TextFieldBuilder().caption(i18n.getMessage(in18Key)).required(true).prompt(i18n.getMessage(in18Key))
                .immediate(true).id(id).buildTextComponent();
    }

    private void buildLayout() {
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
        /* save updated entity */
        final Target target = targetManagement.update(entityFactory.target().update(controllerId)
                .name(nameTextField.getValue()).description(descTextArea.getValue()));
        /* display success msg */
        uINotification.displaySuccess(i18n.getMessage("message.update.success", new Object[] { target.getName() }));
        // publishing through event bus
        eventBus.publish(this, new TargetTableEvent(BaseEntityEventType.UPDATED_ENTITY, target));
    }

    private void addNewTarget() {
        final String newControllerId = controllerIDTextField.getValue();
        final String newName = nameTextField.getValue();
        final String newDesc = descTextArea.getValue();

        final Target newTarget = targetManagement.create(
                entityFactory.target().create().controllerId(newControllerId).name(newName).description(newDesc));

        eventBus.publish(this, new TargetTableEvent(BaseEntityEventType.ADD_ENTITY, newTarget));
        uINotification.displaySuccess(i18n.getMessage("message.save.success", new Object[] { newTarget.getName() }));
        targetTable.setValue(Sets.newHashSet(newTarget.getId()));
    }

    public Window createNewWindow() {
        window = new WindowBuilder(SPUIDefinitions.CREATE_UPDATE_WINDOW)
                .caption(i18n.getMessage(UIComponentIdProvider.TARGET_ADD_CAPTION)).content(this).layout(formLayout)
                .i18n(i18n).saveDialogCloseListener(new SaveOnDialogCloseListener()).buildCommonDialogWindow();
        return window;
    }

    /**
     * Returns Target Update window based on the selected Entity Id in the
     * target table.
     * 
     * @param controllerId
     *            the target controller id
     * @return window or {@code null} if target is not exists.
     */
    public Window getWindow(final String controllerId) {
        final Optional<Target> target = targetManagement.getByControllerID(controllerId);
        if (!target.isPresent()) {
            uINotification.displayWarning(i18n.getMessage("target.not.exists", controllerId));
            return null;
        }
        populateValuesOfTarget(target.get());
        createNewWindow();
        window.addStyleName("target-update-window");
        return window;
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
    }

    private boolean isDuplicate() {
        final String newControlllerId = controllerIDTextField.getValue();
        final Optional<Target> existingTarget = targetManagement.getByControllerID(newControlllerId.trim());
        if (existingTarget.isPresent()) {
            uINotification.displayValidationError(i18n.getMessage("message.target.duplicate.check", newControlllerId));
            return true;
        } else {
            return false;
        }

    }

    private void populateValuesOfTarget(final Target target) {
        resetComponents();
        this.controllerId = target.getControllerId();
        editTarget = Boolean.TRUE;

        controllerIDTextField.setValue(target.getControllerId());
        controllerIDTextField.setEnabled(Boolean.FALSE);
        nameTextField.setValue(target.getName());
        nameTextField.setRequired(true);
        descTextArea.setValue(target.getDescription());
    }

    public FormLayout getFormLayout() {
        return formLayout;
    }

}
