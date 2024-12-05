/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.Getter;
import org.eclipse.hawkbit.repository.ValidString;
import org.eclipse.hawkbit.repository.model.Action.Status;

/**
 * Create and update builder DTO.
 *
 * @param <T> update or create builder interface
 */
public abstract class AbstractActionStatusCreate<T> {

    protected Status status;
    protected Long occurredAt;
    protected Integer code;
    protected List<@ValidString String> messages;
    @Getter
    protected Long actionId;

    public T status(final Status status) {
        this.status = status;
        return (T) this;
    }

    public T occurredAt(final long occurredAt) {
        this.occurredAt = occurredAt;
        return (T) this;
    }

    public T code(final int code) {
        this.code = code;
        return (T) this;
    }

    public T messages(final Collection<String> messages) {
        if (this.messages == null) {
            // create modifiable list
            this.messages = messages.stream().filter(Objects::nonNull).map(String::strip).collect(Collectors.toCollection(ArrayList::new));
        } else {
            this.messages.addAll(messages.stream().filter(Objects::nonNull).map(String::strip).toList());
        }
        return (T) this;
    }

    public T message(final String message) {
        if (message != null) {
            if (this.messages == null) {
                this.messages = new ArrayList<>();
            }
            this.messages.add(message.strip());
        }
        return (T) this;
    }

    public Optional<Long> getOccurredAt() {
        return Optional.ofNullable(occurredAt);
    }
}