/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.decorators;

import com.vaadin.server.Resource;
import com.vaadin.ui.Button;

/**
 * Interface to define method for button decoration.
 */
@FunctionalInterface
public interface SPUIButtonDecorator {

    /**
     * Decorate Button.
     * 
     * @param button
     *            as Button
     * @param style
     *            as String
     * @param setStyle
     *            as String
     * @param icon
     *            as resource
     * @return Button
     */
    Button decorate(Button button, String style, boolean setStyle, Resource icon);

}
