/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.exception;

import java.io.Serial;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;

/**
 * The {@link #InvalidTenantConfigurationKeyException} is thrown when an invalid configuration key is used.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InvalidTenantConfigurationKeyException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final SpServerError THIS_ERROR = SpServerError.SP_CONFIGURATION_KEY_INVALID;

    public InvalidTenantConfigurationKeyException(final String message) {
        super(THIS_ERROR, message);
    }
}