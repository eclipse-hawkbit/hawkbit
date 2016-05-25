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
import java.util.Collections;
import java.util.List;

/**
 * A simple list report series which just contains a list of values of a report.
 *
 *
 *
 *
 */
public class ListReportSeries extends AbstractReportSeries {

    private final List<Number> data = new ArrayList<>();

    /**
     * @param name
     *            the name of the list report series
     */
    public ListReportSeries(final String name) {
        super(name);
    }

    /**
     * Constructs a ListSeries with the given series name and array of values.
     *
     * @param name
     *            the name of the list report series
     * @param values
     *            the values of the list report series
     */
    public ListReportSeries(final String name, final Number... values) {
        this(name);
        setData(values);
    }

    /**
     * Sets the values in the list series to the ones provided.
     *
     * @param values
     */
    private void setData(final Number... values) {
        this.data.clear();
        Collections.addAll(this.data, values);
    }

    /**
     * @return An array of the numeric values
     */
    public Number[] getData() {
        return data.toArray(new Number[data.size()]);
    }
}
