package org.eclipse.hawkbit.eventbus.event;

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
}
