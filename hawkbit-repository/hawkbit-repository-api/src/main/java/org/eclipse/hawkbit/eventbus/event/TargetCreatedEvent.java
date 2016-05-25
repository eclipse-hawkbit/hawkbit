/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

import org.eclipse.hawkbit.repository.model.Target;

/**
 * Defines the {@link AbstractBaseEntityEvent} of creating a new {@link Target}.
 *
 *
 */
public class TargetCreatedEvent extends AbstractBaseEntityEvent<Target> {

    private static final long serialVersionUID = 1L;

    /**
     * @param target
     *            the target which has been created
     */
    public TargetCreatedEvent(final Target target) {
        super(target);
    }
}
