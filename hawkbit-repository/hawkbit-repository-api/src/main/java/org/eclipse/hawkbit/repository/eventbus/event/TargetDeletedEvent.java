/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event;

import org.eclipse.hawkbit.eventbus.event.AbstractDistributedEvent;
import org.eclipse.hawkbit.repository.model.Target;

/**
 *
 * Defines the {@link AbstractBaseEntityEvent} of deleting a {@link Target}.
 */
public class TargetDeletedEvent extends AbstractDistributedEvent {

	private static final long serialVersionUID = 1L;
	private final long targetId;

	/**
	 * @param tenant
	 *            the tenant for this event
	 * @param targetId
	 *            the ID of the target which has been deleted
	 */
	public TargetDeletedEvent(final String tenant, final long targetId) {
		super(-1, tenant);
		this.targetId = targetId;
	}

	/**
	 * @return the targetId
	 */
	public long getTargetId() {
		return targetId;
	}

}
