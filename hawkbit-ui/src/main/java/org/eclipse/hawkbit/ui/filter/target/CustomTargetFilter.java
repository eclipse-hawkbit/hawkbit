/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filter.target;

import java.util.Optional;

import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.filter.FilterExpression;

/**
 * Checks if custom target filter is applied.
 *
 */
public class CustomTargetFilter implements FilterExpression {

    private final Optional<TargetFilterQuery> targetFilterQuery;

    /**
     * Initialize.
     * 
     * @param targetFilterQuery
     *            custom target filter applied
     */
    public CustomTargetFilter(final Optional<TargetFilterQuery> targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.ui.filter.FilterExpression#doFilter()
     */
    @Override
    public boolean doFilter() {
        if (!targetFilterQuery.isPresent()) {
            return false;
        }
        return true;
    }

}
