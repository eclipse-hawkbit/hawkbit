/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.targettype;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeAssignment;
import org.eclipse.hawkbit.mgmt.json.model.distributionsettype.MgmtDistributionSetTypeRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremoduletype.MgmtSoftwareModuleTypeAssigment;

/**
 * Request Body for TargetType POST.
 *
 */
public class MgmtTargetTypeRequestBodyPost extends MgmtTargetTypeRequestBodyPut{

    @JsonProperty
    private List<MgmtDistributionSetTypeAssignment> compatibledistributionsettypes;

    /**
     * @param name
     *          the name to set
     * @return  post request body
     */
    @Override
    public MgmtTargetTypeRequestBodyPost setName(final String name) {
        super.setName(name);
        return this;
    }

    @Override
    public MgmtTargetTypeRequestBodyPost setDescription(final String description) {
        super.setDescription(description);
        return this;
    }

    @Override
    public MgmtTargetTypeRequestBodyPost setColour(final String colour) {
        super.setColour(colour);
        return this;
    }


    /**
     * @return the compatible ds types
     */
    public List<MgmtDistributionSetTypeAssignment> getCompatibleDsTypes() {
        return compatibledistributionsettypes;
    }

    /**
     * @param compatibleDsTypes
     *            the compatible ds types to set
     *
     * @return updated body
     */
    public MgmtTargetTypeRequestBodyPost setCompatibleDsTypes(
            final List<MgmtDistributionSetTypeAssignment> compatibleDsTypes) {
        this.compatibledistributionsettypes = compatibleDsTypes;
        return this;
    }
}
