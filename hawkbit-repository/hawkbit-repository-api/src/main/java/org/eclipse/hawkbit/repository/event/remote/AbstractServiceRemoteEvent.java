/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.event.remote;

import lombok.Getter;

@Getter
public abstract class AbstractServiceRemoteEvent<T extends AbstractRemoteEvent> extends AbstractRemoteEvent {

    private final T remoteEvent;

    protected AbstractServiceRemoteEvent(T remoteEvent) {
        super(remoteEvent == null ? "_empty_source_" : remoteEvent.getSource());
        this.remoteEvent = remoteEvent;
    }

}
