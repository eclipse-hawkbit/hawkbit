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

/**
 * A suggestion which contains the start and the end character position of the
 * suggested token of the suggestion of the token and the actual suggestion.
 */
public class SuggestTokenDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private int start;
    private int end;
    private String suggestion;

    /**
     * Default constructor.
     */
    public SuggestTokenDto() {
        // necessary for java serialization with GWT.
    }

    /**
     * Constructor.
     * 
     * @param start
     *            the character position of the start of the token
     * @param end
     *            the character position of the end of the token
     * @param suggestion
     *            the token suggestion
     */
    public SuggestTokenDto(final int start, final int end, final String suggestion) {
        this.start = start;
        this.end = end;
        this.suggestion = suggestion;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setStart(final int start) {
        this.start = start;
    }

    public void setEnd(final int end) {
        this.end = end;
    }

    public void setSuggestion(final String suggestion) {
        this.suggestion = suggestion;
    }

    @Override
    public String toString() {
        return "SuggestTokenDto [start=" + start + ", end=" + end + ", suggestion=" + suggestion + "]";
    }
}
