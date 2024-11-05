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

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.builder.AbstractTargetFilterQueryUpdateCreate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.exception.InvalidAutoAssignActionTypeException;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

/**
 * Create/build implementation.
 */
public class JpaTargetFilterQueryCreate extends AbstractTargetFilterQueryUpdateCreate<TargetFilterQueryCreate>
        implements TargetFilterQueryCreate {

    private final DistributionSetManagement distributionSetManagement;

    JpaTargetFilterQueryCreate(final DistributionSetManagement distributionSetManagement) {
        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    public JpaTargetFilterQuery build() {
        return new JpaTargetFilterQuery(name, query,
                getAutoAssignDistributionSetId().map(distributionSetManagement::getValidAndComplete).orElse(null),
                getAutoAssignActionType().filter(JpaTargetFilterQueryCreate::isAutoAssignActionTypeValid).orElse(null),
                weight, getConfirmationRequired().orElse(false));
    }

    private static boolean isAutoAssignActionTypeValid(final ActionType actionType) {
        if (!TargetFilterQuery.ALLOWED_AUTO_ASSIGN_ACTION_TYPES.contains(actionType)) {
            throw new InvalidAutoAssignActionTypeException();
        }

        return true;
    }

}
