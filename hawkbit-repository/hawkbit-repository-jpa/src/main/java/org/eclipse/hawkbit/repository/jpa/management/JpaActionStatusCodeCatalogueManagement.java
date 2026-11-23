/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.ActionStatusCodeCatalogueManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatusCode;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusCodeRepository;
import org.eclipse.hawkbit.repository.qfields.ActionStatusCodeFields;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link ActionStatusCodeCatalogueManagement}.
 */
@Transactional(readOnly = true)
@Validated
@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "action-status-code-catalogue-management" }, matchIfMissing = true)
public class JpaActionStatusCodeCatalogueManagement
        extends AbstractJpaRepositoryManagement
        <JpaActionStatusCode, ActionStatusCodeCatalogueManagement.Create, ActionStatusCodeCatalogueManagement.Update,
                ActionStatusCodeRepository, ActionStatusCodeFields>
        implements ActionStatusCodeCatalogueManagement<JpaActionStatusCode> {

    protected JpaActionStatusCodeCatalogueManagement(
            final ActionStatusCodeRepository actionStatusCodeRepository,
            final EntityManager entityManager) {
        super(actionStatusCodeRepository, entityManager);
    }

    @Override
    public Page<Integer> findUndocumentedCodes(final Pageable pageable) {
        // Ignore the incoming sort and build an unsorted page request
        final Pageable unsortedPageable = pageable instanceof OffsetBasedPageRequest
                ? new OffsetBasedPageRequest(pageable.getOffset(), pageable.getPageSize())
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return jpaRepository.findUndocumentedCodes(unsortedPageable);
    }
}
