/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import org.eclipse.persistence.descriptors.DescriptorEvent;

/**
 * 
 * Holds details of action(Create/Update) and @link{DescriptorEvent}.
 *
 */
public class DescriptorEventDetails {

	enum ActionType {
		CREATE, UPDATE;
	}

	private DescriptorEvent descriptorEvent;

	private ActionType actiontype;

	public DescriptorEventDetails(ActionType actionType,
			DescriptorEvent descriptorEvent) {
		this.descriptorEvent = descriptorEvent;
		this.actiontype = actionType;
	}

	public DescriptorEvent getDescriptorEvent() {
		return descriptorEvent;
	}

	public ActionType getActiontype() {
		return actiontype;
	}

}
