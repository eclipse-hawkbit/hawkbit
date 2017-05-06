/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.event;

import org.eclipse.hawkbit.ui.common.table.BaseUIEvent;

/**
 * Distribution Set Filter Event. Is published when there is a filter action on
 * a distribution set table on the Deployment or Distribution View. It is
 * possible to filter by text or tag.
 */
public class RefreshDistributionTableByFilterEvent extends BaseUIEvent {

}
