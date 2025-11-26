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
import static org.eclipse.hawkbit.auth.SpPermission.CREATE_PREFIX;
import static org.eclipse.hawkbit.auth.SpPermission.DELETE_PREFIX;
import static org.eclipse.hawkbit.auth.SpPermission.DELETE_TARGET;
import static org.eclipse.hawkbit.auth.SpPermission.DISTRIBUTION_SET;
import static org.eclipse.hawkbit.auth.SpPermission.DISTRIBUTION_SET_TYPE;
import static org.eclipse.hawkbit.auth.SpPermission.READ_DISTRIBUTION_SET;
import static org.eclipse.hawkbit.auth.SpPermission.READ_PREFIX;
import static org.eclipse.hawkbit.auth.SpPermission.READ_TARGET;
import static org.eclipse.hawkbit.auth.SpPermission.SOFTWARE_MODULE;
import static org.eclipse.hawkbit.auth.SpPermission.SOFTWARE_MODULE_TYPE;
import static org.eclipse.hawkbit.auth.SpPermission.TARGET_TYPE;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_PREFIX;
import static org.eclipse.hawkbit.auth.SpPermission.UPDATE_TARGET;
import static org.eclipse.hawkbit.repository.test.util.SecurityContextSwitch.runAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.eclipse.hawkbit.context.AccessContext;
import org.eclipse.hawkbit.repository.jpa.acm.AccessController.Operation;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

class SystemExecutionTest extends AbstractAccessControllerManagementTest {

    @Autowired
    protected AccessController<JpaSoftwareModule> softwareModuleAccessController;
    @Autowired
    protected AccessController<JpaSoftwareModuleType> softwareModuleTypeAccessController;
    @Autowired
    protected AccessController<JpaDistributionSet> distributionSetAccessController;
    @Autowired
    protected AccessController<JpaDistributionSetType> distributionSetTypeAccessController;
    @Autowired
    protected AccessController<JpaTarget> targetAccessController;
    @Autowired
    protected AccessController<JpaTargetType> targetTypeAccessController;
    @Autowired
    protected AccessController<JpaAction> actionAccessController;

    @Autowired
    List<AccessController<?>> allAccessControllers;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    void verifyWithoutScopes() {
        allAccessControllers.forEach(accessController -> {
            for (final Operation operation : Operation.values()) {
                assertThat(accessController.getAccessRules(operation)).isEmpty();
            }
            final Specification specificationMock = mock(Specification.class);
            for (final Operation operation : Operation.values()) {
                accessController.appendAccessRules(operation, specificationMock);
                verifyNoInteractions(specificationMock);
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    void verifyWithScope() {
        runAs(withAuthorities(READ_PREFIX + SOFTWARE_MODULE + "/type.id==1", UPDATE_PREFIX + SOFTWARE_MODULE),
                () -> verifyAccessController(softwareModuleAccessController));
        runAs(withAuthorities(READ_PREFIX + SOFTWARE_MODULE_TYPE + "/id==1", UPDATE_PREFIX + SOFTWARE_MODULE_TYPE),
                () -> verifyAccessController(softwareModuleTypeAccessController));
        runAs(withAuthorities(READ_DISTRIBUTION_SET + "/type.id==1", CREATE_PREFIX + DISTRIBUTION_SET),
                () -> verifyAccessController(distributionSetAccessController));
        runAs(withAuthorities(READ_PREFIX + DISTRIBUTION_SET_TYPE + "/id==1", DELETE_PREFIX + DISTRIBUTION_SET_TYPE),
                () -> verifyAccessController(distributionSetTypeAccessController));
        runAs(withAuthorities(READ_TARGET, DELETE_TARGET + "/type.id==1"),
                () -> verifyAccessController(targetAccessController));
        runAs(withAuthorities(CREATE_PREFIX + TARGET_TYPE + "/id==1"), () -> verifyAccessController(targetTypeAccessController));
        // Action Access Controller maps CREATE/UPDATE/DELETE to UPDATE - so only UPDATE (or READ scope) is relevant
        // and update will be called for every of the mapped - so 3 times
        runAs(withAuthorities(UPDATE_TARGET + "/type.id==1"), () -> verifyAccessController(actionAccessController, 3));
    }

    private void verifyAccessController(final AccessController<?> accessController) {
        verifyAccessController(accessController, 1);
    }
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void verifyAccessController(final AccessController<?> accessController, final int times) {
        final Specification mock = mock(Specification.class);
        for (final Operation operation : Operation.values()) {
            accessController.appendAccessRules(operation, mock);
        }
        verify(mock, times(times)).and(any()); // once for every access controller is scoped only

        final Specification mockAsSystem = mock(Specification.class);
        for (Operation operation : Operation.values()) {
            AccessContext.asSystem(() -> accessController.appendAccessRules(operation, mockAsSystem));
        }
        verifyNoInteractions(mockAsSystem);
    }
}