/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.builder.AbstractTargetUpdateCreate;
import org.eclipse.hawkbit.repository.builder.TargetCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.util.StringUtils;

/**
 * Create/build implementation.
 *
 */
public class JpaTargetCreate extends AbstractTargetUpdateCreate<TargetCreate> implements TargetCreate {

    JpaTargetCreate() {
        super(null);
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

        target.setDescription(description);
        target.setAddress(address);
        target.setUpdateStatus(getStatus().orElse(TargetUpdateStatus.UNKNOWN));
        getLastTargetQuery().ifPresent(target::setLastTargetQuery);

        return target;
    }

}
