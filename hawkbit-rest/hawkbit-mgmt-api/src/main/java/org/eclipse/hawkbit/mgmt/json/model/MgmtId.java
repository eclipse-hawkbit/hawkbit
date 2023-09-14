/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A generic abstract rest model which contains only a ID for use-case e.g.
 * which allows only posting or putting an ID into the request body, e.g. for
 * assignments.
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtId {
    private Long id;
    
    /**
     * Constructor
     */
    public MgmtId() {
    }

    /**
     * Constructor
     * 
     * @param id
     *            ID of object
     */
    @JsonCreator
    public MgmtId(final Long id) {
        this.id = id;
    }


    /**
     * @return the ID
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id
     *            the ID to set
     */
    public void setId(final Long id) {
        this.id = id;
    }

}
