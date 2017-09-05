/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.builder.AbstractDistributionSetTypeUpdateCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.util.CollectionUtils;

/**
 * Create/build implementation.
 *
 */
public class JpaDistributionSetTypeCreate extends AbstractDistributionSetTypeUpdateCreate<DistributionSetTypeCreate>
        implements DistributionSetTypeCreate {

    private final SoftwareModuleTypeManagement softwareModuleTypeManagement;

    JpaDistributionSetTypeCreate(final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
    }

    @Override
    public JpaDistributionSetType build() {
        final JpaDistributionSetType result = new JpaDistributionSetType(key, name, description, colour);

        findSoftwareModuleTypeWithExceptionIfNotFound(mandatory).forEach(result::addMandatoryModuleType);
        findSoftwareModuleTypeWithExceptionIfNotFound(optional).forEach(result::addOptionalModuleType);

        return result;
    }

    private Collection<SoftwareModuleType> findSoftwareModuleTypeWithExceptionIfNotFound(
            final Collection<Long> softwareModuleTypeId) {
        if (CollectionUtils.isEmpty(softwareModuleTypeId)) {
            return Collections.emptyList();
        }

        final Collection<SoftwareModuleType> module = softwareModuleTypeManagement.get(softwareModuleTypeId);
        if (module.size() < softwareModuleTypeId.size()) {
            throw new EntityNotFoundException(SoftwareModuleType.class, softwareModuleTypeId);
        }

        return module;
    }

}
