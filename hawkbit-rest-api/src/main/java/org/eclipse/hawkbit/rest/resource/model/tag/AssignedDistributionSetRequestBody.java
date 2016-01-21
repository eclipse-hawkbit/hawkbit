/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.tag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body for PUT.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignedDistributionSetRequestBody {

    @JsonProperty(value = "id", required = true)
    private Long distributionSetId;

    public Long getDistributionSetId() {
        return distributionSetId;
    }

    public AssignedDistributionSetRequestBody setDistributionSetId(final Long distributionSetId) {
        this.distributionSetId = distributionSetId;
        return this;
    }

}
