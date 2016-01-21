/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.distributionset;

import java.util.List;

import org.eclipse.hawkbit.rest.resource.model.PagedList;

/**
 * Paged list for SoftwareModule.
 *
 */
public class DistributionSetPagedList extends PagedList<DistributionSetRest> {

    private final List<DistributionSetRest> content;

    /**
     * @param content
     * @param total
     */
    public DistributionSetPagedList(final List<DistributionSetRest> content, final long total) {
        super(content, total);
        this.content = content;
    }

    /**
     * @return the content of the paged list. Never {@code null}.
     */
    public List<DistributionSetRest> getContent() {
        return content;
    }

}
