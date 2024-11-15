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

import org.springframework.context.ApplicationEvent;

/**
 * ApplicationEventFilter for hawkBit internal {@link ApplicationEvent}
 * publishing.
 */
@FunctionalInterface
public interface ApplicationEventFilter {

    /**
     * @param event to verify
     * @return true if event should be filtered
     */
    boolean filter(final ApplicationEvent event);
}