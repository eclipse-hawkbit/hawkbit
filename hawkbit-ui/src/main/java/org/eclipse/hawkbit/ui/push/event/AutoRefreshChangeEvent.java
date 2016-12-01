/**
 * Copyright (c) 2011-2016 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui.push.event;

/**
 *
 */
public class AutoRefreshChangeEvent {

    private final Boolean autoRefreshOn;

    /**
     * @param value
     */
    public AutoRefreshChangeEvent(final Boolean autoEventOn) {
        this.autoRefreshOn = autoEventOn;
    }

    public Boolean getAutoRefreshOn() {
        return autoRefreshOn;
    }

}
