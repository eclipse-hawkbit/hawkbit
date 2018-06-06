/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

/**
 * Constants for RESTful API.
 *
 */
public final class MgmtRestConstants {

    /**
     * API version definition. We are using only major versions.
     */
    public static final String API_VERSION = "v1";

    /**
     * The base URL mapping of the SP rest resources.
     */
    public static final String BASE_V1_REQUEST_MAPPING = "/rest/v1";

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
     * The software module URL mapping rest resource.
     */
    public static final String SOFTWAREMODULE_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + "/softwaremodules";

    public static final String DOWNLOAD_ID_V1_REQUEST_MAPPING_BASE = "/api/" + API_VERSION + "/downloadserver/";

    public static final String DOWNLOAD_ID_V1_REQUEST_MAPPING = "/downloadId/{tenant}/{downloadId}";

    /**
     * The base URL mapping for the spring acuator management context path.
     */
    public static final String BASE_SYSTEM_MAPPING = "/system";

    /**
     * URL mapping for system admin operations.
     */
    public static final String SYSTEM_ADMIN_MAPPING = BASE_SYSTEM_MAPPING + "/admin";

    public static final String SYSTEM_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + BASE_SYSTEM_MAPPING;

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
     * The target URL mapping rest resource.
     */
    public static final String TARGET_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + "/targets";

    /**
     * The tag URL mapping rest resource.
     */
    public static final String TARGET_TAG_V1_REQUEST_MAPPING = BASE_V1_REQUEST_MAPPING + "/targettags";

    /**
     * The tag URL mapping rest resource.
     * 
     * @deprecated {@link #TARGET_TAG_TARGETS_REQUEST_MAPPING} is preferred as
     *             this resource on GET supports paging
     */
    @Deprecated
    public static final String DEPRECATAED_TARGET_TAG_TARGETS_REQUEST_MAPPING = "/{targetTagId}/targets";

    /**
     * The tag URL mapping rest resource.
     */
    public static final String TARGET_TAG_TARGETS_REQUEST_MAPPING = "/{targetTagId}/assigned";

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
     * The deprecated tag URL mapping rest resource.
     * 
     * @deprecated {@link #DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING}
     *             is preferred as this resource on GET supports paging
     */
    @Deprecated
    public static final String DEPRECATED_DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING = "/{distributionsetTagId}/distributionsets";

    /**
     * The tag URL mapping rest resource.
     */
    public static final String DISTRIBUTIONSET_TAG_DISTRIBUTIONSETS_REQUEST_MAPPING = "/{distributionsetTagId}/assigned";

    /**
     * The default offset parameter in case the offset parameter is not present
     * in the request.
     *
     * @see #REQUEST_PARAMETER_PAGING_OFFSET
     */
    public static final String REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET = "0";

    /**
     * The default offset parameter in case the offset parameter is not present
     * in the request.
     *
     * @see #REQUEST_PARAMETER_PAGING_OFFSET
     */
    public static final int REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE = Integer
            .parseInt(REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET);

    /**
     * Limit http parameter for the limitation of returned values for a paged
     * request.
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
     * The request parameter for sorting. The value of the sort parameter must
     * be in the following pattern. Example:
     * http://www.bosch.com/iap/sp/rest/targets?sort=field_1:ASC,field_2:DESC,
     * field_3:ASC
     */
    public static final String REQUEST_PARAMETER_SORTING = "sort";

    /**
     * The request parameter for searching. The value of the search parameter
     * must be in the FIQL syntax.
     */
    public static final String REQUEST_PARAMETER_SEARCH = "q";

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

    // constant class, private constructor.
    private MgmtRestConstants() {

    }
}
