/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;

/**
 * Thrown if user tries to assign a {@link DistributionSet} to a {@link Target}
 * that has an incompatible {@link TargetType}
 */
public class IncompatibleTargetTypeException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;
    private final Collection<String> targetTypeNames;
    private final Collection<String> distributionSetTypeNames;

    /**
     * Creates a new IncompatibleTargetTypeException with
     * {@link SpServerError#SP_TARGET_TYPE_INCOMPATIBLE} error.
     * 
     * @param targetTypeName
     *            Name of the target type
     * @param distributionSetTypeNames
     *            Names of the distribution set types
     */
    public IncompatibleTargetTypeException(final String targetTypeName,
            final Collection<String> distributionSetTypeNames) {
        super(String.format("Target of type %s is not compatible with distribution set of types %s", targetTypeName,
                distributionSetTypeNames), SpServerError.SP_TARGET_TYPE_INCOMPATIBLE);
        this.targetTypeNames = Collections.singleton(targetTypeName);
        this.distributionSetTypeNames = distributionSetTypeNames;
    }

    /**
     * Creates a new IncompatibleTargetTypeException with
     * {@link SpServerError#SP_TARGET_TYPE_INCOMPATIBLE} error.
     *
     * @param targetTypeNames
     *            Name of the target types
     * @param distributionSetTypeName
     *            Name of the distribution set type
     */
    public IncompatibleTargetTypeException(final Collection<String> targetTypeNames,
            final String distributionSetTypeName) {
        super(String.format("Targets of types %s are not compatible with distribution set of type %s", targetTypeNames,
                distributionSetTypeName), SpServerError.SP_TARGET_TYPE_INCOMPATIBLE);
        this.targetTypeNames = targetTypeNames;
        this.distributionSetTypeNames = Collections.singleton(distributionSetTypeName);
    }

    public Collection<String> getTargetTypeNames() {
        return targetTypeNames;
    }

    public Collection<String> getDistributionSetTypeNames() {
        return distributionSetTypeNames;
    }
}
