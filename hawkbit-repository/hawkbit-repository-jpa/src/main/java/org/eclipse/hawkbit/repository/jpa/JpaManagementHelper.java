/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity;
import org.eclipse.hawkbit.repository.jpa.specifications.SpecificationsBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * A collection of static helper methods for the management classes
 */
public final class JpaManagementHelper {
    private JpaManagementHelper() {
    }

    public static <T, J extends T> Page<T> findAllWithCountBySpec(final JpaSpecificationExecutor<J> repository,
            final Pageable pageable, final List<Specification<J>> specList) {
        if (CollectionUtils.isEmpty(specList)) {
            return convertPage(repository.findAll(Specification.where(null), pageable), pageable);
        }

        return convertPage(repository.findAll(combineWithAnd(specList), pageable), pageable);
    }

    public static <T, J extends T> Page<T> convertPage(final Page<J> jpaAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(jpaAll.getContent()), pageable, jpaAll.getTotalElements());
    }

    private static <J> Specification<J> combineWithAnd(final List<Specification<J>> specList) {
        return specList.size() == 1 ? specList.get(0) : SpecificationsBuilder.combineWithAnd(specList);
    }

    public static <T, J extends T> Slice<T> findAllWithoutCountBySpec(final NoCountSliceRepository<J> repository,
            final Pageable pageable, final List<Specification<J>> specList) {
        if (CollectionUtils.isEmpty(specList)) {
            return convertPage(repository.findAllWithoutCount(pageable), pageable);
        }

        return convertPage(repository.findAllWithoutCount(combineWithAnd(specList), pageable), pageable);
    }

    public static <T, J extends T> Slice<T> convertPage(final Slice<J> jpaAll, final Pageable pageable) {
        return new PageImpl<>(Collections.unmodifiableList(jpaAll.getContent()), pageable, 0);
    }

    public static <J> long countBySpec(final JpaSpecificationExecutor<J> repository,
            final List<Specification<J>> specList) {
        if (CollectionUtils.isEmpty(specList)) {
            return repository.count(Specification.where(null));
        }

        return repository.count(combineWithAnd(specList));
    }

    public static <J extends AbstractJpaBaseEntity> J touch(final EntityManager entityManager,
            final CrudRepository<J, ?> repository, final J entity) {
        // merge base entity so optLockRevision gets updated and audit
        // log written because modifying e.g. metadata is modifying the base
        // entity itself for auditing purposes.
        final J result = entityManager.merge(entity);
        result.setLastModifiedAt(0L);

        return repository.save(result);
    }

    // the format of filter string is 'name:version'. 'name' and 'version'
    // fields follow the starts_with semantic, that changes to equal for 'name'
    // field when the semicolon is present
    public static String[] getFilterNameAndVersionEntries(final String filterString) {
        final int semicolonIndex = filterString.indexOf(':');

        final String filterName = semicolonIndex != -1 ? filterString.substring(0, semicolonIndex)
                : (filterString + "%");
        final String filterVersion = semicolonIndex != -1 ? (filterString.substring(semicolonIndex + 1) + "%") : "%";

        return new String[] { !StringUtils.isEmpty(filterName) ? filterName : "%", filterVersion };
    }

}
