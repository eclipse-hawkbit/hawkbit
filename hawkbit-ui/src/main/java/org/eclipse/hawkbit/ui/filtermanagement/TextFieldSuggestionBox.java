/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.rsql.RsqlValidationOracle;
import org.eclipse.hawkbit.repository.rsql.SuggestionContext;
import org.eclipse.hawkbit.repository.rsql.ValidationOracleContext;
import org.eclipse.hawkbit.ui.filtermanagement.client.SuggestTokenDto;
import org.eclipse.hawkbit.ui.filtermanagement.client.SuggestionContextDto;
import org.eclipse.hawkbit.ui.filtermanagement.client.TextFieldSuggestionBoxClientRpc;
import org.eclipse.hawkbit.ui.filtermanagement.client.TextFieldSuggestionBoxServerRpc;

import com.vaadin.server.AbstractExtension;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

/**
 * Extension for the AutoCompleteTexfield.
 *
 */
public class TextFieldSuggestionBox extends AbstractExtension implements TextFieldSuggestionBoxServerRpc {

    private static final long serialVersionUID = 1L;
    private final transient RsqlValidationOracle rsqlValidationOracle;
    private final AutoCompleteTextFieldComponent autoCompleteTextFieldComponent;

    /**
     * Constructor.
     * 
     * @param autoCompleteTextFieldComponent
     * @param rsqlValidationOracle
     *            the suggestion oracle where to retrieve the suggestions from
     */
    public TextFieldSuggestionBox(final RsqlValidationOracle rsqlValidationOracle,
            final AutoCompleteTextFieldComponent autoCompleteTextFieldComponent) {
        this.rsqlValidationOracle = rsqlValidationOracle;
        this.autoCompleteTextFieldComponent = autoCompleteTextFieldComponent;

        registerRpc(this, TextFieldSuggestionBoxServerRpc.class);
    }

    /**
     * Add this extension to the target connector. This method is protected to
     * allow subclasses to require a more specific type of target.
     * 
     * @param target
     *            the connector to attach this extension to
     */
    public void extend(final TextField target) {
        super.extend(target);
    }

    @Override
    public void suggest(final String text, final int cursor) {
        final ValidationOracleContext suggest = rsqlValidationOracle.suggest(text, cursor);
        updateValidationIcon(suggest, text);
        getRpcProxy(TextFieldSuggestionBoxClientRpc.class).showSuggestions(mapToDto(suggest.getSuggestionContext()));
    }

    @Override
    public void executeQuery(final String text, final int cursor) {
        if (!autoCompleteTextFieldComponent.isValidationError()) {
            autoCompleteTextFieldComponent.showValidationInProgress();
            autoCompleteTextFieldComponent.getExecutor()
                    .execute(autoCompleteTextFieldComponent.new StatusCircledAsync(UI.getCurrent()));
        }
    }

    private static SuggestionContextDto mapToDto(final SuggestionContext suggestionContext) {
        return new SuggestionContextDto(suggestionContext.getCursorPosition(),
                suggestionContext.getSuggestions().stream()
                        .filter(suggestion -> suggestion.getTokenImageName() == null
                                || suggestion.getSuggestion().contains(suggestion.getTokenImageName()))
                        .map(suggestion -> new SuggestTokenDto(suggestion.getStart(), suggestion.getEnd(),
                                suggestion.getSuggestion()))
                        .collect(Collectors.toList()));

    }

    private void updateValidationIcon(final ValidationOracleContext suggest, final String text) {
        final String errorMessage = (suggest.getSyntaxErrorContext() != null)
                ? suggest.getSyntaxErrorContext().getErrorMessage() : null;
        autoCompleteTextFieldComponent.onQueryFilterChange(text, !suggest.isSyntaxError(), errorMessage);
    }
}
