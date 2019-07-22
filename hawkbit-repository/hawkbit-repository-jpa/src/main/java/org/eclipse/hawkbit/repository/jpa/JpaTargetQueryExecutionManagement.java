/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collection;
import java.util.Collections;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetQueryExecutionManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Provides a source of
 * {@linkplain org.eclipse.hawkbit.repository.model.Target}s based on Hawkbit's
 * internal device-management.
 */
@Validated
@Transactional(readOnly = true)
public class JpaTargetQueryExecutionManagement implements TargetQueryExecutionManagement {
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final Database database;
    private final TargetRepository targetRepository;

    /**
     * Creates a {@linkplain JpaTargetQueryExecutionManagement}
     * @param targetRepository the target repo
     * @param virtualPropertyReplacer the virtual property replacer
     * @param database the database
     */
    public JpaTargetQueryExecutionManagement(final TargetRepository targetRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer, final Database database) {
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
        this.targetRepository = targetRepository;
    }

    @Override
    public Page<Target> findAll(@NotNull final Pageable pageable) {
        return convertPage(targetRepository.findAll(pageable));
    }

    @Override
    public Page<Target> findByQuery(final Pageable pageable, @NotEmpty final String query) {
        return convertPage(targetRepository.findAll(toSpec(query), pageable));
    }

    @Override
    public Page<Target> findByQuery(@NotNull final Pageable pageable, @NotEmpty final String query,
            @NotNull final Collection<String> inIdList) {
        if (inIdList.isEmpty()) {
            return Page.empty();
        }
        final Specification<JpaTarget> spec = toSpec(query, inIdList);
        return convertPage(targetRepository.findAll(spec, pageable));
    }

    private static Page<Target> convertPage(final Page<? extends Target> result) {
        return new PageImpl<>(Collections.unmodifiableList(result.getContent()), result.getPageable(),
                result.getTotalElements());
    }

    @Override
    public long countByQuery(@NotEmpty final String query) {
        return targetRepository.count((root, q, cb) -> {
            q.distinct(true);
            return toSpec(query).toPredicate(root, q, cb);
        });
    }

    @Override
    public long countByQuery(@NotEmpty final String query, @NotNull final Collection<String> inIdList) {
        return inIdList.isEmpty() ? 0L : targetRepository.count((root, q, cb) -> {
            q.distinct(true);
            return toSpec(query, inIdList).toPredicate(root, q, cb);
        });
    }

    @Override
    public long count() {
        return targetRepository.count();
    }

    private Specification<JpaTarget> toSpec(final String query, final Collection<String> inIdList) {
        return toSpec(query).and(TargetSpecifications.hasControllerId(inIdList));
    }

    private Specification<JpaTarget> toSpec(final String query) {
        return RSQLUtility.parse(query, TargetFields.class, virtualPropertyReplacer, database);
    }
}
