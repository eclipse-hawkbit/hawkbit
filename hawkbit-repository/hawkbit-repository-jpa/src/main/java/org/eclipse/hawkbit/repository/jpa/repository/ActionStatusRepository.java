/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.repository;

import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ActionStatus} repository.
 *
 */
@Transactional(readOnly = true)
public interface ActionStatusRepository extends BaseEntityRepository<JpaActionStatus> {

    /**
     * Counts {@link ActionStatus} entries of given {@link Action} in
     * repository.
     * <p/>
     * No access control applied
     *
     * @param actionId of the action to count status entries for
     * @return number of actions in repository
     */
    long countByActionId(Long actionId);

    /**
     * Retrieves all {@link ActionStatus} entries from repository of given
     * ActionId.
     * <p/>
     * No access control applied
     *
     * @param pageReq parameters
     * @param actionId of the status entries
     * @return pages list of {@link ActionStatus} entries
     */
    Page<ActionStatus> findByActionId(Pageable pageReq, Long actionId);

    /**
     * Finds a filtered list of status messages for an action.
     * <p/>
     * No access control applied
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
    Page<String> findMessagesByActionIdAndMessageNotLike(Pageable pageable, @Param("actionId") Long actionId,
            @Param("filter") String filter);
}
