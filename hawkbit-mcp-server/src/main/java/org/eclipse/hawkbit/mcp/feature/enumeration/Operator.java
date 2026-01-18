package org.eclipse.hawkbit.mcp.feature.enumeration;

import lombok.Getter;

@Getter
public enum Operator {

    EQUALS("==", OperatorType.COMPARISON),
    NOT_EQUALS("!=", OperatorType.COMPARISON),

    GREATER_OR_EQUAL("=ge=", OperatorType.COMPARISON),
    LESS_OR_EQUAL("=le=", OperatorType.COMPARISON),

    IN("=in=", OperatorType.COLLECTION),
    OUT("=out=", OperatorType.COLLECTION),

    IS_NULL("=is=null", OperatorType.NULL_CHECK),
    IS_NOT_NULL("=not=null", OperatorType.NULL_CHECK),

    AND(";", OperatorType.LOGICAL),
    OR(",", OperatorType.LOGICAL);

    private final String symbol;
    private final OperatorType type;

    Operator(String symbol, OperatorType type) {
        this.symbol = symbol;
        this.type = type;
    }

    public static String documentation() {
        return "OPS:\n== != =ge= =le= =in= =out= =is=null =not=null ; ,";
    }
}
