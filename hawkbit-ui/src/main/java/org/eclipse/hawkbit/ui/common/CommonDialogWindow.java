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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.ui.artifacts.smtable.SoftwareModuleAddUpdateWindow;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorderWithIcon;
import org.eclipse.hawkbit.ui.layouts.AbstractCreateUpdateTagLayout;
import org.eclipse.hawkbit.ui.management.targettable.TargetAddUpdateWindowLayout;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.vaadin.hene.flexibleoptiongroup.FlexibleOptionGroupItemComponent;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.vaadin.data.Container.ItemSetChangeEvent;
import com.vaadin.data.Container.ItemSetChangeListener;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Validator;
import com.vaadin.data.validator.NullValidator;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.FieldEvents.TextChangeNotifier;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

/**
 * 
 * Table pop-up-windows including a minimize and close icon in the upper right
 * corner and a save and cancel button at the bottom. Is not intended to reuse.
 * 
 */
public class CommonDialogWindow extends Window {

    private static final long serialVersionUID = 1L;

    private final VerticalLayout mainLayout = new VerticalLayout();

    private final String caption;

    private final Component content;

    private final String helpLink;

    private Button saveButton;

    private Button cancelButton;

    private HorizontalLayout buttonsLayout;

    protected ValueChangeListener buttonEnableListener;

    private final ClickListener cancelButtonClickListener;

    private final ClickListener closeClickListener = event -> onCloseEvent(event);

    private final transient Map<Component, Object> orginalValues;

    private final List<AbstractField<?>> allComponents;

    private final I18N i18n;

    private transient SaveDialogCloseListener closeListener;

    /**
     * Constructor.
     * 
     * @param caption
     *            the caption
     * @param content
     *            the content
     * @param helpLink
     *            the helpLinks
     * @param closeListener
     *            the saveDialogCloseListener
     * @param cancelButtonClickListener
     *            the cancelButtonClickListener
     * @param layout
     *            the abstract layout
     * @param i18n
     *            the i18n service
     */
    public CommonDialogWindow(final String caption, final Component content, final String helpLink,
            final SaveDialogCloseListener closeListener, final ClickListener cancelButtonClickListener,
            final AbstractLayout layout, final I18N i18n) {
        checkNotNull(closeListener);
        this.caption = caption;
        this.content = content;
        this.helpLink = helpLink;
        this.closeListener = closeListener;
        this.cancelButtonClickListener = cancelButtonClickListener;
        this.orginalValues = new HashMap<>();
        this.allComponents = getAllComponents(layout);
        this.i18n = i18n;
        init();
    }

    private void onCloseEvent(final ClickEvent clickEvent) {
        if (!clickEvent.getButton().equals(saveButton)) {
            close();
            return;
        }

        // check first the can window close before you save the dialog, because
        // if you save the dialog first, the entity exist and the duplicate
        // check is failing.
        final boolean canWindowClose = closeListener.canWindowClose();
        if (closeListener.canWindowSaveOrUpdate()) {
            closeListener.saveOrUpdate();
        }

        if (canWindowClose) {
            close();
        }

    }

    @Override
    public void close() {
        super.close();
        orginalValues.clear();
        removeListeners();
        allComponents.clear();
        this.saveButton.setEnabled(false);
    }

    private void removeListeners() {
        for (final AbstractField<?> field : allComponents) {
            removeTextListener(field);
            removeValueChangeListener(field);
            removeItemSetChangeistener(field);
        }
    }

    private void removeItemSetChangeistener(final AbstractField<?> field) {
        if (!(field instanceof Table)) {
            return;
        }
        for (final Object listener : field.getListeners(ItemSetChangeEvent.class)) {
            if (listener instanceof ChangeListener) {
                ((Table) field).removeItemSetChangeListener((ChangeListener) listener);
            }
        }
    }

    private void removeTextListener(final AbstractField<?> field) {
        if (!(field instanceof TextChangeNotifier)) {
            return;
        }
        for (final Object listener : field.getListeners(TextChangeEvent.class)) {
            if (listener instanceof ChangeListener) {
                ((TextChangeNotifier) field).removeTextChangeListener((ChangeListener) listener);
            }
        }
    }

