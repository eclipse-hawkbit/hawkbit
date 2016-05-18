/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.util.ArrayList;
import java.util.List;

/**
 * Query validation result with expected token on error.
 * 
 *
 *
 */
public class ValidationResult {

    private List<String> expectedTokens = new ArrayList<>();

    private String message;

    private Boolean isValidationFailed = Boolean.FALSE;

    /**
     * @return the isValidationFailed
     */
    public Boolean getIsValidationFailed() {
        return isValidationFailed;
    }

    /**
     * @param isValidationFailed
     *            the isValidationFailed to set
     */
    public void setIsValidationFailed(final Boolean isValidationFailed) {
        this.isValidationFailed = isValidationFailed;
    }

    /**
     * @return the expectedTokens
     */
    public List<String> getExpectedTokens() {
        return expectedTokens;
    }

    /**
     * @param expectedTokens
     *            the expectedTokens to set
     */
    public void setExpectedTokens(final List<String> expectedTokens) {
        this.expectedTokens = expectedTokens;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message
     *            the message to set
     */
    public void setMessage(final String message) {
        this.message = message;
    }

}
