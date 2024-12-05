/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.builder;

import jakarta.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Data;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;

/**
 * Builder to create a new dynamic rollout group secret
 */
@Data
@Builder
public class DynamicRolloutGroupTemplate {

    /**
     * The name suffix, by default "" is used.
     */
    @NotNull
    private String nameSuffix = "";

    /**
     * The count of matching Targets that should be assigned to this Group
     */
    private long targetCount;

    /**
     * The group conditions
     */
    private RolloutGroupConditions conditions;

    /**
     * If confirmation is required for this rollout group (considered with confirmation flow active)
     */
    private boolean confirmationRequired;
}