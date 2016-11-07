/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.event;

/**
 * An event declaration which holds an revision for each event so consumers have
 * the chance to know if they might already retrieved a newer event.
 *
 */
@FunctionalInterface
public interface TenantAwareEvent {

    /**
     * @return the tenant of the event.
     */
    String getTenant();
}
