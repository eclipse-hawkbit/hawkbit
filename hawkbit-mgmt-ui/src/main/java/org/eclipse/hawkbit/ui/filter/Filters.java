/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filter;

import java.util.Arrays;
import java.util.List;

/**
 * {@link Filters} which provides the functionality to combine
 * {@link FilterExpression}s.
 * 
 *
 *
 * @see FilterExpression
 *
 */
public final class Filters {

    /**
     * private.
     */
    private Filters() {

    }

    /**
     * Combines the given filter to an or-expression and evaluate them.
     * 
     * @param expressions
     *            the expressions to combine with an or-filter
     * @return an or-combined filter expression
     */
    public static FilterExpression or(final List<FilterExpression> expressions) {
        return or(expressions.toArray(new FilterExpression[expressions.size()]));
    }

    /**
     * Combines the given filter to an or-expression and evaluate them.
     * 
     * @param expressions
     *            the expressions to combine with an or-filter
     * @return an or-combined filter expression
     */
    public static FilterExpression or(final FilterExpression... expressions) {
        return new OrFilterExpression(expressions);
    }

    private static final class OrFilterExpression implements FilterExpression {

        private final FilterExpression[] expresssions;

        private OrFilterExpression(final FilterExpression[] expresssions) {
            this.expresssions = Arrays.copyOf(expresssions, expresssions.length);
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.hawkbit.server.ui.filter.FilterExpression#evaluate()
         */
        @Override
        public boolean doFilter() {
            for (final FilterExpression filterExpression : expresssions) {
                if (filterExpression.doFilter()) {
                    return true;
                }
            }
            return false;
        }
    }
}
