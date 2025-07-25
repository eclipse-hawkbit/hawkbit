package org.eclipse.hawkbit.repository.event.remote;

import lombok.Getter;

@Getter
public abstract class AbstractGroupedRemoteEvent<T extends AbstractRemoteEvent> extends AbstractRemoteEvent {

    private final T remoteEvent;

    public AbstractGroupedRemoteEvent(T remoteEvent) {
        super(remoteEvent.getSource());
        this.remoteEvent = remoteEvent;
    }

}
