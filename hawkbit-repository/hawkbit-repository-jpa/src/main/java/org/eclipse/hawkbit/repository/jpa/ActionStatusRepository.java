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
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ActionStatus} repository.
 *
 */
@Transactional(readOnly = true, isolation = Isolation.READ_UNCOMMITTED)
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
     * Counts {@link ActionStatus} entries of given {@link Action} with given
     * {@link Status} in repository.
     * 
     * @param action
     *            to count status entries
     * @param status
     *            to filter for
     * @return number of actions in repository
     */
    Long countByActionAndStatus(JpaAction action, Status status);

    /**
     * Retrieves all {@link ActionStatus} entries from repository of given
     * {@link Action}.
     * 
     * @param pageReq
     *            parameters
     * @param action
     *            of the status entries
     * @return pages list of {@link ActionStatus} entries
     */
    Page<ActionStatus> findByAction(Pageable pageReq, JpaAction action);

    /**
     * Finds all status updates for the defined action and target including
     * {@link ActionStatus#getMessages()}.
     *
     * @param pageReq
     *            for page configuration
     * @param target
     *            to look for
     * @param action
     *            to look for
     * @return Page with found targets
     */
    @EntityGraph(value = "ActionStatus.withMessages", type = EntityGraphType.LOAD)
    Page<ActionStatus> getByAction(Pageable pageReq, JpaAction action);

}
