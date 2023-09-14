/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.push;

import com.vaadin.ui.UI;

/**
 * Interface declaring a strategy to push events from the back-end to the UI.
 *
 */
public interface EventPushStrategy {

    /**
     * Initialize the event push strategy, this is bound to the life-cycle of
     * the {@link UI} so the strategy can be initialized based a {@link UI}.
     * 
     * @param vaadinUI
     *            the {@link UI}
     */
    void init(UI vaadinUI);

    /**
     * Cleans up resources when the strategy is not be used anymore e.g.
     * {@link UI#detach()}.
     */
    void clean();
}
