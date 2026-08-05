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
import static org.eclipse.hawkbit.auth.SpPermission.CREATE_AUTO_ASSIGNMENT;
import static org.eclipse.hawkbit.auth.SpPermission.READ_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.auth.SpPermission.READ_TARGET;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_TARGET;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;

import org.eclipse.hawkbit.repository.AutoAssignmentManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action;
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
        runAs(withAuthorities(
                READ_DISTRIBUTION_SET + "/type.id==" + dsType1.getId() + " or id==" + ds2Type2.getId(),
                UPDATE_DISTRIBUTION_SET + "/type.id==" + dsType1.getId(),
                // create auto assignment permission and read / update target needed to create an auto assignment
                CREATE_AUTO_ASSIGNMENT, READ_TARGET, UPDATE_TARGET), () -> {
                    // a readable and updatable distribution set can be auto assigned
                    assertThat(autoAssignmentManagement
                            .create(AutoAssignmentManagement.Create.builder().name("aa1").targetFilterQuery("id==*")
                                    .distributionSet(distributionSetManagement.get(ds1Type1.getId()))
                                    .actionType(Action.ActionType.FORCED).confirmationRequired(false).build())
                            .getDistributionSet().getId()).isEqualTo(ds1Type1.getId());
                    // a readable distribution set can be auto assigned
                    autoAssignmentManagement.create(AutoAssignmentManagement.Create.builder().name("aa2").targetFilterQuery("id==*")
                            .distributionSet(distributionSetManagement.get(ds2Type2.getId()))
                            .actionType(Action.ActionType.FORCED).confirmationRequired(false).build());
                    // a not readable distribution set cannot be auto assigned - ds3Type2 was created as system in
                    // setup, so create(...) is the single invocation expected to throw under the restricted rights
                    final AutoAssignmentManagement.Create aa3 = AutoAssignmentManagement.Create.builder()
                            .name("aa3").targetFilterQuery("id==*").distributionSet(ds3Type2)
                            .actionType(Action.ActionType.FORCED).confirmationRequired(false).build();
                    assertThatThrownBy(() -> autoAssignmentManagement.create(aa3))
                            .isInstanceOf(EntityNotFoundException.class);
                });
    }
}