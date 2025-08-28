/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.exception;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;

/**
 * Thrown if user tries to assign a {@link DistributionSet} to a {@link Target} that has an incompatible {@link TargetType}
 */
@EqualsAndHashCode(callSuper = true)
@Getter
@ToString(callSuper = true)
public class IncompatibleTargetTypeException extends AbstractServerRtException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Collection<String> targetTypeNames;
    private final Collection<String> distributionSetTypeNames;

    public IncompatibleTargetTypeException(final String targetTypeName, final Collection<String> distributionSetTypeNames) {
        super(SpServerError.SP_TARGET_TYPE_INCOMPATIBLE,
                String.format("Target of type %s is not compatible with distribution set of types %s", targetTypeName, distributionSetTypeNames)
        );
        this.targetTypeNames = Collections.singleton(targetTypeName);
        this.distributionSetTypeNames = distributionSetTypeNames;
    }

    public IncompatibleTargetTypeException(final Collection<String> targetTypeNames, final String distributionSetTypeName) {
        super(SpServerError.SP_TARGET_TYPE_INCOMPATIBLE,
                String.format("Targets of types %s are not compatible with distribution set of type %s", targetTypeNames,
                distributionSetTypeName));
        this.targetTypeNames = targetTypeNames;
        this.distributionSetTypeNames = Collections.singleton(distributionSetTypeName);
    }
}