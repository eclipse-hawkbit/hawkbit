/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * The {@link Target} is the target of all provisioning operations. It contains
 * the currently installed {@link DistributionSet} (i.e. current state). In
 * addition it holds the target {@link DistributionSet} that has to be
 * provisioned next (i.e. target state).
 * </p>
 */
public interface Target extends NamedEntity {

    /**
     * Maximum length of controllerId.
     */
    int CONTROLLER_ID_MAX_SIZE = 256;

    /**
     * Maximum length of securityToken.
     */
    int SECURITY_TOKEN_MAX_SIZE = 128;

    /**
     * Maximum length of address.
     */
    int ADDRESS_MAX_SIZE = 512;

    /**
     * Maximum length of key of controller attribute
     */
    int CONTROLLER_ATTRIBUTE_KEY_SIZE = 128;

    /**
     * Maximum length of value of controller attribute
     */
    int CONTROLLER_ATTRIBUTE_VALUE_SIZE = 128;

    /**
     * @return business identifier of the {@link Target}
     */
    String getControllerId();

    /**
     * @return the securityToken
     */
    String getSecurityToken();

    /**
     * @return {@link TargetWithActionType} with default settings
     */
    default TargetWithActionType getTargetWithActionType() {
        return new TargetWithActionType(getControllerId());
    }

    /**
     * @return the address under which the target can be reached
     */
    URI getAddress();

    /**
     * @return time in {@link TimeUnit#MILLISECONDS} GMT when the {@link Target}
     *         polled the server the last time or <code>null</code> if target
     *         has never queried yet.
     */
    Long getLastTargetQuery();

    /**
     * @return time in {@link TimeUnit#MILLISECONDS} GMT when
     *         {@link #getInstalledDistributionSet()} was applied.
     */
    Long getInstallationDate();

    /**
     * @return current status of the {@link Target}.
     */
    TargetUpdateStatus getUpdateStatus();

    /**
     * @return Target type {@link TargetType}.
     */
    TargetType getTargetType();

    /**
     * @return the poll time which holds the last poll time of the target, the
     *         next poll time and the overdue time. In case the
     *         {@link #lastTargetQuery} is not set e.g. the target never polled
     *         before this method returns {@code null}
     */
    PollStatus getPollStatus();

    /**
     * The auto confirmation status is present, when it's active for the target.
     * Will only be considered in case the confirmation flow is active.
     *
     * @return the {@link AutoConfirmationStatus} if activated
     */
    AutoConfirmationStatus getAutoConfirmationStatus();

    /**
     * @return <code>true</code> if the {@link Target} has not jet provided
     *         {@link #getControllerAttributes()}.
     */
    boolean isRequestControllerAttributes();
}
