/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.ActionStatusBuilder;
import org.eclipse.hawkbit.repository.builder.RolloutBuilder;
import org.eclipse.hawkbit.repository.builder.RolloutGroupBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaActionStatusBuilder;
import org.eclipse.hawkbit.repository.jpa.builder.JpaRolloutGroupBuilder;
import org.springframework.validation.annotation.Validated;

/**
 * JPA Implementation of {@link EntityFactory}.
 */
@Validated
public class JpaEntityFactory implements EntityFactory {

    private final RolloutBuilder rolloutBuilder;

    @SuppressWarnings("java:S107")
    public JpaEntityFactory(final RolloutBuilder rolloutBuilder) {
        this.rolloutBuilder = rolloutBuilder;
    }

    @Override
    public ActionStatusBuilder actionStatus() {
        return new JpaActionStatusBuilder();
    }

    @Override
    public RolloutGroupBuilder rolloutGroup() {
        return new JpaRolloutGroupBuilder();
    }

    @Override
    public RolloutBuilder rollout() {
        return rolloutBuilder;
    }
}