/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.dd.criteria;

/**
 * Constants that are used to transfer data between server-side and client-side
 * accept criteria.
 *
 */
public final class AcceptCriteriaConstants {

    /**
     * Key for the configured valid drag source.
     */
    public static final String DRAG_SOURCE = "ds";

    /**
     * Key-prefix for a configured valid drop target.
     */
    public static final String DROP_TARGET = "dt";

    /**
     * Key for the number of configured valid drop targets.
     */
    public static final String DROP_TARGET_COUNT = "cdt";

    /**
     * Key-prefix for a configured drop area.
     */
    public static final String DROP_AREA = "da";

    /**
     * Key for the number of configured drop areas.
     */
    public static final String DROP_AREA_COUNT = "cda";

    /**
     * Key-prefix for a configured drop area of the entire drop area
     * configuration.
     */
    public static final String DROP_AREA_CONFIG = "dac";

    /**
     * Key for the number of drop areas in the entire configuration.
     */
    public static final String DROP_AREA_CONFIG_COUNT = "cdac";

    /**
     * Key-prefix for a valid drag source component.
     */
    public static final String COMPONENT = "component";

    /**
     * Key for the number of configured valid drag source components.
     */
    public static final String COMPONENT_COUNT = "c";

    /**
     * Key for the selected mode.
     */
    public static final String MODE = "m";

    /**
     * Strict-mode.
     */
    public static final String STRICT_MODE = "s";

    /**
     * Prefix-mode.
     */
    public static final String PREFIX_MODE = "p";

    /**
     * Constants class.
     */
    private AcceptCriteriaConstants() {

    }

}
