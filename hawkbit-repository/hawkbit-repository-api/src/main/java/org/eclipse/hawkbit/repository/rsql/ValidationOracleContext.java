/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.rsql;

/**
 * A context object which contains information about validation and suggestions
 * of a parsed RSQL query.
 */
public class ValidationOracleContext {

    private boolean syntaxError;

    private SuggestionContext suggestionContext;

    private SyntaxErrorContext syntaxErrorContext;

    public boolean isSyntaxError() {
        return syntaxError;
    }

    public void setSyntaxError(final boolean syntaxError) {
        this.syntaxError = syntaxError;
    }

    public SuggestionContext getSuggestionContext() {
        return suggestionContext;
    }

    public void setSuggestionContext(final SuggestionContext suggestionContext) {
        this.suggestionContext = suggestionContext;
    }

    public SyntaxErrorContext getSyntaxErrorContext() {
        return syntaxErrorContext;
    }

    public void setSyntaxErrorContext(final SyntaxErrorContext syntaxErrorContext) {
        this.syntaxErrorContext = syntaxErrorContext;
    }
}
