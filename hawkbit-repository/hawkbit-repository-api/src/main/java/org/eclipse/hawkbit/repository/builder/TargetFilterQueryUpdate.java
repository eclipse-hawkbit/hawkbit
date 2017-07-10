/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import javax.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

/**
 * Builder to update an existing {@link TargetFilterQuery} entry. Defines all
 * fields that can be updated.
 *
 */
public interface TargetFilterQueryUpdate {
    /**
     * @param name
     *            of {@link TargetFilterQuery#getName()}
     * @return updated builder instance
     */
    TargetFilterQueryUpdate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) String name);

    /**
     * @param query
     *            of {@link TargetFilterQuery#getQuery()}
     * @return updated builder instance
     */
    TargetFilterQueryUpdate query(@Size(min = 1, max = TargetFilterQuery.QUERY_MAX_SIZE) String query);

}
