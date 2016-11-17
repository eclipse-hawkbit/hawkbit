/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.eclipse.hawkbit.repository.model.Action.Status;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public abstract class AbstractActionStatusCreate<T> {
    protected Status status;
    protected Long occurredAt;
    protected Collection<String> messages;
    protected Long actionId;

    public Long getActionId() {
        return actionId;
    }

    public T status(final Status status) {
        this.status = status;

        return (T) this;
    }

    public T occurredAt(final Long occurredAt) {
        this.occurredAt = occurredAt;

        return (T) this;
    }

    public T messages(final Collection<String> messages) {
        if (this.messages == null) {
            this.messages = messages;
        } else {
            this.messages.addAll(messages);
        }

        return (T) this;
    }

    public T message(final String message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(message);

        return (T) this;
    }

    public Optional<Long> getOccurredAt() {
        return Optional.ofNullable(occurredAt);
    }

}
