/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model.targettype;

/**
 * Request Body for TargetType POST.
 *
 */
public class MgmtTargetTypeRequestBodyPost extends MgmtTargetTypeRequestBodyPut{

    /**
     * @param name
     *          the name to set
     * @return  post request body
     */
    public MgmtTargetTypeRequestBodyPost setName(final String name) {
        super.setName(name);
        return this;
    }

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

}
