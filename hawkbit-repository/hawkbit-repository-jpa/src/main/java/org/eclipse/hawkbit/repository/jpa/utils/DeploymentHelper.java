/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.utils;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.repository.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.repository.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.specifications.ActionSpecifications;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Utility class for deployment related topics.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DeploymentHelper {

    /**
     * This method is called, when cancellation has been successful. It sets the
     * action to canceled, resets the meta data of the target and in case there
     * is a new action this action is triggered.
     *
     * @param action the action which is set to canceled
     * @param actionRepository for the operation
     * @param targetRepository for the operation
     */
    public static void successCancellation(final JpaAction action, final ActionRepository actionRepository,
            final TargetRepository targetRepository) {

        // set action inactive
        action.setActive(false);
        action.setStatus(Status.CANCELED);

        final JpaTarget target = action.getTarget();
        final List<Action> nextActiveActions = actionRepository
                .findAll(ActionSpecifications.byTargetIdAndIsActive(target.getId()), Sort.by(Sort.Order.asc(AbstractJpaBaseEntity_.ID)))
                .stream()
                .filter(a -> !a.getId().equals(action.getId()))
                .map(Action.class::cast)
                .toList();

        if (nextActiveActions.isEmpty()) {
            target.setAssignedDistributionSet(target.getInstalledDistributionSet());
            target.setUpdateStatus(TargetUpdateStatus.IN_SYNC);
        } else {
            target.setAssignedDistributionSet((JpaDistributionSet) nextActiveActions.get(0).getDistributionSet());
        }

        targetRepository.save(target);
    }

    /**
     * Executes the modifying action in new transaction
     *
     * @param txManager transaction manager interface
     * @param transactionName the name of the new transaction
     * @param action the callback to execute in new tranaction
     * @return the result of the action
     */
    public static <T> T runInNewTransaction(@NotNull final PlatformTransactionManager txManager,
            final String transactionName, @NotNull final TransactionCallback<T> action) {
        return runInNewTransaction(txManager, transactionName, Isolation.DEFAULT.value(), action);
    }

    /**
     * Executes the modifying action in new transaction
     *
     * @param txManager transaction manager interface
     * @param transactionName the name of the new transaction
     * @param isolationLevel isolation level of the new transaction
     * @param action the callback to execute in new tranaction
     * @return the result of the action
     */
    public static <T> T runInNewTransaction(@NotNull final PlatformTransactionManager txManager,
            final String transactionName, final int isolationLevel, @NotNull final TransactionCallback<T> action) {
        final DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName(transactionName);
        def.setReadOnly(false);
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        def.setIsolationLevel(isolationLevel);
        return new TransactionTemplate(txManager, def).execute(action);
    }
}