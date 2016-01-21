/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.eventbus.event;

import java.io.Serializable;

/**
 * Marks an event to as an distributed event which will be distributed to other
 * nodes.
 *
 *
 *
 *
 */
public interface DistributedEvent extends Event, NodeAware, Serializable {

}
