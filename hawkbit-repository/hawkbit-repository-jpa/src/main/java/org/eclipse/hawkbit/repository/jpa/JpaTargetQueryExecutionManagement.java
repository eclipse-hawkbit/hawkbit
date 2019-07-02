/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetQueryExecutionManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collection;

/**
 * Provides a source of
 * {@linkplain org.eclipse.hawkbit.repository.model.Target}s based on Hawkbit's
 * internal device-management.
 */
@Transactional(readOnly = true)
public class JpaTargetQueryExecutionManagement implements TargetQueryExecutionManagement<JpaTarget> {
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final Database database;
    private final TargetRepository targetRepository;

    public JpaTargetQueryExecutionManagement(final TargetRepository targetRepository,
            final VirtualPropertyReplacer virtualPropertyReplacer, final Database database) {
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
        this.targetRepository = targetRepository;
    }

    @Override
    public Page<JpaTarget> findAll(@NotNull final Pageable pageable) {
        return targetRepository.findAll(pageable);
    }

    @Override
    public Page<JpaTarget> findByQuery(final Pageable pageable, @NotEmpty final String query) {
        return targetRepository.findAll(toSpec(query), pageable);
    }

    @Override
    public Page<JpaTarget> findByQuery(@NotNull final Pageable pageable, @NotEmpty final String query,
            final Collection<String> inIdList) {
        if (inIdList.isEmpty()) {
            return Page.empty();
        }
        final Specification<JpaTarget> spec = toSpec(query, inIdList);
        return targetRepository.findAll(spec, pageable);
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

    private Specification<JpaTarget> toSpec(final String query, final Collection<String> inIdList) {
        return toSpec(query).and(TargetSpecifications.hasControllerId(inIdList));
    }

    private Specification<JpaTarget> toSpec(final String query) {
        return RSQLUtility.parse(query, TargetFields.class, virtualPropertyReplacer, database);
    }

}
