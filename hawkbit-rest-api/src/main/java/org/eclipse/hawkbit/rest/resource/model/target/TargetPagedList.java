/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.rest.resource.model.target;

import java.util.List;

import org.eclipse.hawkbit.rest.resource.model.PagedList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Paged list for targets.
 *
 */
public class TargetPagedList extends PagedList<TargetRest> {

    private final List<TargetRest> content;

    /**
     * @param content
     * @param total
     */
    @JsonCreator
    public TargetPagedList(@JsonProperty("content") final List<TargetRest> content,
            @JsonProperty("total") final long total) {
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
