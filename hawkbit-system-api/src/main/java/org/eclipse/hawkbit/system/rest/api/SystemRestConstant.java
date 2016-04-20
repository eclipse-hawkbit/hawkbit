/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.system.rest.api;

/**
 *
 */
public final class SystemRestConstant {

    /**
     * API version definition. We are using only major versions.
     */
    public static final String API_VERSION = "v1";

    /**
     * The base URL mapping for the spring acuator management context path.
     */
    public static final String BASE_SYSTEM_MAPPING = "/system";

    /**
     * The base URL mapping of the SP rest resources.
     */
    public static final String BASE_V1_REQUEST_MAPPING = "/rest/" + API_VERSION;

    /**
     * URL mapping for system admin operations.
     */
    public static final String SYSTEM_ADMIN_MAPPING = BASE_SYSTEM_MAPPING + "/admin";

    public static final String SYSTEM_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + BASE_SYSTEM_MAPPING;

    private SystemRestConstant() {

    }
}
