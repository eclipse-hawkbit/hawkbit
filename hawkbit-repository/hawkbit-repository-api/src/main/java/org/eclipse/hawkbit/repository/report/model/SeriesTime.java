/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.report.model;

/**
 * A series time enum definition for {@link DataReportSeriesItem}s.
 *
 *
 *
 *
 */
public enum SeriesTime {

    /**
     * hour.
     */
    HOUR,
    /**
     * day.
     */
    DAY,
    /**
     * week.
     */
    WEEK,
    /**
     * month.
     */
    MONTH,
    /**
     * year.
     */
    YEAR,

    /**
     * more than one year.
     */
    MORE_THAN_YEAR,

    /**
     * never.
     */
    NEVER;

}
