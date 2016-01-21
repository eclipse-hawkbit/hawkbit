/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.report.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * A data report series which contains a list of {@link DataReportSeriesItem}.
 * 
 *
 *
 * @param <T>
 *            the type of the report series item
 */
public class DataReportSeries<T extends Object> extends AbstractReportSeries {

    private final List<DataReportSeriesItem<T>> data = new ArrayList<>();

    /**
     * @param name
     *            the name of data series
     */
    public DataReportSeries(final String name) {
        super(name);
    }

    /**
     * Constructs a ListSeries with the given series name and array of values.
     *
     * @param name
     *            the name of the data series
     * @param values
     *            data report series item for this data series.
     */
    public DataReportSeries(final String name, final List<DataReportSeriesItem<T>> values) {
        this(name);
        setData(values);
    }

    private void setData(final List<DataReportSeriesItem<T>> values) {
        this.data.clear();
        data.addAll(values);
    }

    /**
     * @return An array of the numeric values
     */
    @SuppressWarnings("unchecked")
    public DataReportSeriesItem<T>[] getData() {
        return data.toArray(new DataReportSeriesItem[data.size()]);
    }

    public Stream<DataReportSeriesItem<T>> getDataStream() {
        return data.stream();
    }
}
