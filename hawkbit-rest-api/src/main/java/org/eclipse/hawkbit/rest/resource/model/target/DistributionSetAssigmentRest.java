/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.rest.resource.model.target;

import org.eclipse.hawkbit.rest.resource.model.IdRest;
import org.eclipse.hawkbit.rest.resource.model.distributionset.ActionTypeRest;

/**
 * Request Body of DistributionSet for assignment operations (ID only).
 *
 */
public class DistributionSetAssigmentRest extends IdRest {
    private long forcetime;
    private ActionTypeRest type;

    /**
     * @return the type
     */
    public ActionTypeRest getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(final ActionTypeRest type) {
        this.type = type;
    }

    /**
     * @return the forcetime
     */
    public long getForcetime() {
        return forcetime;
    }

    /**
     * @param forcetime
     *            the forcetime to set
     */
    public void setForcetime(final long forcetime) {
        this.forcetime = forcetime;
    }

}
