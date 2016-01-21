/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.rest.resource.model.target;

import java.util.List;

import org.eclipse.hawkbit.rest.resource.model.PagedList;
import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Paged list for targets.
 *
 *
 *
 *
 */
@ApiModel("Paged list of targets")
public class TargetPagedList extends PagedList<TargetRest> {

    @ApiModelProperty(value = ApiModelProperties.TARGET_LIST, required = true)
    private final List<TargetRest> content;

    /**
     * @param content
     * @param total
     */
    public TargetPagedList(final List<TargetRest> content, final long total) {
        super(content, total);
        this.content = content;
    }

    /**
     * @return the content of the paged list. Never {@code null}.
     */
    public List<TargetRest> getContent() {
        return content;
    }

}
