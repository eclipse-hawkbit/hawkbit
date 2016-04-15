/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi;

/**
 * Constants for the direct device integration rest resources.
 */
public final class ControllerConstants {

    /**
     * The base URL mapping of the direct device integration rest resources.
     */
    public static final String BASE_V1_REQUEST_MAPPING = "/{tenant}/controller/v1";

    /**
     * The base URL mapping of the artifact repository rest resources.
     */
    public static final String ARTIFACTS_V1_REQUEST_MAPPING = "/{tenant}/controller/artifacts/v1";

    /**
     * Deployment action resources.
     */
    public static final String DEPLOYMENT_BASE_ACTION = "deploymentBase";

    /**
     * Cancel action resources.
     */
    public static final String CANCEL_ACTION = "cancelAction";

    /**
     * Feedback channel.
     */
    public static final String FEEDBACK = "feedback";

    /**
     * Config data action resources.
     */
    public static final String CONFIG_DATA_ACTION = "configData";

    /**
     * The artifact URL mapping rest resource.
     */
    public static final String ARTIFACT_DOWNLOAD = "artifact";

    /**
     * The artifact by filename URL mapping rest resource.
     */
    public static final String ARTIFACT_DOWNLOAD_BY_FILENAME = "/filename";

    /**
     * File suffix for MDH hash download (see Linux md5sum).
     */
    public static final String ARTIFACT_MD5_DWNL_SUFFIX = ".MD5SUM";

    // constant class, private constructor.
    private ControllerConstants() {

    }
}
