/*
 * Copyright (c) 2026 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.hawkbit.mcp.feature.target;

import org.eclipse.hawkbit.mcp.feature.enumeration.Operator;

public final class TargetFilterSchema {

    public static final String FIELDS = """
            id,name,description,createdat,lastmodifiedat,controllerid,ipaddress,lastcontrollerrequestat,updatestatus
            attribute.<key>
            metadata.<key>
            tag.name
            targettype.key,targettype.name
            assignedds.name,assignedds.version,installedds.name,installedds.version
            """;

    public static String documentation() {
        return """
                FIELDS:
                %s

                %s
                """.formatted(FIELDS, Operator.documentation());
    }
}
