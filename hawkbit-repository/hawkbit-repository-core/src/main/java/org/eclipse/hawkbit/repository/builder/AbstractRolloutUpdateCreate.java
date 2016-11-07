/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.Action.ActionType;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public abstract class AbstractRolloutUpdateCreate<T> extends AbstractNamedEntityBuilder<T> {
    protected Long set;
    protected String targetFilterQuery;
    protected ActionType actionType;
    protected Long forcedTime;

    public T set(final Long set) {
        this.set = set;
        return (T) this;
    }

    public T targetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery;
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

}
