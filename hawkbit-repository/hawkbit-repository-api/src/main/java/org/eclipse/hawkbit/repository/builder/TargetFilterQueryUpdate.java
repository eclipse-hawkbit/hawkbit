/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.hibernate.validator.constraints.NotEmpty;

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
    TargetFilterQueryUpdate name(@NotEmpty String name);

    /**
     * @param query
     *            of {@link TargetFilterQuery#getQuery()}
     * @return updated builder instance
     */
    TargetFilterQueryUpdate query(@NotEmpty String query);

}
