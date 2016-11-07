/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.event;

import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * TenantAwareEvent to represent software module type add, update or delete.
 */
public class SoftwareModuleTypeEvent {

    /**
     * Software module type events in the Upload UI.
     */
    public enum SoftwareModuleTypeEnum {
        ADD_SOFTWARE_MODULE_TYPE, DELETE_SOFTWARE_MODULE_TYPE, UPDATE_SOFTWARE_MODULE_TYPE
    }

    private SoftwareModuleType softwareModuleType;

    private final SoftwareModuleTypeEnum softwareModuleTypeEnum;

    /**
     * @param softwareModuleTypeEnum
     * @param softwareModuleType
     */
    public SoftwareModuleTypeEvent(final SoftwareModuleTypeEnum softwareModuleTypeEnum,
            final SoftwareModuleType softwareModuleType) {
        this.softwareModuleTypeEnum = softwareModuleTypeEnum;
        this.softwareModuleType = softwareModuleType;
    }

    public SoftwareModuleTypeEnum getSoftwareModuleTypeEnum() {
        return softwareModuleTypeEnum;
    }

    public SoftwareModuleType getSoftwareModuleType() {
        return softwareModuleType;
    }

    public void setSoftwareModuleType(final SoftwareModuleType softwareModuleType) {
        this.softwareModuleType = softwareModuleType;
    }

}
