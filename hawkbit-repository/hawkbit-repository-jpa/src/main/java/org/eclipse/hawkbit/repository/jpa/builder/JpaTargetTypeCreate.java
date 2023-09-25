/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.builder.AbstractTargetTypeUpdateCreate;
import org.eclipse.hawkbit.repository.builder.TargetTypeCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetType;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;

/**
 * Create/build implementation.
 *
 */
public class JpaTargetTypeCreate extends AbstractTargetTypeUpdateCreate<TargetTypeCreate>
        implements TargetTypeCreate {

    private final DistributionSetTypeManagement distributionSetTypeManagement;

    /**
     * Constructor
     *
     * @param distributionSetTypeManagement
     *          Distribution set type management
     */
    JpaTargetTypeCreate(final DistributionSetTypeManagement distributionSetTypeManagement) {
        this.distributionSetTypeManagement = distributionSetTypeManagement;
    }

    @Override
    public JpaTargetType build() {
        final JpaTargetType result = new JpaTargetType(name, description, colour);

        findDistributionSetTypeWithExceptionIfNotFound(compatible).forEach(result::addCompatibleDistributionSetType);

        return result;
    }

    private Collection<DistributionSetType> findDistributionSetTypeWithExceptionIfNotFound(
            final Collection<Long> distributionSetTypeId) {
        if (CollectionUtils.isEmpty(distributionSetTypeId)) {
            return Collections.emptyList();
        }

        final Collection<DistributionSetType> type = distributionSetTypeManagement.get(distributionSetTypeId);
        if (type.size() < distributionSetTypeId.size()) {
            throw new EntityNotFoundException(SoftwareModuleType.class, distributionSetTypeId);
        }

        return type;
    }

}
