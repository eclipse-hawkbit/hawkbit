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

import java.util.Collections;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.ql.jpa.QLSupport;
import org.eclipse.hawkbit.repository.ActionStatusCodeManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.JpaManagementHelper;
import org.eclipse.hawkbit.repository.jpa.JpaRepositoryConfiguration;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatusCode;
import org.eclipse.hawkbit.repository.jpa.repository.ActionStatusCodeRepository;
import org.eclipse.hawkbit.repository.model.ActionStatusCode;
import org.eclipse.hawkbit.repository.qfields.ActionStatusCodeFields;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link ActionStatusCodeManagement}.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@Validated
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "action-status-code-management" }, matchIfMissing = true)
@HandleAuthorizationDenied(handlerClass = JpaRepositoryConfiguration.ManagementExceptionThrowingMethodAuthorizationDeniedHandler.class)
public class JpaActionStatusCodeManagement implements ActionStatusCodeManagement<ActionStatusCode> {

    private final ActionStatusCodeRepository actionStatusCodeRepository;

    public JpaActionStatusCodeManagement(final ActionStatusCodeRepository actionStatusCodeRepository) {
        this.actionStatusCodeRepository = actionStatusCodeRepository;
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public ActionStatusCode create(final Create create) {
        if (actionStatusCodeRepository.existsByCode(create.getCode())) {
            throw new EntityAlreadyExistsException("Action status code " + create.getCode() + " already exists");
        }

        final JpaActionStatusCode entity = new JpaActionStatusCode();
        entity.setCode(create.getCode());
        entity.setName(create.getName());
        entity.setDescription(create.getDescription());
        entity.setSeverity(create.getSeverity());

        // Concurrent duplicate creation will be mapped to EntityAlreadyExistsException by global exception translation
        return actionStatusCodeRepository.save(entity);
    }

    @Override
    @Transactional
    @Retryable(includes = ConcurrencyFailureException.class, maxRetriesString = Constants.RETRY_MAX, delayString = Constants.RETRY_DELAY)
    public ActionStatusCode update(final Update update) {
        final JpaActionStatusCode entity = actionStatusCodeRepository.findById(update.getId())
                .orElseThrow(() -> new EntityNotFoundException(ActionStatusCode.class, update.getId()));

        if (update.getName() != null) {
            entity.setName(update.getName());
        }
        if (update.getDescription() != null) {
            entity.setDescription(update.getDescription());
        }
        if (update.getSeverity() != null) {
            entity.setSeverity(update.getSeverity());
        }

        return actionStatusCodeRepository.save(entity);
    }

    @Override
    public ActionStatusCode get(final long id) {
        return find(id).orElseThrow(() -> new EntityNotFoundException(ActionStatusCode.class, id));
    }

    @Override
    public Optional<ActionStatusCode> find(final long id) {
        return actionStatusCodeRepository.findById(id).map(entity -> entity);
    }

    @Override
    public Page<ActionStatusCode> findByRsql(final String rsql, final Pageable pageable) {
        final Specification<JpaActionStatusCode> spec = rsql == null
                ? Specification.unrestricted()
                : QLSupport.getInstance().buildSpec(rsql, ActionStatusCodeFields.class);

        return JpaManagementHelper.findAllWithCountBySpec(
                actionStatusCodeRepository,
                Collections.singletonList(spec),
                pageable
        );
    }

    @Override
    public Page<Integer> findUndocumentedCodes(final Pageable pageable) {
        // Ignore the incoming sort and build an unsorted page request
        final Pageable unsortedPageable;
        if (pageable instanceof OffsetBasedPageRequest) {
            unsortedPageable = new OffsetBasedPageRequest(
                    pageable.getOffset(),
                    pageable.getPageSize()
            );
        } else {
            unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        }

        return actionStatusCodeRepository.findUndocumentedCodes(unsortedPageable);
    }
}
