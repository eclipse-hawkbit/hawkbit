/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.builder.AbstractActionStatusCreate;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;

/**
 * Create/build implementation.
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
        if (code != null) {
            result.setCode(code);
        }
        return result;
    }
}
