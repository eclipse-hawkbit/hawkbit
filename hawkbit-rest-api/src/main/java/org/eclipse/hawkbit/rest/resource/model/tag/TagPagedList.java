/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model.tag;

import java.util.List;

import org.eclipse.hawkbit.rest.resource.model.PagedList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Paged list for tags.
 *
 */
public class TagPagedList extends PagedList<TagRest> {

    private final List<TagRest> content;

    /**
     * @param content
     * @param total
     */
    @JsonCreator
    public TagPagedList(@JsonProperty("content") final List<TagRest> content, @JsonProperty("total") final long total) {
        super(content, total);
        this.content = content;
    }

    /**
     * @return the content of the paged list. Never {@code null}.
     */
    public List<TagRest> getContent() {
        return content;
    }

}
