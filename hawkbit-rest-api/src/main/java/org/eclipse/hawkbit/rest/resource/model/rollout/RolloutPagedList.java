/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.rollout;

import java.util.List;

import org.eclipse.hawkbit.rest.resource.model.PagedList;

/**
 * Paged list for Rollout.
 *
 *
 */
public class RolloutPagedList extends PagedList<RolloutResponseBody> {

    private final List<RolloutResponseBody> content;

    public RolloutPagedList(final List<RolloutResponseBody> content, final long total) {
        super(content, total);
        this.content = content;
    }

    /**
     * @return the content of the paged list. Never {@code null}.
     */
    public List<RolloutResponseBody> getContent() {
        return content;
    }

}
