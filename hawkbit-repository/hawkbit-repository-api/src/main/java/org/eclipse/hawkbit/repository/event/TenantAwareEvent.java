/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event;

/**
 * An event declaration which holds a revision for each event so consumers have
 * the chance to know if they might already have been retrieved a newer event.
 */
@FunctionalInterface
public interface TenantAwareEvent {

    /**
     * @return the tenant of the event.
     */
    String getTenant();
}