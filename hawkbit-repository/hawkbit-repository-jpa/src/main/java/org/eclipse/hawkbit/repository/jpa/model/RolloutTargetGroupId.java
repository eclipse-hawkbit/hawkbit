/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serializable;

import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;

/**
 * Combined unique key of the table {@link RolloutTargetGroup}.
 *
 */
public class RolloutTargetGroupId implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long rolloutGroup;
    private Long target;

    /**
     * default constructor necessary for JPA.
     */
    public RolloutTargetGroupId() {
        // default constructor necessary for JPA, empty.
    }

    /**
     * Constructor.
     * 
     * @param rolloutGroup
     *            the rollout group for this key
     * @param target
     *            the target for this key
     */
    public RolloutTargetGroupId(final RolloutGroup rolloutGroup, final Target target) {
        this.rolloutGroup = rolloutGroup.getId();
        this.target = target.getId();
    }

    public Long getRolloutGroup() {
        return rolloutGroup;
    }

    public Long getTarget() {
        return target;
    }
}
