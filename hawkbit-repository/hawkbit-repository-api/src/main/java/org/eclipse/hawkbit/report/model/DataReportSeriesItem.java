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
 * An data report series item which contains a type and a value.
 *
 *
 *
 * @param <T>
 *            the type of the report series item
 */
public class DataReportSeriesItem<T extends Serializable> implements Serializable {

    private static final long serialVersionUID = 1L;
    private final T type;
    private final Number data;

    /**
     * @param type
     *            the type of the data report series item
     * @param data
     *            the data of the report series item
     */
    public DataReportSeriesItem(final T type, final Number data) {
        this.type = type;
        this.data = data;
    }

    /**
     * @return the type of the data report item
     */
    public T getType() {
        return type;
    }

    /**
     * @return the data of the data report item
     */
    public Number getData() {
        return data;
    }
}
