/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event;

import static com.google.common.base.Preconditions.checkNotNull;

public class CacheClearedEvent extends AbstractDistributedEvent {

    private static final long serialVersionUID = 1L;

    private static final int NO_REVISION = -1;

    private final String cacheName;

    /**
     * Constructor.
     *
     * @param tanent
     *            the tenant for this event
     * @param cacheName
     *            the name of the cache which was cleared
     */
    public CacheClearedEvent(final String tanent, final String cacheName) {
        super(NO_REVISION, tanent);
        this.cacheName = checkNotNull(cacheName);
    }

    public String getCacheName() {
        return cacheName;
    }

    @Override
    public String getTenant() {
        // TODO Auto-generated method stub
        return null;
    }
}
