/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    public SuggestionContext getSuggestionContext() {
        return suggestionContext;
    }

    public SyntaxErrorContext getSyntaxErrorContext() {
        return syntaxErrorContext;
    }

    public void setSyntaxError(final boolean syntaxError) {
        this.syntaxError = syntaxError;
    }

    public void setSuggestionContext(final SuggestionContext suggestionContext) {
        this.suggestionContext = suggestionContext;
    }

    public void setSyntaxErrorContext(final SyntaxErrorContext syntaxErrorContext) {
        this.syntaxErrorContext = syntaxErrorContext;
    }
}
