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

import org.eclipse.hawkbit.repository.builder.RolloutGroupBuilder;
import org.eclipse.hawkbit.repository.builder.RolloutGroupCreate;
import org.eclipse.hawkbit.repository.model.RolloutGroup;

/**
 * Builder implementation for {@link RolloutGroup}.
 */
public class JpaRolloutGroupBuilder implements RolloutGroupBuilder {

    @Override
    public RolloutGroupCreate create() {
        return new JpaRolloutGroupCreate();
    }
}