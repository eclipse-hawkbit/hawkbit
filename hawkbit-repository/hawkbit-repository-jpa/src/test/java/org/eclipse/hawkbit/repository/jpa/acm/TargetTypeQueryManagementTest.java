/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.acm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.hawkbit.auth.SpPermission.READ_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.auth.SpPermission.READ_TARGET;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_TARGET;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;

import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.junit.jupiter.api.Test;

/**
 * Note: Still all test gets READ_REPOSITORY since find methods are inherited with request for READ_REPOSITORY. However,
 * using READ_DISTRIBUTION_SET scoping - the scopes still work.
 * <p/>
 * Feature: Component Tests - Access Control<br/>
 * Story: Test Distribution Set Access Controller
 */
class TargetTypeQueryManagementTest extends AbstractAccessControllerManagementTest {

    @Test
    void verifyAutoAssignmentRestrictionByDs() {
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                .create(TargetFilterQueryManagement.Create.builder().name("test").query("id==*").build());

        runAs(withAuthorities(
                READ_DISTRIBUTION_SET + "/type.id==" + dsType1.getId() + " or id==" + ds2Type2.getId(),
                UPDATE_DISTRIBUTION_SET + "/type.id==" + dsType1.getId(),
                // read / update target needed to update target filter query
                READ_TARGET, UPDATE_TARGET), () -> {
                    assertThat(targetFilterQueryManagement
                            .updateAutoAssignDS(new AutoAssignDistributionSetUpdate(targetFilterQuery.getId()).ds(ds1Type1.getId())
                                    .actionType(Action.ActionType.FORCED).confirmationRequired(false))
                            .getAutoAssignDistributionSet().getId()).isEqualTo(ds1Type1.getId());
                    targetFilterQueryManagement
                            .updateAutoAssignDS(new AutoAssignDistributionSetUpdate(targetFilterQuery.getId())
                                    .ds(ds2Type2.getId()).actionType(Action.ActionType.FORCED).confirmationRequired(false))
                            .getAutoAssignDistributionSet().getId();
                    final AutoAssignDistributionSetUpdate autoAssignDistributionSetUpdate = new AutoAssignDistributionSetUpdate(
                            targetFilterQuery.getId())
                            .ds(ds3Type2.getId()).actionType(Action.ActionType.FORCED).confirmationRequired(false);
                    assertThatThrownBy(() -> targetFilterQueryManagement.updateAutoAssignDS(autoAssignDistributionSetUpdate))
                            .isInstanceOf(EntityNotFoundException.class);
                });
    }
}