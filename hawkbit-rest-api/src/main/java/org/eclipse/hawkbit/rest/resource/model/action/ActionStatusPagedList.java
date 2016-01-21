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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Paged list rest model for ActionStatus to RESTful API representation.
 *
 *
 *
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionStatusPagedList extends PagedList<ActionStatusRest> {

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
