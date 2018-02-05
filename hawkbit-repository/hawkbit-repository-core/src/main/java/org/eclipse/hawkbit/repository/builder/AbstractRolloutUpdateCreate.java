/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Optional;

import org.eclipse.hawkbit.repository.ValidString;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.springframework.util.StringUtils;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public abstract class AbstractRolloutUpdateCreate<T> extends AbstractNamedEntityBuilder<T> {
    protected Long set;

    @ValidString
    protected String targetFilterQuery;

    protected ActionType actionType;
    protected Long forcedTime;
    protected Long startAt;

    public T set(final long set) {
        this.set = set;
        return (T) this;
    }

    public T targetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = StringUtils.trimWhitespace(targetFilterQuery);
        return (T) this;
    }

    public T actionType(final ActionType actionType) {
        this.actionType = actionType;
        return (T) this;
    }

    public T forcedTime(final Long forcedTime) {
        this.forcedTime = forcedTime;
        return (T) this;
    }

    public T startAt(final Long startAt) {
        this.startAt = startAt;
        return (T) this;
    }

    public Optional<Long> getSet() {
        return Optional.ofNullable(set);
    }

    public Optional<ActionType> getActionType() {
        return Optional.ofNullable(actionType);
    }

    public Optional<Long> getForcedTime() {
        return Optional.ofNullable(forcedTime);
    }

    public Optional<Long> getStartAt() {
        return Optional.ofNullable(startAt);
    }
}
