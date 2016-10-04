/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event;

import org.eclipse.hawkbit.repository.model.DistributionSetTag;

/**
 * Defines the {@link AbstractBaseEntityEvent} for creation of a new
 * {@link DistributionSetTag}.
 *
 */
public class DistributionSetTagCreatedEvent extends AbstractBaseEntityEvent<DistributionSetTag> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param tag
     *            the tag which is updated
     */
    public DistributionSetTagCreatedEvent(final DistributionSetTag tag) {
        super(tag);
    }
}
