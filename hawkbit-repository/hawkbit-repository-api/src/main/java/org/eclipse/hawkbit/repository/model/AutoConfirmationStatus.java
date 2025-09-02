/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

/**
 * {@link AutoConfirmationStatus} of a {@link Target}.
 */
public interface AutoConfirmationStatus extends BaseEntity {

    /**
     * For which target this status is corresponding to.
     *
     * @return the {@link Target}
     */
    Target getTarget();

    /**
     * The user who initiated the auto confirmation. Will be set on auto confirmation activation and could be null. In this case the created_by
     * can be considered as initiator.
     *
     * @return the user
     */
    String getInitiator();

    /**
     * Unix timestamp of the activation.
     *
     * @return activation time as unix timestamp
     */
    long getActivatedAt();

    /**
     * Optional value, which can be set during activation.
     *
     * @return the remark
     */
    String getRemark();

    /**
     * Construct the action message based on the current status.
     *
     * @return the constructed message which can be used for the action status as a message
     */
    String constructActionMessage();
}