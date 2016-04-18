/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.customrenderers.client.renderers;

import java.io.Serializable;

/**
 * RendererData class with Name and Status.
 * 
 */

public class RendererData implements Serializable {
	private static final long serialVersionUID = -5018181529953620263L;

	private String name;

	private String status;

	/**
	 * Initialize the RendererData.
	 */
	public RendererData() {

	}

	/**
	 * Initialize the RendererData.
	 * 
	 * @param name
	 *            Name of the Rollout.
	 * @param status
	 *            Status of Rollout.
	 */
	public RendererData(String name, String status) {
		super();
		this.name = name;
		this.status = status;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
