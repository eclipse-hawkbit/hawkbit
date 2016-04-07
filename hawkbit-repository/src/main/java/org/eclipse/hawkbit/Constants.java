/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit;

/**
 * A constant class which holds only static constants used within the SP server.
 *
 */
public final class Constants {

    /**
     * Defines the maximum entries for SQL in-statement. Many database have
     * limit of elements within an SQL in-statement, e.g. Oracle with maximum
     * 1000 elements. So everywhere we are using in-statements in our SQL
     * queries we need to split these elements for the in-statements by this
     * number.
     */
    public static final int MAX_ENTRIES_IN_STATEMENT = 999;

    /**
     * Constant class only private constructor.
     */
    private Constants() {

    }

}
