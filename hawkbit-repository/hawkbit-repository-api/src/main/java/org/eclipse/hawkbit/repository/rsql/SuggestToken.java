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
 * A suggestion which contains the start and the end character position of the
 * suggested token of the suggestion of the token and the actual suggestion.
 */
public class SuggestToken {

    private final int start;
    private final int end;
    private final String suggestion;
    private final String tokenImageName;

    /**
     * Constructor.
     * 
     * @param start
     *            the character position of the start of the token
     * @param end
     *            the character position of the end of the token
     * @param tokenImageName
     *            the entered name of the token, e.g. could be the beginning of
     *            the suggestion like 'na' or 'name'
     * @param suggestion
     *            the token suggestion
     */
    public SuggestToken(final int start, final int end, final String tokenImageName, final String suggestion) {
        this.start = start;
        this.end = end;
        this.tokenImageName = tokenImageName;
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

    public String getTokenImageName() {
        return tokenImageName;
    }

    @Override
    public String toString() {
        return "SuggestToken [start=" + start + ", end=" + end + ", suggestion=" + suggestion + ", tokenImageName="
                + tokenImageName + "]";
    }
}
