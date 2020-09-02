/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.push.event;

/**
 * Interface to indicate an entity event which contains parent entity id.
 */
@FunctionalInterface
public interface ParentIdAwareEvent {

    /**
     * @return the ID of the parent entity of this event.
     */
    Long getParentEntityId();
}
