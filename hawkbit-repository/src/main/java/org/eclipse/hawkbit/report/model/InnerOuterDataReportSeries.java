/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.report.model;

import java.io.Serializable;

/**
 * A double data series which contains an inner and an outer series ideal for
 * showing donut charts.
 *
 *
 *
 * @param <T>
 *            The type parameter for the report series data
 */
public class InnerOuterDataReportSeries<T extends Serializable> {

    private final DataReportSeries<T> innerSeries;
    private final DataReportSeries<T> outerSeries;

    /**
     * @param innerSeries
     *            the innerseries of an circle donut chart
     * @param outerSeries
     *            the outer series of an donut chart
     */
    public InnerOuterDataReportSeries(final DataReportSeries<T> innerSeries, final DataReportSeries<T> outerSeries) {
        this.innerSeries = innerSeries;
        this.outerSeries = outerSeries;
    }

    /**
     * @return the innerSeries
     */
    public DataReportSeries<T> getInnerSeries() {
        return this.innerSeries;
    }

    /**
     * @return the outerSeries
     */
    public DataReportSeries<T> getOuterSeries() {
        return this.outerSeries;
    }
}
