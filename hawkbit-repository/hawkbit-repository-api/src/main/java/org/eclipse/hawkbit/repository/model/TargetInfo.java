/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.io.Serializable;
import java.net.URI;
import java.text.AttributedCharacterIterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface TargetInfo extends Serializable {
    /**
     * @return the address under which the target can be reached
     */
    URI getAddress();

    /**
     * @param address
     *            the target address to set
     *
     * @throws IllegalArgumentException
     *             If the given string violates RFC&nbsp;2396
     */
    void setAddress(String address);

    /**
     * @return {@link Target} this info element belongs to.
     */
    Target getTarget();

    /**
     * @return time in {@link TimeUnit#MILLISECONDS} GMT when the {@link Target}
     *         polled the server the last time.
     */
    Long getLastTargetQuery();

    /**
     * @return {@link AttributedCharacterIterator} that have been provided by
     *         the {@link Target} itself, e.g. hardware revision, serial number,
     *         mac address etc.
     */
    Map<String, String> getControllerAttributes();

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
     * @return currently installed {@link DistributionSet}.
     */
    DistributionSet getInstalledDistributionSet();

    /**
     * @return the poll time which holds the last poll time of the target, the
     *         next poll time and the overdue time. In case the
     *         {@link #lastTargetQuery} is not set e.g. the target never polled
     *         before this method returns {@code null}
     */
    PollStatus getPollStatus();

    /**
     * @return <code>true</code> if the {@link Target} has not jet provided
     *         {@link #getControllerAttributes()}.
     */
    boolean isRequestControllerAttributes();

}
