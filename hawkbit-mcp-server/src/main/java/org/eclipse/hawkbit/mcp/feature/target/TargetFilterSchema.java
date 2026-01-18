package org.eclipse.hawkbit.mcp.feature.target;

import org.eclipse.hawkbit.mcp.feature.enumeration.Operator;

public class TargetFilterSchema {

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
