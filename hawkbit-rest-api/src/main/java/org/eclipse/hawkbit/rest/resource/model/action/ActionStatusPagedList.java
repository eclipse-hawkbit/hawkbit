/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.action;

import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.rest.resource.model.PagedList;
import org.eclipse.hawkbit.rest.resource.model.doc.ApiModelProperties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Paged list rest model for ActionStatus to RESTful API representation.
 *
 *
 *
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("Paged list of action status")
public class ActionStatusPagedList extends PagedList<ActionStatusRest> {

    @ApiModelProperty(value = ApiModelProperties.ACTION_STATUS_LIST)
    private final List<ActionStatusRest> content;

    /**
     * @param content
     * @param total
     */
    public ActionStatusPagedList(final List<ActionStatusRest> content, final long total) {
        super(content, total);
        this.content = content;
    }

    /**
     * Default constructor.
     */
    public ActionStatusPagedList() {
        super(Collections.emptyList(), 0);
        this.content = Collections.emptyList();
    }

    /**
     * @return the content of the paged list. Never {@code null}.
     */
    public List<ActionStatusRest> getContent() {
        return content;
    }

}
