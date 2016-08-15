/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.report.model;

import java.io.Serializable;

/**
 * An abstract report series.
 */
public class AbstractReportSeries implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;

    /**
     * @param name
     *            the name of the series
     */
    public AbstractReportSeries(final String name) {
        this.name = name;
    }

    /**
     * @return the name of the series
     */
    public String getName() {
        return name;
    }
}
