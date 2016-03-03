/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ActionStatus} repository.
 *
 *
 *
 *
 */
@Transactional(readOnly = true)
public interface ActionStatusRepository
        extends BaseEntityRepository<ActionStatus, Long>, JpaSpecificationExecutor<ActionStatus> {

    /**
     * @param target
     * @param action
     * @return
     */
    Long countByAction(Action action);

    /**
     * @param action
     * @param retrieved
     * @return
     */
    Long countByActionAndStatus(Action action, Status retrieved);

    /**
     * @param pageReq
     * @param action
     * @return
     */
    Page<ActionStatus> findByAction(Pageable pageReq, Action action);

    /**
     * Finds all status updates for the defined action and target order by
     * {@link ActionStatus#getId()} desc including
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
    Page<ActionStatus> getByAction(Pageable pageReq, Action action);

}
