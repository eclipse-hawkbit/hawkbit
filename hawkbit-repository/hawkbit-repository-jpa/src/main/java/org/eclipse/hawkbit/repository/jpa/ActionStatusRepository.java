/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ActionStatus} repository.
 *
 */
@Transactional(readOnly = true)
public interface ActionStatusRepository
        extends BaseEntityRepository<JpaActionStatus, Long>, JpaSpecificationExecutor<JpaActionStatus> {

    /**
     * Counts {@link ActionStatus} entries of given {@link Action} in
     * repository.
     *
     * @param action
     *            to count status entries
     * @return number of actions in repository
     */
    Long countByAction(JpaAction action);

    /**
     * Retrieves all {@link ActionStatus} entries from repository of given
     * ActionId.
     *
     * @param pageReq
     *            parameters
     * @param actionId
     *            of the status entries
     * @return pages list of {@link ActionStatus} entries
     */
    Page<ActionStatus> findByActionId(Pageable pageReq, Long actionId);

    /**
     * Finds all status updates for the defined action and target including
     * {@link ActionStatus#getMessages()}.
     *
     * @param pageReq
     *            for page configuration
     * @param target
     *            to look for
     * @param actionId
     *            to look for
     * @return Page with found targets
     */
    @EntityGraph(value = "ActionStatus.withMessages", type = EntityGraphType.LOAD)
    Page<ActionStatus> getByActionId(Pageable pageReq, Long actionId);

    /**
     * Finds a filtered list of status messages for an action.
     *
     * @param pageable
     *            for page configuration
     * @param actionId
     *            for which to get the status messages
     * @param filter
     *            is the SQL like pattern to use for filtering out or excluding
     *            the messages
     *
     * @return Page with found status messages.
     */
    @Query("SELECT message FROM JpaActionStatus actionstatus JOIN actionstatus.messages message WHERE actionstatus.action.id = :actionId AND message NOT LIKE :filter")
    Page<String> findMessagesByActionIdAndMessageNotLike(final Pageable pageable, @Param("actionId") Long actionId,
            @Param("filter") String filter);
}
