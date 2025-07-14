/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Constants for RESTful API.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MgmtRestConstants {

    /**
     * API version definition. We are using only major versions.
     */
    public static final String API_VERSION = "v1";
    /**
     * The base URL mapping of the SP rest resources.
     */
    public static final String BASE_REST_MAPPING = "/rest";
    /**
     * The base URL mapping of the SP rest resources.
     */
    public static final String BASE_V1_REQUEST_MAPPING = BASE_REST_MAPPING + "/v1";
    /**
     * The software module URL mapping rest resource.
     */
    public static final String SOFTWAREMODULE_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + "/softwaremodules";
    /**
     * The target URL mapping rest resource.
     */
    public static final String TARGET_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + "/targets";
    /**
     * The tag URL mapping rest resource.
     */
    public static final String TARGET_TAG_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + "/targettags";
    /**
     * The target type URL mapping rest resource.
     */
    public static final String TARGETTYPE_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + "/targettypes";
    /**
     * The target group URL mapping rest resource.
     */
    public static final String TARGET_GROUP_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + "/targetgroups";
    /**
     * The tag URL mapping rest resource.
     */
    public static final String DISTRIBUTIONSET_TAG_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING
            + "/distributionsettags";
    /**
     * The target URL mapping rest resource.
     */
    public static final String TARGET_FILTER_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + "/targetfilters";
    /**
     * The action URL mapping rest resource.
     */
    public static final String ACTION_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + "/actions";
    /**
     * The software module type URL mapping rest resource.
     */
    public static final String SOFTWAREMODULETYPE_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + "/softwaremoduletypes";
    /**
     * The distributon set base resource.
     */
    public static final String DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING
            + "/distributionsettypes";
    /**
     * The software module URL mapping rest resource.
     */
    public static final String DISTRIBUTIONSET_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + "/distributionsets";
    /**
     * The rollout URL mapping rest resource.
     */
    public static final String ROLLOUT_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + "/rollouts";
    /**
     * The basic authentication validation mapping
     */
    public static final String AUTH_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + "/userinfo";
    /**
     * String representation of
     * {@link #REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE}.
     */
    public static final String REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT = "50";
    /**
     * The default limit parameter in case the limit parameter is not present in
     * the request.
     *
     * @see #REQUEST_PARAMETER_PAGING_LIMIT
     */
    public static final int REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE = Integer
            .parseInt(REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT);
    /**
     * The base URL mapping for the spring acuator management context path.
     */
    public static final String BASE_SYSTEM_MAPPING = "/system";
    public static final String SYSTEM_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + BASE_SYSTEM_MAPPING;
    /**
     * URL mapping for system admin operations.
     */
    public static final String SYSTEM_ADMIN_MAPPING = BASE_SYSTEM_MAPPING + "/admin";
    /**
     * The target URL mapping, href link for assigned target type.
     */
    public static final String TARGET_V1_ASSIGNED_TARGET_TYPE = "targetType";
    /**
     * The target URL mapping, href link for autoConfirm state of a target.
     */
    public static final String TARGET_V1_AUTO_CONFIRM = "autoConfirm";
    /**
     * The target URL mapping, href link activate auto-confirm on a target.
     */
    public static final String TARGET_V1_ACTIVATE_AUTO_CONFIRM = "activate";
    /**
     * The target URL mapping, href link deactivate auto-confirm on a target.
     */
    public static final String TARGET_V1_DEACTIVATE_AUTO_CONFIRM = "deactivate";
    /**
     * The target URL mapping, href link for assigned distribution set.
     */
    public static final String TARGET_V1_ASSIGNED_DISTRIBUTION_SET = "assignedDS";
    /**
     * The target URL mapping, href link for installed distribution set.
     */
    public static final String TARGET_V1_INSTALLED_DISTRIBUTION_SET = "installedDS";
    /**
     * The target URL mapping, href link for target attributes.
     */
    public static final String TARGET_V1_ATTRIBUTES = "attributes";
    /**
     * The target URL mapping, href link for target actions.
     */
    public static final String TARGET_V1_ACTIONS = "actions";
    /**
     * The target URL mapping, href link for canceled actions.
     */
    public static final String TARGET_V1_CANCELED_ACTION = "canceledaction";
    /**
     * The target URL mapping, href link for canceled actions.
     */
    public static final String TARGET_V1_ACTION_STATUS = "status";
    /**
     * The target URL mapping, href link for a rollout.
     */
    public static final String TARGET_V1_ROLLOUT = "rollout";
    /**
     * The target URL mapping rest resource.
     */
    public static final String TARGET_TARGET_TYPE_V1_REQUEST_MAPPING = "/{targetId}/targettype";
    /**
     * The target type URL mapping rest resource.
     */
    public static final String TARGETTYPE_V1_DS_TYPES = "compatibledistributionsettypes";
    /**
     * The tag URL mapping rest resource.
     */
    public static final String TARGET_TAG_TARGETS_REQUEST_MAPPING = "/{targetTagId}/assigned";
    /**
     * The tag URL mapping rest resource.
     */
    public static final String DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING = "/{distributionsetTagId}/assigned";
    /**
     * Target group URL mapping rest resource
     */
    public static final String TARGET_GROUP_TARGETS_REQUEST_MAPPING = "/{group}/assigned";
    /**
     * The default offset parameter in case the offset parameter is not present in the request.
     *
     * @see #REQUEST_PARAMETER_PAGING_OFFSET
     */
    public static final String REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET = "0";
    /**
     * The default offset parameter in case the offset parameter is not present in the request.
     *
     * @see #REQUEST_PARAMETER_PAGING_OFFSET
     */
    public static final int REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE = Integer.parseInt(REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET);
    /**
     * Limit http parameter for the limitation of returned values for a paged request.
     */
    public static final String REQUEST_PARAMETER_PAGING_LIMIT = "limit";
    /**
     * The maximum limit of entities returned by rest resources.
     */
    public static final int REQUEST_PARAMETER_PAGING_MAX_LIMIT = 500;
    /**
     * Paging http parameter for the offset for a paged request.
     */
    public static final String REQUEST_PARAMETER_PAGING_OFFSET = "offset";
    /**
     * The request parameter for sorting. The value of the sort parameter must be in the following pattern. Example:
     * http://www.bosch.com/iap/sp/rest/targets?sort=field_1:ASC,field_2:DESC,field_3:ASC
     */
    public static final String REQUEST_PARAMETER_SORTING = "sort";
    /**
     * The request parameter for searching. The value of the search parameter must be in the FIQL syntax.
     */
    public static final String REQUEST_PARAMETER_SEARCH = "q";
    /**
     * The request parameter for specifying the representation mode. The value of this parameter can either be "full" or "compact".
     */
    public static final String REQUEST_PARAMETER_REPRESENTATION_MODE = "representation";
    /**
     * The default representation mode.
     */
    public static final String REQUEST_PARAMETER_REPRESENTATION_MODE_DEFAULT = "compact";
    /**
     * Request parameter for async
     */
    public static final String REQUEST_PARAMETER_ASYNC = "async";
    /**
     * The target URL mapping, href link for artifact download.
     */
    public static final String SOFTWAREMODULE_V1_ARTIFACT = "artifacts";
    /**
     * The target URL mapping, href link for software module access.
     */
    public static final String DISTRIBUTIONSET_V1_MODULE = "modules";
    /**
     * The target URL mapping, href link for type information.
     */
    public static final String SOFTWAREMODULE_V1_TYPE = "type";
    public static final String DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULES = "optionalmodules";
    public static final String DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULES = "mandatorymodules";
    public static final String DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULE_TYPES = "optionalmoduletypes";
    public static final String DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULE_TYPES = "mandatorymoduletypes";
    /**
     * Request parameter if the artifact url handler should be used
     */
    public static final String REQUEST_PARAMETER_USE_ARTIFACT_URL_HANDLER = "useartifacturlhandler";

    // Orders
    public static final String TARGET_ORDER = "1000";
    public static final String TARGET_TAG_ORDER = "2000";
    public static final String TARGET_TYPE_ORDER = "3000";
    public static final String TARGET_FILTER_ORDER = "4000";
    public static final String TARGET_GROUP_ORDER = "4500";
    public static final String ACTION_ORDER = "5000";
    public static final String ROLLOUT_ORDER = "6000";
    public static final String DISTRIBUTION_SET_ORDER = "7000";
    public static final String DISTRIBUTION_SET_TYPE_ORDER = "8000";
    public static final String DISTRIBUTION_SET_TAG_ORDER = "9000";
    public static final String SOFTWARE_MODULE_ORDER = "10000";
    public static final String SOFTWARE_MODULE_TYPE_ORDER = "11000";
    public static final String TENANT_ORDER = "100000";
    public static final String BASIC_AUTH_ORDER = "200000";
    public static final String DOWNLOAD_ARTIFACT_ORDER = "1000000";
}
