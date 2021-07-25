/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.TagFields;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.builder.GenericTagUpdate;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.builder.TargetTypeCreate;
import org.eclipse.hawkbit.repository.builder.TargetTypeUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaTagCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.TagSpecification;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of {@link TargetTypeManagement}.
 *
 */
@Transactional(readOnly = true)
@Validated
public class JpaTargetTypeManagement implements TargetTypeManagement {

    private final TargetTypeRepository targetTypeRepository;

    private final TargetRepository targetRepository;

    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final Database database;

    public JpaTargetTypeManagement(final TargetTypeRepository targetTypeRepository,
                                   final TargetRepository targetRepository, final VirtualPropertyReplacer virtualPropertyReplacer,
                                   final Database database) {
        this.targetTypeRepository = targetTypeRepository;
        this.targetRepository = targetRepository;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
    }

    @Override
    public Optional<TargetType> getByName(String name) {
        return Optional.empty();
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public TargetType create(TargetTypeCreate create) {
        return null;
    }

    @Override
    public List<TargetType> create(Collection<TargetTypeCreate> creates) {
        return null;
    }

    @Override
    public void delete(String targetTypeName) {

    }

    @Override
    public Page<TargetType> findAll(Pageable pageable) {
        return null;
    }

    @Override
    public Page<TargetType> findByTarget(Pageable pageable, String controllerId) {
        return null;
    }

    @Override
    public Page<TargetType> findByRsql(Pageable pageable, String rsqlParam) {
        return null;
    }

    @Override
    public Optional<TargetType> get(long id) {
        // TODO: Add error handler
        Optional<TargetType> type = targetTypeRepository.findById(id).map(targetType -> targetType);
        return type;
    }

    @Override
    public List<TargetType> get(Collection<Long> ids) {
        return null;
    }

    @Override
    public TargetType update(TargetTypeUpdate update) {
        return null;
    }

    @Override
    public DistributionSetType assignOptionalDistributionSetTypes(long targetTypeId, Collection<Long> distributionSetTypeIds) {
        return null;
    }

    @Override
    public DistributionSetType unassignDistributionSetType(long targetTypeId, long distributionSetTypeIds) {
        return null;
    }
}
