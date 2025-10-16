/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collections;
import java.util.List;

import jakarta.persistence.EntityManager;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity;
import org.eclipse.hawkbit.repository.jpa.repository.NoCountSliceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * A collection of static helper methods for the management classes
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JpaManagementHelper {

    public static <T, J extends T> Page<T> findAllWithCountBySpec(
            final JpaSpecificationExecutor<J> repository, final List<Specification<J>> specList, final Pageable pageable) {
        if (CollectionUtils.isEmpty(specList)) {
            return convertPage(repository.findAll(Specification.unrestricted(), pageable), pageable);
        }

        return convertPage(repository.findAll(combineWithAnd(specList), pageable), pageable);
    }

    public static <T, J extends T> Page<T> convertPage(final Page<J> jpaAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(jpaAll.getContent()), pageable, jpaAll.getTotalElements());
    }

    public static <J> Specification<J> combineWithAnd(final List<Specification<J>> specList) {
        if (ObjectUtils.isEmpty(specList)) {
            return Specification.unrestricted();
        } else if (specList.size() == 1) {
            return specList.get(0);
        } else {
            Specification<J> specs = specList.get(0);
            for (final Specification<J> specification : specList.subList(1, specList.size())) {
                specs = specs.and(specification);
            }
            return specs;
        }
    }

    public static <T, J extends T> Slice<T> findAllWithoutCountBySpec(
            final NoCountSliceRepository<J> repository, final List<Specification<J>> specList, final Pageable pageable) {
        if (CollectionUtils.isEmpty(specList)) {
            return convertSlice(repository.findAllWithoutCount(pageable), pageable);
        }

        return convertSlice(repository.findAllWithoutCount(combineWithAnd(specList), pageable), pageable);
    }

    public static <T, J extends T> Slice<T> convertSlice(final Slice<J> jpaAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(jpaAll.getContent()), pageable, 0);
    }

    public static <J> long countBySpec(final JpaSpecificationExecutor<J> repository, final List<Specification<J>> specList) {
        if (CollectionUtils.isEmpty(specList)) {
            return repository.count(Specification.unrestricted());
        }

        return repository.count(combineWithAnd(specList));
    }

    public static <J extends AbstractJpaBaseEntity> J touch(
            final EntityManager entityManager, final CrudRepository<J, ?> repository, final J entity) {
        // merge base entity so optLockRevision gets updated and audit
        // log written because modifying e.g. metadata is modifying the base
        // entity itself for auditing purposes.
        final J result = entityManager.merge(entity);
        result.setLastModifiedAt(1);

        return repository.save(result);
    }
}