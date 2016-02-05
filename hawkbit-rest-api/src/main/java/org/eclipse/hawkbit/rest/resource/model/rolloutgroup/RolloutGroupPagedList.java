/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.rolloutgroup;

import java.util.List;

import org.eclipse.hawkbit.rest.resource.model.PagedList;

/**
 * Paged list for Rollout.
 *
 */
public class RolloutGroupPagedList extends PagedList<RolloutGroupResponseBody> {

    private final List<RolloutGroupResponseBody> content;

    public RolloutGroupPagedList(final List<RolloutGroupResponseBody> content, final long total) {
        super(content, total);
        this.content = content;
    }

    /**
     * @return the content of the paged list. Never {@code null}.
     */
    public List<RolloutGroupResponseBody> getContent() {
        return content;
    }

}
