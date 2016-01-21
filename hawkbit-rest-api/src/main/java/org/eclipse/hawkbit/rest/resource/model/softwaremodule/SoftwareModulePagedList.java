/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.softwaremodule;

import java.util.List;

import org.eclipse.hawkbit.rest.resource.model.PagedList;

/**
 * Paged list for SoftwareModule.
 *
 */
public class SoftwareModulePagedList extends PagedList<SoftwareModuleRest> {

    private final List<SoftwareModuleRest> content;

    /**
     * @param content
     * @param total
     */
    public SoftwareModulePagedList(final List<SoftwareModuleRest> content, final long total) {
        super(content, total);
        this.content = content;
    }

    /**
     * @return the content of the paged list. Never {@code null}.
     */
    public List<SoftwareModuleRest> getContent() {
        return content;
    }

}
