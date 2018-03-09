/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Arrays;
import java.util.Optional;

public enum UpdateMode {

    MERGE,

    REPLACE,

    REMOVE;

    public static Optional<UpdateMode> valueOfIgnoreCase(final String name) {
        return Arrays.stream(values()).filter(mode -> mode.name().equalsIgnoreCase(name)).findFirst();
    }

}
