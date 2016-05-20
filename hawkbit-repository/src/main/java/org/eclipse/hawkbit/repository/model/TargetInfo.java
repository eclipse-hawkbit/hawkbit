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
import java.util.Map;

import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInfo.PollStatus;

public interface TargetInfo extends Serializable {

    Long getId();

    /**
     * @return the ipAddress
     */
    URI getAddress();

    Target getTarget();

    Long getLastTargetQuery();

    Map<String, String> getControllerAttributes();

    Long getInstallationDate();

    TargetUpdateStatus getUpdateStatus();

    DistributionSet getInstalledDistributionSet();

    /**
     * @return the poll time which holds the last poll time of the target, the
     *         next poll time and the overdue time. In case the
     *         {@link #lastTargetQuery} is not set e.g. the target never polled
     *         before this method returns {@code null}
     */
    PollStatus getPollStatus();

}