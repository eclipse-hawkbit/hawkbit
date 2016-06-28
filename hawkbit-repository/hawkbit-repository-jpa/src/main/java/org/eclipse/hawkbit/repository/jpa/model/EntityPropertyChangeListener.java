/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import org.eclipse.hawkbit.repository.jpa.model.DescriptorEventDetails.ActionType;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.descriptors.DescriptorEventAdapter;

/**
 * Listens to change in property values of an entity.
 *
 */
public class EntityPropertyChangeListener extends DescriptorEventAdapter {

	@Override
	public void postInsert(final DescriptorEvent event) {
		AbstractDescriptorEventVisitor visitor = new AbstractDescriptorEventVisitorImpl();
		((AbstractJpaBaseEntity) event.getObject()).postActionOnEntity(visitor,
				new DescriptorEventDetails(ActionType.CREATE, event));

	}

	@Override
	public void postUpdate(final DescriptorEvent event) {
		AbstractDescriptorEventVisitor visitor = new AbstractDescriptorEventVisitorImpl();	
		((AbstractJpaBaseEntity) event.getObject()).postActionOnEntity(visitor,
				new DescriptorEventDetails(ActionType.UPDATE, event));
	}
	
}
