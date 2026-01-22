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
     * The base URL mapping of the rest resources.
     */
    public static final String REST = "/rest";
    /**
     * API version definition. We are using only major versions.
     */
    public static final String API_VERSION_1 = "v1";
    /**
     * The base URL mapping of the rest V1 resources.
     */
    public static final String REST_V1 = REST + "/" + API_VERSION_1;

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
     * String representation of {@link #REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE}.
     */
    public static final String REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT = "50";
    /**
     * The default limit parameter in case the limit parameter is not present in the request.
     *
     * @see #REQUEST_PARAMETER_PAGING_LIMIT
     */
    public static final int REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE = Integer.parseInt(REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT);
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
     * The request parameter for sorting. The value of the sort parameter must be in the following pattern.<br/>
     * Example: <code>https://www.foo.com/iap/sp/rest/targets?sort=field_1:ASC,field_2:DESC,field_3:ASC</code>
     */
    public static final String REQUEST_PARAMETER_SORTING = "sort";

    // API Orders (used for sorting in OpenAPI doc / Swagger UI)
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
