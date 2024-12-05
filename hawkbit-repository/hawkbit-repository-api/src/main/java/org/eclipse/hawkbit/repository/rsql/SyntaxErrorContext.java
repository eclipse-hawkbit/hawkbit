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
 * An syntax error context object which holds the character position of the
 * syntax error and message.
 */
public class SyntaxErrorContext {

    private int characterPosition = -1;
    private String errorMessage;

    /**
     * Default constructor.
     */
    public SyntaxErrorContext() {
        // nothing to initialize
    }

    /**
     * Constructor.
     *
     * @param characterPosition the position of the character within the RSQL query string the
     *         error occurs.
     * @param errorMessage the error message with further information
     */
    public SyntaxErrorContext(final int characterPosition, final String errorMessage) {
        this.characterPosition = characterPosition;
        this.errorMessage = errorMessage;
    }

    public int getCharacterPosition() {
        return characterPosition;
    }

    public void setCharacterPosition(final int characterPosition) {
        this.characterPosition = characterPosition;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
