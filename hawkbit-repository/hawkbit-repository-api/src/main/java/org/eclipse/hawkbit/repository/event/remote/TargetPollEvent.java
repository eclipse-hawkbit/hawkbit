/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.repository.event.remote;

import org.eclipse.hawkbit.repository.model.Target;

/**
 *
 */
public class TargetPollEvent extends RemoteTenantAwareEvent {

    private static final long serialVersionUID = 1L;
    private String controllerId;
    private String targetAdress;

    /**
     * Default constructor.
     */
    public TargetPollEvent() {
        // for serialization libs like jackson
    }

    public TargetPollEvent(final Target target, final String applicationId) {
        super(target.getControllerId(), target.getTenant(), applicationId);
        this.controllerId = target.getControllerId();
        this.targetAdress = target.getTargetInfo().getAddress().toString();
    }

    public String getControllerId() {
        return controllerId;
    }

    /**
     * @return the targetAdress
     */
    public String getTargetAdress() {
        return targetAdress;
    }
}
