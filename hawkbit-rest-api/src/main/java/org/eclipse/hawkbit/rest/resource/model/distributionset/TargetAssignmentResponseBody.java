/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.distributionset;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Response Body of Target for assignment operations.
 * 
 *
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("Target Assignment Response")
public class TargetAssignmentResponseBody {

    @ApiModelProperty(value = "The number of targets have been assigned")
    private int assigned;
    @ApiModelProperty(value = "The number of targets which already have been the assignment")
    private int alreadyAssigned;
    @ApiModelProperty(value = "The total number of targets")
    private int total;

    /**
     * @return the assigned
     */
    public int getAssigned() {
        return assigned;
    }

    /**
     * @param assigned
     *            the assigned to set
     */
    public void setAssigned(final int assigned) {
        this.assigned = assigned;
    }

    /**
     * @return the alreadyAssigned
     */
    public int getAlreadyAssigned() {
        return alreadyAssigned;
    }

    /**
     * @param alreadyAssigned
     *            the alreadyAssigned to set
     */
    public void setAlreadyAssigned(final int alreadyAssigned) {
        this.alreadyAssigned = alreadyAssigned;
    }

    /**
     * @return the total
     */
    public int getTotal() {
        return total;
    }

    /**
     * @param total
     *            the total to set
     */
    public void setTotal(final int total) {
        this.total = total;
    }
}
