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

import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TargetType;

/**
 * the {@link DistributionSetTypeNotInTargetTypeException} is thrown when a
 * {@link DistributionSetType} is requested as part of a {@link TargetType} but
 * is not returned in {@link TargetType#getCompatibleDistributionSetTypes()}.
 */
public class DistributionSetTypeNotInTargetTypeException extends EntityNotFoundException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param distributionSetTypeId
     *            that is not compatible with given {@link TargetType}s
     * @param targetTypeIds
     *            of the {@link TargetType}s where given {@link DistributionSetType}
     *            is not part of
     */
    public DistributionSetTypeNotInTargetTypeException(final Long distributionSetTypeId,
            final Collection<Long> targetTypeIds) {
        super("DistributionSetType " + distributionSetTypeId + " is not compatible to TargetTypes " + targetTypeIds);
    }

    /**
     * Constructor
     *
     * @param distributionSetTypeIds
     *            that are not compatible with given {@link TargetType}
     * @param targetTypeId
     *            of the {@link TargetType} where given {@link DistributionSetType}s
     *            are not part of
     */
    public DistributionSetTypeNotInTargetTypeException(final Collection<Long> distributionSetTypeIds,
            final Long targetTypeId) {
        super("DistributionSetTypes " + distributionSetTypeIds + " are not compatible to TargetTypes " + targetTypeId);
    }
}
