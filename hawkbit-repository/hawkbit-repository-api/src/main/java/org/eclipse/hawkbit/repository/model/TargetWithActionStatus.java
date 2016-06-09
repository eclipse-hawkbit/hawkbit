/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import org.eclipse.hawkbit.repository.model.Action.Status;

/**
 * 
 * Target with action status.
 *
 */
public class TargetWithActionStatus {

    private Target target;

    private Status status;

    public TargetWithActionStatus(final Target target) {
        this.target = target;
    }

    public TargetWithActionStatus(final Target target, final Status status) {
        this.status = status;
        this.target = target;
    }

    public Target getTarget() {
        return target;
    }

    public Status getStatus() {
        return status;
    }

    public void setTarget(final Target target) {
        this.target = target;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

}
