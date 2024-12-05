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

import org.eclipse.hawkbit.repository.builder.ActionStatusBuilder;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.model.ActionStatus;

/**
 * Builder implementation for {@link ActionStatus}.
 */
public class JpaActionStatusBuilder implements ActionStatusBuilder {

    @Override
    public ActionStatusCreate create(final long actionId) {
        return new JpaActionStatusCreate(actionId);
    }
}