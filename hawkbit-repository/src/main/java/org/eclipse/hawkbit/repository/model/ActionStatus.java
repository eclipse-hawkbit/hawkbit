/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;

import org.eclipse.hawkbit.repository.model.Action.Status;

public interface ActionStatus extends TenantAwareBaseEntity {

    Long getOccurredAt();

    void setOccurredAt(Long occurredAt);

    /**
     * Adds message including splitting in case it exceeds 512 length.
     *
     * @param message
     *            to add
     */
    void addMessage(String message);

    List<String> getMessages();

    Action getAction();

    void setAction(Action action);

    Status getStatus();

    void setStatus(Status status);

}