/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.repository.model.BaseEntity;

/**
 * Thrown if too many entries are added to repository.
 *
 */
public final class QuotaExceededException extends AbstractServerRtException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new QuotaExceededException with
     * {@link SpServerError#SP_QUOTA_EXCEEDED} error.
     */
    public QuotaExceededException() {
        super(SpServerError.SP_QUOTA_EXCEEDED);
    }

    /**
     * @param cause
     *            for the exception
     */
    public QuotaExceededException(final Throwable cause) {
        super(SpServerError.SP_QUOTA_EXCEEDED, cause);
    }

    /**
     * @param type
     *            that hit quota
     * @param inserted
     *            cause for the hit
     * @param quota
     *            that is defined by the repository
     */
    public QuotaExceededException(final Class<? extends BaseEntity> type, final long inserted, final int quota) {
        this(type.getSimpleName(), inserted, quota);
    }

    /**
     * 
     * @param type
     *            that hit quota
     * @param inserted
     *            cause for the hit
     * @param quota
     *            that is defined by the repository
     */
    public QuotaExceededException(final String type, final long inserted, final int quota) {
        super("Request contains too many entries of {" + type + "}. {" + inserted + "} is bejond the permitted {"
                + quota + "}.", SpServerError.SP_QUOTA_EXCEEDED);
    }
}