    private void removeValueChangeListener(final AbstractField<?> field) {
        for (final Object listener : field.getListeners(ValueChangeEvent.class)) {
            if (listener instanceof ChangeListener) {
                field.removeValueChangeListener((ChangeListener) listener);
            }
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
            mainLayout.setExpandRatio(content, 1.0F);
        }

        createMandatoryLabel();

        final HorizontalLayout buttonLayout = createActionButtonsLayout();
        mainLayout.addComponent(buttonLayout);
        mainLayout.setComponentAlignment(buttonLayout, Alignment.TOP_CENTER);

        setCaption(caption);
        setCaptionAsHtml(true);
        setContent(mainLayout);
        setResizable(false);
        center();
        setModal(true);
        addStyleName("fontsize");
        setOrginaleValues();
        addComponenetListeners();
    }

    /**
     * saves the original values in a Map so we can use them for detecting
     * changes
     */
    public final void setOrginaleValues() {
        for (final AbstractField<?> field : allComponents) {
            Object value = field.getValue();

            if (field instanceof Table) {
                value = Sets.newHashSet(((Table) field).getContainerDataSource().getItemIds());
            }
            orginalValues.put(field, value);
        }
        saveButton.setEnabled(isSaveButtonEnabledAfterValueChange(null, null));
    }

    protected void addCloseListenerForSaveButton() {
        saveButton.addClickListener(closeClickListener);
    }

    protected void addCloseListenerForCancelButton() {
        cancelButton.addClickListener(closeClickListener);
    }

    protected void addComponenetListeners() {
        for (final AbstractField<?> field : allComponents) {
            if (field instanceof TextChangeNotifier) {
                ((TextChangeNotifier) field).addTextChangeListener(new ChangeListener(field));
            }

            if (field instanceof Table) {
                ((Table) field).addItemSetChangeListener(new ChangeListener(field));
            } else {
                field.addValueChangeListener(new ChangeListener(field));
            }
        }
    }

    private boolean isSaveButtonEnabledAfterValueChange(final Component currentChangedComponent,
            final Object newValue) {
        return isMandatoryFieldNotEmptyAndValid(currentChangedComponent, newValue)
                && isValuesChanged(currentChangedComponent, newValue);
    }

