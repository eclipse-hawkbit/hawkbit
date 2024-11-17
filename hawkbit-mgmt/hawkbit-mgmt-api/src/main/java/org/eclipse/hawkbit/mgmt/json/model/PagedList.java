/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.hateoas.RepresentationModel;

/**
 * A list representation with meta data for pagination, e.g. containing the
 * total elements and size of content. The content of the actual list is stored
 * in the {@link #content} field.
 *
 * @param <T> the type of elements in this list
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class PagedList<T> extends RepresentationModel<PagedList<T>> {

    @JsonProperty
    private final List<T> content;

    @JsonProperty
    private final long total;

    private final int size;

    /**
     * creates a new paged list with the given {@code content} and {@code total}
     * .
     *
     * @param content the actual content of the list
     * @param total the total amount of elements
     * @throws NullPointerException in case {@code content} is {@code null}.
     */
    @JsonCreator
    public PagedList(@JsonProperty("content") @NotNull final List<T> content, @JsonProperty("total") final long total) {
        this.size = content.size();
        this.total = total;
        this.content = content;
    }
}