/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
