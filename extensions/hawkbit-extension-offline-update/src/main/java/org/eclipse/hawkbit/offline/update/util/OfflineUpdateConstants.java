/**
 * Copyright (c) Siemens AG, 2017
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.offline.update.util;

/**
 * Constants for the offline software update REST resources.
 */
public final class OfflineUpdateConstants {

    /**
     * The base URL mapping of the offline software update REST resources.
     */
    public static final String BASE_V1_REQUEST_MAPPING = "/rest/v1/distributionsets";

    public static final String UPDATE_OFFLINE_TARGET = "offlineInstall";

    /**
     * Private constructor to disable the creation of an instance as this is a
     * class containing only constants.
     */
    private OfflineUpdateConstants() {
        throw new IllegalAccessError("This class contains only constants.");
    }
}
