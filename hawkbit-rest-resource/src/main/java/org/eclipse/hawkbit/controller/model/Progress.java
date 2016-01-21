/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.controller.model;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Action fulfillment progress by means of gives the achieved amount of maximal
 * of possible levels.
 *
 */
public class Progress {

    @NotNull
    private final Integer cnt;

    private final Integer of;

    /**
     * Constructor.
     *
     * @param cnt
     *            achieved amount
     * @param of
     *            maximum levels
     */
    @JsonCreator
    public Progress(@JsonProperty("cnt") final Integer cnt, @JsonProperty("of") final Integer of) {
        super();
        this.cnt = cnt;
        this.of = of;
    }

    public Integer getCnt() {
        return cnt;
    }

    public Integer getOf() {
        return of;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Progress [cnt=" + cnt + ", of=" + of + "]";
    }

}
