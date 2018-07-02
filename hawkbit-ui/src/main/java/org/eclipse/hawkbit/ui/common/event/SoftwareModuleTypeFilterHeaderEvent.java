/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.event;

/**
 * Event which is thrown when menubar item in the filter header is chosen or
 * modify modus is closed.
 *
 */
public class SoftwareModuleTypeFilterHeaderEvent extends FilterHeaderEvent {

    /**
     * Constructor
     * 
     * @param filterHeaderEnum
     *            FilterHeaderEnum
     */
    public SoftwareModuleTypeFilterHeaderEvent(final FilterHeaderEnum filterHeaderEnum) {
        super(filterHeaderEnum);
    }

}
