/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.targettype;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request Body for TargetType POST.
 *
 */
public class MgmtTargetTypeRequestBodyPost extends MgmtTargetTypeRequestBodyPut{

    @JsonProperty(required = true)
    private String name;

    @JsonProperty
    private String key;

    @Override
    public MgmtTargetTypeRequestBodyPost setDescription(final String description) {
        super.setDescription(description);
        return this;
    }

    @Override
    public MgmtTargetTypeRequestBodyPost setColour(final String colour) {
        super.setColour(colour);
        return this;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     *
     * @return updated body
     */
    public MgmtTargetTypeRequestBodyPost setName(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key
     *            the key to set
     * @return updated body
     */
    public MgmtTargetTypeRequestBodyPost setKey(final String key) {
        this.key = key;
        return this;
    }
}
