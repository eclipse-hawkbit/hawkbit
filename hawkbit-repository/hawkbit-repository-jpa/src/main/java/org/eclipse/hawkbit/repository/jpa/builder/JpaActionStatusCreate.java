/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.builder.AbstractActionStatusCreate;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;

/**
 * Create/build implementation.
 *
 */
public class JpaActionStatusCreate extends AbstractActionStatusCreate<ActionStatusCreate>
        implements ActionStatusCreate {

    JpaActionStatusCreate(final Long actionId) {
        super.actionId = actionId;
    }

    @Override
    public JpaActionStatus build() {
        final JpaActionStatus result = new JpaActionStatus(status, getOccurredAt().orElse(System.currentTimeMillis()));
        if (messages != null) {
            messages.forEach(result::addMessage);
        }
        return result;
    }
}
