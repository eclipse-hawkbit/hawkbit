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

/**
 * Paged list rest model for {@link Action} to RESTful API representation.
 *
 */
public class ActionPagedList extends PagedList<ActionRest> {

    private final List<ActionRest> content;

    /**
     * Empty default constructor.
     */
    public ActionPagedList() {
        super(Collections.emptyList(), 0);
        this.content = Collections.emptyList();
    }

    /**
     * @param content
     * @param total
     */
    public ActionPagedList(final List<ActionRest> content, final long total) {
        super(content, total);
        this.content = content;
    }

    /**
     * @return the content of the paged list. Never {@code null}.
     */
    public List<ActionRest> getContent() {
        return content;
    }

}
