/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.rsql.RsqlValidationOracle;
import org.eclipse.hawkbit.repository.rsql.ValidationOracleContext;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;

/**
 * An textfield with the {@link TextFieldSuggestionBox} extension which shows
 * suggestions in a suggestion-pop-up window while typing.
 */
public class AutoCompleteTextFieldComponent extends CustomField<String> {
    private static final long serialVersionUID = 1L;

    private final transient RsqlValidationOracle rsqlValidationOracle;

    private final transient List<ValidationListener> listeners = new LinkedList<>();

    private final Label validationIcon;
    private final TextField queryTextField;
    private final HorizontalLayout autoCompleteLayout;

    private boolean isValid;
    private String targetFilterQuery;

    /**
     * Constructor for AutoCompleteTextFieldComponent
     *
     * @param rsqlValidationOracle
     *            RsqlValidationOracle
     */
    public AutoCompleteTextFieldComponent(final RsqlValidationOracle rsqlValidationOracle) {
        this.rsqlValidationOracle = rsqlValidationOracle;

        this.validationIcon = createStatusIcon();
        this.queryTextField = createSearchField();
        this.autoCompleteLayout = new HorizontalLayout();

        this.isValid = false;
        this.targetFilterQuery = "";

        init();
    }

    private void init() {
        autoCompleteLayout.setSizeFull();
        autoCompleteLayout.setSpacing(false);
        autoCompleteLayout.setMargin(false);
        autoCompleteLayout.addStyleName("custom-search-layout");

        autoCompleteLayout.addComponent(validationIcon);
        autoCompleteLayout.setComponentAlignment(validationIcon, Alignment.MIDDLE_CENTER);

        autoCompleteLayout.addComponent(queryTextField);
        autoCompleteLayout.setExpandRatio(queryTextField, 1.0F);

        queryTextField.addValueChangeListener(event -> onQueryFilterChange(event.getValue()));

        new TextFieldSuggestionBox(rsqlValidationOracle).extend(queryTextField);
    }

    @Override
    protected Component initContent() {
        return autoCompleteLayout;
    }

    @Override
    protected void doSetValue(final String value) {
        if (value == null) {
            queryTextField.setValue("");
            hideValidationIcon();

            targetFilterQuery = "";
            isValid = false;
        } else {
            queryTextField.setValue(value);
        }
    }

    @Override
    public String getValue() {
        return targetFilterQuery;
    }

    /**
     * Add changed listener to text field
     *
     * @param listener
     *            Value change listener
     */
    public void addTextfieldChangedListener(final ValueChangeListener<String> listener) {
        queryTextField.addValueChangeListener(listener);
    }

    private static Label createStatusIcon() {
        final Label statusIcon = new Label(VaadinIcons.CHECK_CIRCLE.getHtml(), ContentMode.HTML);
        statusIcon.setId(UIComponentIdProvider.VALIDATION_STATUS_ICON_ID);
        statusIcon.setVisible(false);

        return statusIcon;
    }

    private static TextField createSearchField() {
        final TextField textField = new TextFieldBuilder(TargetFilterQuery.QUERY_MAX_SIZE)
                .id(UIComponentIdProvider.CUSTOM_FILTER_QUERY).buildTextComponent();
        textField.setWidthFull();
        textField.addStyleName("target-filter-textfield");

        textField.setValueChangeMode(ValueChangeMode.EAGER);

        return textField;
    }

    /**
     * Clears the textfield and resets the validation icon.
     */
    @Override
    public void clear() {
        queryTextField.clear();
        hideValidationIcon();
    }

    @Override
    public void focus() {
        queryTextField.focus();
    }

    /**
     * Adds the given listener
     * 
     * @param validationListener
     *            the listener to be called in case of validation status change
     */
    public void addValidationListener(final ValidationListener validationListener) {
        listeners.add(validationListener);
    }

    private void onQueryFilterChange(final String newQuery) {
        final ValidationOracleContext validationContext = rsqlValidationOracle.suggest(newQuery, newQuery.length());
        final boolean valid = !validationContext.isSyntaxError();
        final String message = valid ? newQuery : validationContext.getSyntaxErrorContext().getErrorMessage();

        targetFilterQuery = newQuery;
        isValid = valid;

        if (valid) {
            showValidationSuccesIcon();
        } else {
            showValidationFailureIcon(message);
        }

        fireEvent(createValueChange(newQuery, false));
        listeners.forEach(listener -> listener.validationChanged(valid, message));
    }

    private void hideValidationIcon() {
        validationIcon.setVisible(false);
    }

    private void showValidationSuccesIcon() {
        validationIcon.setVisible(true);
        validationIcon.setValue(VaadinIcons.CHECK_CIRCLE.getHtml());
        validationIcon.setStyleName(SPUIStyleDefinitions.SUCCESS_ICON);
        validationIcon.setDescription(null);
    }

    private void showValidationFailureIcon(final String validationMessage) {
        validationIcon.setVisible(true);
        validationIcon.setValue(VaadinIcons.CLOSE_CIRCLE.getHtml());
        validationIcon.setStyleName(SPUIStyleDefinitions.ERROR_ICON);
        validationIcon.setDescription(validationMessage);
    }

    /**
     * @return True if auto complete component else false
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Change listener on the textfield.
     */
    @FunctionalInterface
    public interface ValidationListener {
        /**
         * Called when the text has been changed and validated.
         * 
         * @param valid
         *            indicates if the entered query text is valid
         * @param validationMessage
         *            the entered query text
         */
        void validationChanged(final boolean valid, final String validationMessage);
    }
}
