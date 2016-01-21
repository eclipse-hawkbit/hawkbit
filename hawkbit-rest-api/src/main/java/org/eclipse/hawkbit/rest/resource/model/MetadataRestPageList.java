/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model;

import java.util.List;

/**
 * The rest model for a paged meta data list.
 *
 */
public class MetadataRestPageList extends PagedList<MetadataRest> {

    private final List<MetadataRest> content;

    /**
     * @param content
     *            the meta data rest model list as content
     * @param total
     *            the total number of the meta data
     */
    public MetadataRestPageList(final List<MetadataRest> content, final long total) {
        super(content, total);
        this.content = content;
    }

    /**
     * @return the content of this paged list
     */
    public List<MetadataRest> getContent() {
        return content;
    }
}
