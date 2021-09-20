/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.builder.AbstractTargetUpdateCreate;
import org.eclipse.hawkbit.repository.builder.TargetCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.util.StringUtils;

/**
 * Create/build implementation.
 *
 */
public class JpaTargetCreate extends AbstractTargetUpdateCreate<TargetCreate> implements TargetCreate {

    private final TargetTypeManagement targetTypeManagement;

    /**
     * Constructor
     *
     * @param targetTypeManagement
     *          Target type management
     */
    JpaTargetCreate(final TargetTypeManagement targetTypeManagement) {
        super(null);
        this.targetTypeManagement = targetTypeManagement;
    }

    @Override
    public JpaTarget build() {
        JpaTarget target;

        if (StringUtils.isEmpty(securityToken)) {
            target = new JpaTarget(controllerId);
        } else {
            target = new JpaTarget(controllerId, securityToken);
        }

        if (!StringUtils.isEmpty(name)) {
            target.setName(name);
        }

        if (targetTypeId != null){
            TargetType targetType = targetTypeManagement.get(targetTypeId)
                    .orElseThrow(() -> new EntityNotFoundException(TargetType.class, targetTypeId));
            target.setTargetType(targetType);
        }

        target.setDescription(description);
        target.setAddress(address);
        target.setUpdateStatus(getStatus().orElse(TargetUpdateStatus.UNKNOWN));
        getLastTargetQuery().ifPresent(target::setLastTargetQuery);

        return target;
    }

}
