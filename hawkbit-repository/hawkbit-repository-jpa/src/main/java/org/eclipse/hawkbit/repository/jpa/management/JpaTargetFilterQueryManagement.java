/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import cz.jirutka.rsql.parser.RSQLParserException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.ql.jpa.QLSupport;
import org.eclipse.hawkbit.repository.AutoAssignmentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.jpa.repository.TargetFilterQueryRepository;
import org.eclipse.hawkbit.repository.model.AutoAssignment;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.qfields.TargetFields;
import org.eclipse.hawkbit.repository.qfields.TargetFilterQueryFields;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link TargetFilterQueryManagement}.
 */
@Slf4j
@Transactional(readOnly = true)
@Validated
@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "target-filter-management" }, matchIfMissing = true)
class JpaTargetFilterQueryManagement
        extends
        AbstractJpaRepositoryManagement<JpaTargetFilterQuery, TargetFilterQueryManagement.UpdateCreate, TargetFilterQueryManagement.Update, TargetFilterQueryRepository, TargetFilterQueryFields>
        implements TargetFilterQueryManagement<JpaTargetFilterQuery> {

    private AutoAssignmentManagement<? extends AutoAssignment> autoAssignmentManagement;
    private DistributionSetManagement<? extends DistributionSet> distributionSetManagement;

    protected JpaTargetFilterQueryManagement(
            final TargetFilterQueryRepository targetFilterQueryRepository, final EntityManager entityManager,
            final AutoAssignmentManagement<? extends AutoAssignment> autoAssignmentManagement,
            final DistributionSetManagement<? extends DistributionSet> distributionSetManagement) {
        super(targetFilterQueryRepository, entityManager);
        this.autoAssignmentManagement = autoAssignmentManagement;
        this.distributionSetManagement = distributionSetManagement;
    }

    @Override
    @Transactional
    public JpaTargetFilterQuery create(final UpdateCreate create) {
        validate(create);
        return super.create(create);
    }

    @Override
    @Transactional
    public List<JpaTargetFilterQuery> create(final Collection<UpdateCreate> creates) {
        creates.forEach(this::validate);
        return super.create(creates);
    }

    @Override
    protected JpaTargetFilterQuery jpaEntity(final Object create) {
        return super.jpaEntity(create);
    }

    @Override
    @Transactional
    public JpaTargetFilterQuery update(final Update update) {
        updateAutoAssignment(update);
        validate(update);
        return super.update(update);
    }

    @Override
    @Transactional
    public Map<Long, JpaTargetFilterQuery> update(final Collection<Update> updates) {
        updates.forEach(update -> {
            updateAutoAssignment(update);
            validate(update);
        });
        return super.update(updates);
    }

    @Override
    @Transactional
    public void delete(final long id) {
        findLinkedAutoAssignment(id).ifPresent(autoAssignment -> {
            unlinkAutoAssignment(id);
            autoAssignmentManagement.delete(autoAssignment.getId());
        });
        super.delete(id);
    }

    @Override
    @Transactional
    public void delete(final Collection<Long> ids) {
        ids.forEach(id -> findLinkedAutoAssignment(id).ifPresent(autoAssignment -> {
            unlinkAutoAssignment(id);
            autoAssignmentManagement.delete(autoAssignment.getId());
        }));
        super.delete(ids);
    }

    @Override
    @Transactional
    public AutoAssignment createLinkedAutoAssignment(final long id, final AutoAssignmentManagement.Create create) {

        unlinkAutoAssignment(id);
        findLinkedAutoAssignment(id).ifPresent(autoAssignment -> autoAssignmentManagement.delete(autoAssignment.getId()));
        entityManager.flush();

        final AutoAssignment created = autoAssignmentManagement.create(create);
        return created;
    }

    @Override
    @Transactional
    public void deleteLinkedAutoAssignment(final long id) {
        unlinkAutoAssignment(id);
        autoAssignmentManagement.findByName(get(id).getName())
                .ifPresent(autoAssignment -> autoAssignmentManagement.delete(autoAssignment.getId()));
    }

    @Override
    public Optional<AutoAssignment> findLinkedAutoAssignment(final long id) {
        final TargetFilterQuery targetFilterQuery = get(id);
        Optional<AutoAssignment> searchResult = autoAssignmentManagement.findByName(targetFilterQuery.getName());
        if (searchResult.isPresent() && !searchResult.get().getTargetFilterQuery().equals(targetFilterQuery.getQuery())) {
            searchResult = Optional.empty();
        }

        return searchResult;
    }

    @Override
    public void verifyTargetFilterQuerySyntax(final String query) {
        try {
            QLSupport.getInstance().validate(query, TargetFields.class, JpaTarget.class);
        } catch (final RSQLParserException | RSQLParameterUnsupportedFieldException e) {
            log.debug("The RSQL query '{}}' is invalid.", query, e);
            throw new RSQLParameterSyntaxException("Cannot create a Rollout with an empty target query filter!");
        }
    }

    private void validate(final UpdateCreate create) {
        Optional.ofNullable(create.getQuery()).ifPresent(query -> {
            // validate the RSQL query syntax
            QLSupport.getInstance().validate(query, TargetFields.class, JpaTarget.class);
        });
    }

    private void validate(final Update update) {
        final JpaTargetFilterQuery targetFilterQuery = jpaRepository.getById(update.getId());
        Optional.ofNullable(update.getQuery()).ifPresent(query -> {
            // validate the RSQL query syntax
            QLSupport.getInstance().validate(query, TargetFields.class, JpaTarget.class);
            // set the new query
            targetFilterQuery.setQuery(query);
        });
    }

    private void updateAutoAssignment(Update update) {
        findLinkedAutoAssignment(update.getId()).ifPresent(autoAssignment -> {
            AutoAssignmentManagement.Create create = AutoAssignmentManagement.Create.builder()
                    .name(update.getName() != null ? update.getName() : autoAssignment.getName())
                    .description(autoAssignment.getDescription())
                    .targetFilterQuery(update.getQuery() != null ? update.getQuery() : autoAssignment.getTargetFilterQuery())
                    .distributionSet(autoAssignment.getDistributionSet())
                    .actionType(autoAssignment.getActionType())
                    .confirmationRequired(autoAssignment.isConfirmationRequired())
                    .weight(autoAssignment.getWeight().orElse(null))
                    .startAt(autoAssignment.getStartAt())
                    .build();
            unlinkAutoAssignment(update.getId());
            autoAssignmentManagement.delete(autoAssignment.getId());
            entityManager.flush();
            autoAssignmentManagement.create(create);
        });
    }


    /**
     * Clears the in-memory link from the target filter query to its (read-only mapped) auto assignment.
     * <p>
     * The auto assignment is an independent entity linked to the filter only by matching name and query. When it is
     * removed or replaced within the same transaction, the still-managed target filter query would otherwise keep a
     * reference to the no-longer-persistent auto assignment, which Hibernate rejects on flush. Callers that delete or
     * replace the linked auto assignment must call this first.
     */
    @Transactional
    private void unlinkAutoAssignment(final long id) {
        jpaRepository.getById(id).setAutoAssignment(null);
    }
}