    private boolean isValuesChanged(final Component currentChangedComponent, final Object newValue) {
        for (final AbstractField<?> field : allComponents) {
            Object originalValue = orginalValues.get(field);
            if (field instanceof CheckBox && originalValue == null) {
                originalValue = Boolean.FALSE;
            }
            final Object currentValue = getCurrentVaue(currentChangedComponent, newValue, field);

            if (!isValueEquals(field, originalValue, currentValue)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValueEquals(final AbstractField<?> field, final Object orginalValue,
            final Object currentValue) {
        if (Set.class.equals(field.getType())) {
            return CollectionUtils.isEqualCollection(CollectionUtils.emptyIfNull((Collection<?>) orginalValue),
                    CollectionUtils.emptyIfNull((Collection<?>) currentValue));
        }

        if (String.class.equals(field.getType())) {
            return Objects.equals(Strings.emptyToNull((String) orginalValue),
                    Strings.emptyToNull((String) currentValue));
        }

        return Objects.equals(orginalValue, currentValue);
    }

    private static Object getCurrentVaue(final Component currentChangedComponent, final Object newValue,
            final AbstractField<?> field) {
        Object currentValue = field.getValue();
        if (field instanceof Table) {
            currentValue = ((Table) field).getContainerDataSource().getItemIds();
        }

        if (field.equals(currentChangedComponent)) {
            currentValue = newValue;
        }
        return currentValue;
    }

    private boolean shouldMandatoryLabelShown() {
        for (final AbstractField<?> field : allComponents) {
            if (field.isRequired()) {
                return true;
            }
        }

        return false;
    }

    private boolean isMandatoryFieldNotEmptyAndValid(final Component currentChangedComponent, final Object newValue) {

        boolean valid = true;
        final List<AbstractField<?>> requiredComponents = allComponents.stream().filter(field -> field.isRequired())
                .collect(Collectors.toList());

        requiredComponents.addAll(allComponents.stream().filter(this::hasNullValidator).collect(Collectors.toList()));

        for (final AbstractField field : requiredComponents) {
            Object value = getCurrentVaue(currentChangedComponent, newValue, field);

            if (String.class.equals(field.getType())) {
                value = Strings.emptyToNull((String) value);
            }

            if (Set.class.equals(field.getType())) {
                value = emptyToNull((Collection<?>) value);
            }

            if (value == null) {
                return false;
            }

            // We need to loop through the entire loop for validity testing.
            // Otherwise the UI will only mark the
            // first field with errors and then stop. If there are several
            // fields with errors, this is bad.
            field.setValue(value);
            if (!field.isValid()) {
                valid = false;
            }
        }

        return valid;
    }

    private static Object emptyToNull(final Collection<?> c) {
        return (c == null || c.isEmpty()) ? null : c;
    }

    private boolean hasNullValidator(final Component component) {

        if (component instanceof AbstractField<?>) {
            final AbstractField<?> fieldComponent = (AbstractField<?>) component;
            for (final Validator validator : fieldComponent.getValidators()) {
                if (validator instanceof NullValidator) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<AbstractField<?>> getAllComponents(final AbstractLayout abstractLayout) {
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

            if (c instanceof FlexibleOptionGroupItemComponent) {
                components.add(((FlexibleOptionGroupItemComponent) c).getOwner());
            }
        }
        return components;
    }

    private HorizontalLayout createActionButtonsLayout() {

        buttonsLayout = new HorizontalLayout();
        buttonsLayout.setSizeFull();
        buttonsLayout.setSpacing(true);
        buttonsLayout.setSpacing(true);
        buttonsLayout.addStyleName("actionButtonsMargin");

        createSaveButton();
        createCancelButton();

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
                FontAwesome.TIMES, SPUIButtonStyleNoBorderWithIcon.class);
        cancelButton.setSizeUndefined();
        cancelButton.addStyleName("default-color");
        addCloseListenerForCancelButton();
        if (cancelButtonClickListener != null) {
            cancelButton.addClickListener(cancelButtonClickListener);
        }

        buttonsLayout.addComponent(cancelButton);
        buttonsLayout.setComponentAlignment(cancelButton, Alignment.MIDDLE_LEFT);
        buttonsLayout.setExpandRatio(cancelButton, 1.0F);
    }

    private void createSaveButton() {
        saveButton = SPUIComponentProvider.getButton(SPUIComponentIdProvider.SAVE_BUTTON, "Save", "", "", true,
                FontAwesome.SAVE, SPUIButtonStyleNoBorderWithIcon.class);
        saveButton.setSizeUndefined();
        saveButton.addStyleName("default-color");
        addCloseListenerForSaveButton();
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

    public AbstractComponent getButtonsLayout() {
        return this.buttonsLayout;
    }

    private class ChangeListener implements ValueChangeListener, TextChangeListener, ItemSetChangeListener {

        private static final long serialVersionUID = 1L;
        private final Field<?> field;

        public ChangeListener(final Field<?> field) {
            super();
            this.field = field;
        }

        @Override
        public void textChange(final TextChangeEvent event) {
            saveButton.setEnabled(isSaveButtonEnabledAfterValueChange(field, event.getText()));
        }

        @Override
        public void valueChange(final ValueChangeEvent event) {
            saveButton.setEnabled(isSaveButtonEnabledAfterValueChange(field, field.getValue()));
        }

        @Override
        public void containerItemSetChange(final ItemSetChangeEvent event) {
            if (!(field instanceof Table)) {
                return;
            }
            final Table table = (Table) field;
            saveButton.setEnabled(
                    isSaveButtonEnabledAfterValueChange(table, table.getContainerDataSource().getItemIds()));
        }
    }

    /**
     * Adds the component manually to the allComponents-List and adds a
     * ValueChangeListener to it. Necessary in Update Distribution Type as the
     * CheckBox concerned is an ItemProperty...
     * 
     * @param component
     *            AbstractField
     */
    public void updateAllComponents(final AbstractField component) {

        allComponents.add(component);
        component.addValueChangeListener(new ChangeListener(component));
    }

    public VerticalLayout getMainLayout() {
        return mainLayout;
    }

    public void setSaveButtonEnabled(final boolean enabled) {
        saveButton.setEnabled(enabled);
    }

    public void setCancelButtonEnabled(final boolean enabled) {
        cancelButton.setEnabled(enabled);
    }

    /**
     * Called before the save happens.
     *
     */
    public interface SaveDialogCloseListener {

        /**
         * Default true for all the windows except for DistributionAddUpdate
         * window.
         * 
         * @return true/false .
         */
        boolean canWindowSaveOrUpdate();

        /**
         * @return true/false based on the dialog window to be closed or not.
         */
        default boolean canWindowClose() {
            return canWindowSaveOrUpdate();
        }

        /**
         * Saves/Updates
         * 
         */
        void saveOrUpdate();
    }

}
