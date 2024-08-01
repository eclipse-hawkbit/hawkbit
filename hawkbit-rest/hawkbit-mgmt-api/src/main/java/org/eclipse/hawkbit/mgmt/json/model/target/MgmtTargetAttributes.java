/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.mgmt.json.model.target;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link Map} with attributes of SP Target.
 */
public class MgmtTargetAttributes extends HashMap<String, String> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    @JsonIgnore
    public boolean isEmpty() {
        return super.isEmpty();
    }
}