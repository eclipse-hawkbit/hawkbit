/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ddi.rest.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Constants for the direct device integration rest resources.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DdiRestConstants {

    /**
     * The base URL mapping of the direct device integration rest resources.
     */
    public static final String BASE_V1_REQUEST_MAPPING = "/{tenant}/controller/v1";
    /**
     * Deployment action resources.
     */
    public static final String DEPLOYMENT_BASE_ACTION = "deploymentBase";
    /**
     * Confirmation base resource.
     */
    public static final String CONFIRMATION_BASE = "confirmationBase";
    /**
     * Activate auto-confirm
     */
    public static final String AUTO_CONFIRM_ACTIVATE = "activateAutoConfirm";
    /**
     * Deactivate auto-confirm
     */
    public static final String AUTO_CONFIRM_DEACTIVATE = "deactivateAutoConfirm";
    /**
     * Installed action resources.
     */
    public static final String INSTALLED_BASE_ACTION = "installedBase";
    /**
     * Cancel action resources.
     */
    public static final String CANCEL_ACTION = "cancelAction";
    /**
     * Feedback channel.
     */
    public static final String FEEDBACK = "feedback";
    /**
     * File suffix for MDH hash download (see Linux md5sum).
     */
    public static final String ARTIFACT_MD5_DOWNLOAD_SUFFIX = ".MD5SUM";
    /**
     * Config data action resources.
     */
    public static final String CONFIG_DATA_ACTION = "configData";
    /**
     * Default value specifying that no action history to be sent as part of response to deploymentBase
     * {@link DdiRootControllerRestApi#getControllerDeploymentBaseAction}.
     * {@link DdiRootControllerRestApi#getConfirmationBaseAction}.
     */
    public static final String NO_ACTION_HISTORY = "0";
    /**
     * Media type for CBOR content. Unfortunately, there is no other constant we
     * can reuse - even the Jackson data converter simply hardcodes this.
     */
    public static final String MEDIA_TYPE_CBOR = "application/cbor";
}