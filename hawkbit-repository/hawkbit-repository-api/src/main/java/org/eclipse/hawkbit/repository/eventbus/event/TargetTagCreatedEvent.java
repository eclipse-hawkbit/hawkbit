/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event;

import org.eclipse.hawkbit.repository.model.TargetTag;

/**
 * Defines the {@link AbstractBaseEntityEvent} for creation of a new
 * {@link TargetTag}.
 *
 */
public class TargetTagCreatedEvent extends AbstractBaseEntityEvent<TargetTag> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * 
     * @param tag
     *            the tag which has been created
     */
    public TargetTagCreatedEvent(final TargetTag tag) {
        super(tag);
    }
}
