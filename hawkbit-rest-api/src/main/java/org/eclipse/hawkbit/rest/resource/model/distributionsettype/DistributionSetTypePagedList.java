/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.distributionsettype;

import java.util.List;

import org.eclipse.hawkbit.rest.resource.model.PagedList;
import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Paged list for DistributionSetType.
 *
 *
 *
 *
 */
@ApiModel("Paged list of distribution set types")
public class DistributionSetTypePagedList extends PagedList<DistributionSetTypeRest> {

    @ApiModelProperty(value = ApiModelProperties.DS_TYPE_LIST, required = true)
    private final List<DistributionSetTypeRest> content;

    /**
     * @param content
     * @param total
     */
    public DistributionSetTypePagedList(final List<DistributionSetTypeRest> content, final long total) {
        super(content, total);
        this.content = content;
    }

    /**
     * @return the content of the paged list. Never {@code null}.
     */
    public List<DistributionSetTypeRest> getContent() {
        return content;
    }

}
