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

import java.util.ArrayList;
import java.util.List;

/**
 * The context which holds suggestions for the current cursor position.
 */
public class SuggestionContext {

    private String rsqlQuery;
    private int cursorPosition;
    private List<SuggestToken> suggestions = new ArrayList<>();

    /**
     * Default constructor.
     */
    public SuggestionContext() {
        // nothing to initialize
    }

    /**
     * Constructor.
     *
     * @param rsqlQuery the original RSQL based query the suggestions based on
     * @param cursorPosition the current cursor position
     * @param suggestions the suggestions for the current cursor position
     */
    public SuggestionContext(final String rsqlQuery, final int cursorPosition, final List<SuggestToken> suggestions) {
        this.rsqlQuery = rsqlQuery;
        this.cursorPosition = cursorPosition;
        this.suggestions = suggestions;
    }

    public List<SuggestToken> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(final List<SuggestToken> suggestions) {
        this.suggestions = suggestions;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public void setCursorPosition(final int cursorPosition) {
        this.cursorPosition = cursorPosition;
    }

    public String getRsqlQuery() {
        return rsqlQuery;
    }

    public void setRsqlQuery(final String rsqlQuery) {
        this.rsqlQuery = rsqlQuery;
    }
}
