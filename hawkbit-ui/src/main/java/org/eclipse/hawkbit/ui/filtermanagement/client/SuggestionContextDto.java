/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement.client;

import java.io.Serializable;
import java.util.List;

/**
 * Suggestion context with the current cursor position
 */
public class SuggestionContextDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private int cursorPosition;
    private List<SuggestTokenDto> suggestions;

    /**
     * Default constructor.
     */
    public SuggestionContextDto() {
        // necessary for java serialization with GWT.
    }

    /**
     * Constructor.
     * 
     * @param cursorPosition
     *            the current cursor position
     * @param suggestions
     *            the suggestions for the current cursor position
     */
    public SuggestionContextDto(final int cursorPosition, final List<SuggestTokenDto> suggestions) {
        this.cursorPosition = cursorPosition;
        this.suggestions = suggestions;
    }

    /**
     * @return Suggestions
     */
    public List<SuggestTokenDto> getSuggestions() {
        return suggestions;
    }

    /**
     * @return Current cursor position
     */
    public int getCursorPosition() {
        return cursorPosition;
    }

    /**
     * Sets the cursor position
     *
     * @param cursorPosition
     *          Cursor position
     */
    public void setCursorPosition(final int cursorPosition) {
        this.cursorPosition = cursorPosition;
    }

    /**
     * Sets the suggestions
     *
     * @param suggestions
     *          List of suggestions
     */
    public void setSuggestions(final List<SuggestTokenDto> suggestions) {
        this.suggestions = suggestions;
    }

    @Override
    public String toString() {
        return "SuggestionContextDto [cursorPosition=" + cursorPosition + ", suggestions=" + suggestions + "]";
    }
}
