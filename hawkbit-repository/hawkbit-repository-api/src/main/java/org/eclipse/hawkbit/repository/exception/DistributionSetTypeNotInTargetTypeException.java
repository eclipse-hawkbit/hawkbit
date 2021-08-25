/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

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
     *            that is not part of given {@link TargetType}
     * @param targetTypeId
     *            of the {@link TargetType} where given
     *            {@link DistributionSetType} is not part of
     */
    public DistributionSetTypeNotInTargetTypeException(final Long distributionSetTypeId,
                                                               final Long targetTypeId) {
        super(DistributionSetType.class.getSimpleName() + " with id {" + distributionSetTypeId + "} is not part of "
                + TargetType.class.getSimpleName() + " with id {" + targetTypeId + "}.");
    }
}
