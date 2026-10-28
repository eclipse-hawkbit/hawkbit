/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documents the permissions a REST endpoint requires.
 * <p/>
 * It is documentation only: it is not evaluated at the REST layer. The actual access checks are done by the services
 * (via {@code @PreAuthorize}) the endpoint delegates to. The OpenAPI documentation is generated from it, so the
 * permissions are not written by hand in the operation descriptions. If permission tracking is enabled, the actually checked
 * permissions of the requests are verified against it.
 * <p/>
 * Use the {@link ApiPermission} constants as values.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiredPermissions {

    /**
     * @return the permissions (see {@link ApiPermission}) that are all required to call the endpoint. Empty means that only
     *         authentication is required (if no {@link #anyOf()} is set)
     */
    String[] value();

    /**
     * @return groups of alternative permissions - at least one permission of every group is required (e.g. when the
     *         service checks {@code A or B})
     */
    AnyOf[] anyOf() default {};

    /**
     * @return permissions that are required only under some conditions, e.g. depending on the request parameters or the
     *         state of the entities (the condition shall be explained in the description)
     */
    String[] optional() default {};

    /**
     * Group of alternative permissions - any of them is sufficient.
     */
    @Documented
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface AnyOf {

        /**
         * @return the alternative permissions (see {@link ApiPermission})
         */
        String[] value();
    }
}
