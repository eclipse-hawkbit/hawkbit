/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * An implementation of the {@link PageRequest} which is offset based by means
 * the offset is given and not the page number as in the original
 * {@link PageRequest} implementation where the offset is generated. Due that
 * the REST-API is working with {@code offset} and {@code limit} parameter we
 * need an offset based page request.
 */
@Data
public final class OffsetBasedPageRequest extends PageRequest {

    private static final long serialVersionUID = 1L;

    private final long offset;

    /**
     * Creates a new {@link OffsetBasedPageRequest}. Offsets are zero indexed,
     * thus providing 0 for {@code offset} will return the first entry.
     * 
     * @param offset
     *            zero-based offset index.
     * @param limit
     *            the limit of the page to be returned.
     */
    public OffsetBasedPageRequest(final long offset, final int limit) {
        this(offset, limit, Sort.unsorted());
    }

    /**
     * Creates a new {@link OffsetBasedPageRequest}. Offsets are zero indexed,
     * thus providing 0 for {@code offset} will return the first entry.
     * 
     * @param offset
     *            zero-based offset index.
     * @param limit
     *            the limit of the page to be returned.
     * @param sort
     *            sort can be {@literal null}.
     */
    public OffsetBasedPageRequest(final long offset, final int limit, final Sort sort) {
        super(0, limit, sort);
        this.offset = offset;
    }
}