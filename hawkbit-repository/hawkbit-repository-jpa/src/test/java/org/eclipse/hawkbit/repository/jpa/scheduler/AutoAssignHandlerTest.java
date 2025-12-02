/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.scheduler;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;

import org.assertj.core.api.Assertions;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.SliceImpl;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Feature: Unit Tests - Repository<br/>
 * Story: Auto assign checker
 */
@ExtendWith(MockitoExtension.class)
class AutoAssignHandlerTest {

    @Mock
    private TargetFilterQueryManagement<? extends TargetFilterQuery> targetFilterQueryManagement;
    @Mock
    private TargetManagement<? extends Target> targetManagement;
    @Mock
    private DeploymentManagement deploymentManagement;
    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    LockRegistry lockRegistry;

    private JpaAutoAssignHandler autoAssignHandler;

    @BeforeEach
    void before() {
        autoAssignHandler = new JpaAutoAssignHandler(
                targetFilterQueryManagement, targetManagement, deploymentManagement, transactionManager, lockRegistry, Optional.empty());
    }

    /**
     * Single device check triggers update for matching auto assignment filter.
     */
    @Test
    void failToLockInHandleAll() {
        final Lock lock = mock(Lock.class);
        when(lock.tryLock()).thenReturn(false);
        when(lockRegistry.obtain(any())).thenReturn(lock);
        final TargetFilterQuery matching = mock(TargetFilterQuery.class);
        when(targetFilterQueryManagement.findWithAutoAssignDS(any())).thenReturn(new SliceImpl<>(Arrays.asList(matching)));

        assertThatNoException().isThrownBy(autoAssignHandler::handleAll);
    }

    /**
     * Single device check triggers update for matching auto assignment filter.
     */
    @Test
    void handleSingleTarget() {
        final String target = getRandomString();
        final long ds = getRandomLong();
        final TargetFilterQuery matching = mockFilterQuery(ds);
        final TargetFilterQuery notMatching = mockFilterQuery(ds);
        when(targetFilterQueryManagement.findWithAutoAssignDS(any())).thenReturn(new SliceImpl<>(Arrays.asList(notMatching, matching)));
        when(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(target, ds, matching.getQuery())).thenReturn(true);
        when(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatibleAndUpdatable(target, ds, notMatching.getQuery()))
                .thenReturn(false);

        autoAssignHandler.handleSingleTarget(target);

        verify(deploymentManagement).assignDistributionSets(Mockito.argThat(deployReqMatcher(target, ds)), any());
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
}