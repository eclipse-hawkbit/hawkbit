/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event;

import org.eclipse.hawkbit.repository.model.Target;

/**
 * Defines the {@link AbstractBaseEntityEvent} of updating a {@link Target}.
 *
 */
public class TargetUpdatedEvent extends AbstractBaseEntityEvent<Target> {

	private static final long serialVersionUID = 5665118668865832477L;

	public TargetUpdatedEvent(Target baseEntity) {
		super(baseEntity);
	}

}
