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
import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;

import org.eclipse.hawkbit.repository.rsql.RsqlValidationOracle;
import org.eclipse.hawkbit.ui.common.builder.TextFieldBuilder;
import org.eclipse.hawkbit.ui.filtermanagement.event.CustomFilterUIEvent;
import org.eclipse.hawkbit.ui.filtermanagement.state.FilterManagementUIState;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.vaadin.spring.events.EventBus;

import com.vaadin.event.ShortcutListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

/**
 * An textfield with the {@link TextFieldSuggestionBox} extension which shows
 * suggestions in a suggestion-pop-up window while typing.
 */
@SpringComponent
@ViewScope
public class AutoCompleteTextFieldComponent extends HorizontalLayout {

    private static final long serialVersionUID = 1L;

    @Autowired
    private FilterManagementUIState filterManagementUIState;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient RsqlValidationOracle rsqlValidationOracle;

    @Autowired
    @Qualifier("uiExecutor")
    private transient Executor executor;

    private transient List<FilterQueryChangeListener> listeners = new LinkedList<>();

    private final Label validationIcon;
    private final TextField queryTextField;

    /**
     * Constructor.
     */
    public AutoCompleteTextFieldComponent() {

        queryTextField = createSearchField();
        validationIcon = createStatusIcon();

        setSizeUndefined();
        setSpacing(true);
        addStyleName("custom-search-layout");
        addComponents(validationIcon, queryTextField);
        setComponentAlignment(validationIcon, Alignment.TOP_CENTER);
    }

    /**
     * Called by the spring-framework when this bean has be post-constructed.
     */
    @PostConstruct
    public void postConstruct() {
        new TextFieldSuggestionBox(rsqlValidationOracle, this).extend(queryTextField);
        queryTextField.addShortcutListener(new EnterShortCutListener());
    }

    /**
     * Clears the textfield and resets the validation icon.
     */
    public void clear() {
        queryTextField.clear();
        validationIcon.setValue(FontAwesome.CHECK_CIRCLE.getHtml());
        validationIcon.setStyleName("hide-status-label");
    }

    @Override
    public void focus() {
        queryTextField.focus();
    }

    /**
     * Adds the given listener
     * 
     * @param textChangeListener
     *            the listener to be called in case of text changed
     */
    public void addTextChangeListener(final FilterQueryChangeListener textChangeListener) {
        listeners.add(textChangeListener);
    }

    public void setValue(final String textValue) {
        queryTextField.setValue(textValue);
    }

    public String getValue() {
        return queryTextField.getValue();
    }

    /**
     * Called when the filter-query has been changed in the textfield, e.g. from
     * client-side.
     * 
     * @param currentText
     *            the current text of the textfield which has been changed
     * @param valid
     *            {@code boolean} if the current text is RSQL syntax valid
     *            otherwise {@code false}
     * @param validationMessage
     *            a message shown in case of syntax errors as tooltip
     */
    public void onQueryFilterChange(final String currentText, final boolean valid, final String validationMessage) {
        if (valid) {
            showValidationSuccesIcon(currentText);
        } else {
            showValidationFailureIcon(validationMessage);
        }
        listeners.forEach(listener -> listener.queryChanged(valid, currentText));
    }

    /**
     * Shows the validation success icon in the textfield
     * 
     * @param text
     *            the text to store in the UI state object
     */
    public void showValidationSuccesIcon(final String text) {
        validationIcon.setValue(FontAwesome.CHECK_CIRCLE.getHtml());
        validationIcon.setStyleName(SPUIStyleDefinitions.SUCCESS_ICON);
        filterManagementUIState.setFilterQueryValue(text);
        filterManagementUIState.setIsFilterByInvalidFilterQuery(Boolean.FALSE);
    }

    /**
     * Shows the validation error icon in the textfield
     * 
     * @param validationMessage
     *            the validation message which should be added to the error-icon
     *            tooltip
     */
    public void showValidationFailureIcon(final String validationMessage) {
        validationIcon.setValue(FontAwesome.TIMES_CIRCLE.getHtml());
        validationIcon.setStyleName(SPUIStyleDefinitions.ERROR_ICON);
        validationIcon.setDescription(validationMessage);
        filterManagementUIState.setFilterQueryValue(null);
        filterManagementUIState.setIsFilterByInvalidFilterQuery(Boolean.TRUE);
    }

    public boolean isValidationError() {
        return validationIcon.getStyleName().equals(SPUIStyleDefinitions.ERROR_ICON);
    }

    private TextField createSearchField() {
        final TextField textField = new TextFieldBuilder().immediate(true).id("custom.query.text.Id")
                .maxLengthAllowed(SPUILabelDefinitions.TARGET_FILTER_QUERY_TEXT_FIELD_LENGTH).buildTextComponent();
        textField.addStyleName("target-filter-textfield");
        textField.setWidth(900.0F, Unit.PIXELS);
        textField.setTextChangeEventMode(TextChangeEventMode.EAGER);
        textField.setImmediate(true);
        textField.setTextChangeTimeout(100);
        textField.addShortcutListener(new EnterShortCutListener());
        return textField;
    }

    private static Label createStatusIcon() {
        final Label statusIcon = new Label();
        statusIcon.setImmediate(true);
        statusIcon.setContentMode(ContentMode.HTML);
        statusIcon.setSizeFull();
        setInitialStatusIconStyle(statusIcon);
        statusIcon.setId(SPUIComponentIdProvider.VALIDATION_STATUS_ICON_ID);
        return statusIcon;
    }

    private static void setInitialStatusIconStyle(final Label statusIcon) {
        statusIcon.setValue(FontAwesome.CHECK_CIRCLE.getHtml());
        statusIcon.setStyleName("hide-status-label");
    }

    class StatusCircledAsync implements Runnable {
        private final UI current;

        public StatusCircledAsync(final UI current) {
            this.current = current;
        }

        @Override
        public void run() {
            UI.setCurrent(current);
            eventBus.publish(this, CustomFilterUIEvent.FILTER_TARGET_BY_QUERY);
        }
    }

    private final class EnterShortCutListener extends ShortcutListener {

        private static final long serialVersionUID = 1L;

        public EnterShortCutListener() {
            super("Enter", KeyCode.ENTER, new int[0]);
        }

        @Override
        public void handleAction(final Object sender, final Object target) {
            if (!isValidationError()) {
                showValidationInProgress();
                executor.execute(new StatusCircledAsync(UI.getCurrent()));
            }
        }

        private void showValidationInProgress() {
            validationIcon.setValue(null);
            validationIcon.addStyleName("show-status-label");
            validationIcon.setStyleName(SPUIStyleDefinitions.TARGET_FILTER_SEARCH_PROGRESS_INDICATOR_STYLE);
        }
    }

    /**
     * Change listener on the textfield.
     */
    @FunctionalInterface
    public interface FilterQueryChangeListener {
        /**
         * Called when the text has been changed and validated.
         * 
         * @param valid
         *            indicates if the entered query text is valid
         * @param query
         *            the entered query text
         */
        void queryChanged(final boolean valid, final String query);
    }
}
