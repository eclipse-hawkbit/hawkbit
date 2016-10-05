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
     * @param rsqlQuery
     *            the original RSQL based query the suggestions based on
     * @param cursorPosition
     *            the current cursor position
     * @param suggestions
     *            the suggestions for the current cursor position
     */
    public SuggestionContextDto(final int cursorPosition, final List<SuggestTokenDto> suggestions) {
        this.cursorPosition = cursorPosition;
        this.suggestions = suggestions;
    }

    public List<SuggestTokenDto> getSuggestions() {
        return suggestions;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public void setCursorPosition(final int cursorPosition) {
        this.cursorPosition = cursorPosition;
    }

    public void setSuggestions(final List<SuggestTokenDto> suggestions) {
        this.suggestions = suggestions;
    }

    @Override
    public String toString() {
        return "SuggestionContextDto [cursorPosition=" + cursorPosition + ", suggestions=" + suggestions + "]";
    }
}
