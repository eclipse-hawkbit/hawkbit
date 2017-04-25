/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.view.filter;

import org.eclipse.hawkbit.ui.common.table.AbstractBaseViewFilter;
import org.eclipse.hawkbit.ui.distributions.DistributionsView;

/**
 * View Filter class which holds the information about the Distributions View
 *
 */
public class OnlyEventsFromDistributionsViewFilter extends AbstractBaseViewFilter {

    @Override
    protected Class<?> getOriginView() {
        return DistributionsView.class;
    }
}
