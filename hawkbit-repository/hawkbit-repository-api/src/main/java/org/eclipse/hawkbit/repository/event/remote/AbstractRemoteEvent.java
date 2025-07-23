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

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS, // full class name in JSON
        property = "@class" // standard property name
)
@Getter
public abstract class AbstractRemoteEvent extends ApplicationEvent {

    private final String id;

    protected AbstractRemoteEvent() {
        this("_empty_default_");
    }

    protected AbstractRemoteEvent(Object source) {
        super(source);
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final AbstractRemoteEvent that = (AbstractRemoteEvent) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
