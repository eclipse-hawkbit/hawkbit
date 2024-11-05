/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.autoassign;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.ContextAware;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.SliceImpl;
import org.springframework.transaction.PlatformTransactionManager;

@Feature("Unit Tests - Repository")
@Story("Auto assign checker")
@ExtendWith(MockitoExtension.class)
class AutoAssignCheckerTest {

    @Mock
    private TargetFilterQueryManagement targetFilterQueryManagement;
    @Mock
    private TargetManagement targetManagement;
    @Mock
    private DeploymentManagement deploymentManagement;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private ContextAware contextAware;

    private AutoAssignChecker sut;

    @BeforeEach
    void before() {
        sut = new AutoAssignChecker(targetFilterQueryManagement, targetManagement, deploymentManagement,
                transactionManager, contextAware);
    }

    @Test
    @Description("Single device check triggers update for matching auto assignment filter.")
    void checkForDevice() {
        mockRunningAsNonSystem();
        final String target = getRandomString();
        final long ds = getRandomLong();
        final TargetFilterQuery matching = mockFilterQuery(ds);
        final TargetFilterQuery notMatching = mockFilterQuery(ds);
        when(targetFilterQueryManagement.findWithAutoAssignDS(any()))
                .thenReturn(new SliceImpl<>(Arrays.asList(notMatching, matching)));

        when(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(target, ds, matching.getQuery()))
                .thenReturn(true);
        when(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(target, ds, notMatching.getQuery()))
                .thenReturn(false);

        sut.checkSingleTarget(target);

        verify(deploymentManagement).assignDistributionSets(eq(matching.getAutoAssignInitiatedBy()),
                Mockito.argThat(deployReqMatcher(target, ds)), any());
        Mockito.verifyNoMoreInteractions(deploymentManagement);
    }

    private static TargetFilterQuery mockFilterQuery(final long dsId) {
        final DistributionSet ds = mock(DistributionSet.class);
        when(ds.getId()).thenReturn(dsId);
        final TargetFilterQuery filter = mock(TargetFilterQuery.class);
        when(filter.getId()).thenReturn(getRandomLong());
        when(filter.getQuery()).thenReturn(getRandomString());
        lenient().when(filter.getAutoAssignInitiatedBy()).thenReturn(getRandomString());
        when(filter.getAutoAssignDistributionSet()).thenReturn(ds);
        return filter;
    }

    private static long getRandomLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    private static String getRandomString() {
        return UUID.randomUUID().toString();
    }

    private ArgumentMatcher<List<DeploymentRequest>> deployReqMatcher(final String target, final long ds) {
        return requests -> {
            final DeploymentRequest request = requests.get(0);
            return requests.size() == 1 && request.getDistributionSetId() == ds && request.getControllerId() == target;
        };
    }

    private void mockRunningAsNonSystem() {
        when(contextAware.getCurrentTenant()).thenReturn(getRandomString());
        when(contextAware.runAsTenantAsUser(any(String.class), any(String.class), any(TenantAware.TenantRunner.class)))
                .thenAnswer(i -> ((TenantAware.TenantRunner) i.getArgument(2)).run());
    }

}
