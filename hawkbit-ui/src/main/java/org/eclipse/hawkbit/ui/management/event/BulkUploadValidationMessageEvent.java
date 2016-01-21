/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.event;

/**
 * Holds error message on failure of tag or distribution set assignment in bulk
 * targets upload.
 * 
 */
public class BulkUploadValidationMessageEvent {

    String validationErrorMessage;

    /**
     * @param validationErrorMessage
     *            error message on validation failure.
     */
    public BulkUploadValidationMessageEvent(final String validationErrorMessage) {
        this.validationErrorMessage = validationErrorMessage;
    }

    /**
     * @return the validationErrorMessage
     */
    public String getValidationErrorMessage() {
        return validationErrorMessage;
    }

}
