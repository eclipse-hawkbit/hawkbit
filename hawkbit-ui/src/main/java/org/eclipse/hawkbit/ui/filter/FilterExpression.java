/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filter;

/**
 * A filter expression interface definition to implement the UI filter
 * mechanism. The filter expression can evaluate if e.g. Targets should
 * currently be added to the target list or if the current enabled filtered will
 * filter the target and not show the newly created target.
 * 
 *
 *
 *
 */
public interface FilterExpression {

    /**
     * @return {@code true} if the expression evaluate that it should be
     *         filtered and not shown on the UI, otherwise {@code false}
     */
    boolean doFilter();

}
