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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.ui.artifacts.smtable.SoftwareModuleAddUpdateWindow;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleBorderWithIcon;
import org.eclipse.hawkbit.ui.layouts.AbstractCreateUpdateTagLayout;
import org.eclipse.hawkbit.ui.management.targettable.TargetAddUpdateWindowLayout;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;

import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.TextChangeNotifier;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * TODO: AbstractColorPicker und FlexibleOptionGroupItemComponent Superclass for
 * pop-up-windows including a minimize and close icon in the upper right corner
 * and a save and cancel button at the bottom.
 */
public class CommonDialogWindow extends Window implements Serializable {

    private static final long serialVersionUID = 1L;

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

    private Map<Component, Object> orginalValues;

    private final List<AbstractField<?>> allComponents;

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
            final FormLayout formLayout, final I18N i18n) {
        checkNotNull(saveButtonClickListener);
        checkNotNull(cancelButtonClickListener);
        this.caption = caption;
        this.content = content;
        this.helpLink = helpLink;
        this.saveButtonClickListener = saveButtonClickListener;
        this.cancelButtonClickListener = cancelButtonClickListener;
        this.allComponents = getAllComponents(formLayout);
        this.i18n = i18n;
        init();
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
        setOrginaleValues();
        addListeners();
    }

    private final void setOrginaleValues() {
        for (final AbstractField<?> field : allComponents) {
            orginalValues.put(field, field.getValue());
        }
    }

    private final void addListeners() {
        for (final AbstractField<?> field : allComponents) {
            field.addValueChangeListener(event -> saveButton.setEnabled(isSaveButtonEnabled()));
            if (field instanceof TextChangeNotifier) {
                ((TextChangeNotifier) field)
                        .addTextChangeListener(event -> saveButton.setEnabled(isSaveButtonEnabled()));
            }
        }

        saveButton.addClickListener(event -> save());
    }

    private void save() {
        saveButton.setEnabled(false);
        setOrginaleValues();
    }

    private boolean isSaveButtonEnabled() {
        return isMandatoyFieldNotEmpty() && isValuesChanged();
    }

    private boolean isValuesChanged() {
        for (final AbstractField<?> field : allComponents) {
            final Object currentValue = field.getValue();
            final Object orginalValue = orginalValues.get(field);

            if (!Objects.equals(currentValue, orginalValue)) {
                return true;
            }

        }
        return false;
    }

    private boolean shouldMandatoryLabelShown() {
        for (final AbstractField<?> field : allComponents) {
            if (field.isRequired()) {
                return true;
            }
        }

        return false;
    }

    private boolean isMandatoyFieldNotEmpty() {
        for (final AbstractField<?> field : allComponents) {
            // TODO empty string.
            if (field.isRequired() && field.getValue() == null) {
                return false;
            }
        }
        return true;
    }

    private List<AbstractField<?>> getAllComponents(final AbstractLayout abstractLayout) {
        final List<AbstractField<?>> components = new ArrayList<>();

        final Iterator<Component> iterate = abstractLayout.iterator();
        while (iterate.hasNext()) {
            final Component c = iterate.next();
            if (c instanceof AbstractLayout) {
                components.addAll(getAllComponents((AbstractLayout) c));
            }

            if (c instanceof AbstractField) {
                components.add((AbstractField<?>) c);
            }
        }
        return components;
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

        if (!shouldMandatoryLabelShown()) {
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

    private void addHelpLink() {

        if (StringUtils.isEmpty(helpLink)) {
            return;
        }
        final Link helpLinkComponent = SPUIComponentProvider.getHelpLink(helpLink);
        buttonsLayout.addComponent(helpLinkComponent);
        buttonsLayout.setComponentAlignment(helpLinkComponent, Alignment.MIDDLE_RIGHT);
    }

}
