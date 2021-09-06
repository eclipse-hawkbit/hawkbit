/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.documentation;

/**
 * Constants for API documentation.
 */
public final class ApiModelPropertiesGeneric {
    public static final String ENDING = "  of the entity";

    // generic
    public static final String TENANT = "The tenant";
    public static final String ITEM_ID = "The technical identifier " + ENDING;
    public static final String NAME = "The name" + ENDING;
    public static final String DESCRPTION = "The description" + ENDING;
    public static final String COLOUR = "The colour" + ENDING;
    public static final String DELETED = "Deleted flag, used for soft deleted entities";

    public static final String CREATED_BY = "Entity was originally created by (User, AMQP-Controller, anonymous etc.)";
    public static final String CREATED_AT = "Entity was originally created at (timestamp UTC in milliseconds)";
    public static final String LAST_MODIFIED_BY = "Entity was last modified by (User, AMQP-Controller, anonymous etc.)";
    public static final String LAST_MODIFIED_AT = "Entity was last modified at (timestamp UTC in milliseconds)";

    // Paging elements
    public static final String SIZE = "Current page size";
    public static final String TOTAL_ELEMENTS = "Total number of elements";
    public static final String SELF_LINKS_TO_RESOURCE = "Links to the given resource itself";

    private ApiModelPropertiesGeneric() {
        // utility class
    }

    // parameters
    public static final String OFFSET = "The paging offset (default is 0).";
    public static final String LIMIT = "The maximum number of entries in a page (default is 50).";
    public static final String SORT = "The query parameter sort allows to define the sort order for the result of a query. "
            + "A sort criteria consists of the name of a field and the sort direction (ASC for ascending and DESC descending). "
            + "The sequence of the sort criteria (multiple can be used) defines the sort order of the entities in the result.";
    public static final String FIQL = "Query fields based on the Feed Item Query Language (FIQL). See Entity Definitions for available fields.";

    // Error/exception handling
    public static final String EXCEPTION_CLASS = "The exception class name.";
    public static final String ERROR_CODE = "The exception error code.";
    public static final String ERROR_MESSAGE = "The exception human readable message.";
    public static final String ERROR_PARAMETERS = "The exception message parameters.";

}
