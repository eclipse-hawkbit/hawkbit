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
