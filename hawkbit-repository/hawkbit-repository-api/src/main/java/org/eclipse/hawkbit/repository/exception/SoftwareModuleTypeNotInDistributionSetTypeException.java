/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * the {@link SoftwareModuleTypeNotInDistributionSetTypeException} is thrown
 * when a {@link SoftwareModuleType} is requested as part of a
 * {@link DistributionSetType} but actually neither
 * {@link DistributionSetType#getMandatoryModuleTypes()} or
 * {@link DistributionSetType#getOptionalModuleTypes()}.
 */
public class SoftwareModuleTypeNotInDistributionSetTypeException extends EntityNotFoundException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     * 
     * @param moduleTypeId
     *            thats is not part of given {@link DistributionSetType}
     * @param distributionSetTypeId
     *            of the {@link DistributionSetType} where given
     *            {@link SoftwareModuleType} is not part of
     */
    public SoftwareModuleTypeNotInDistributionSetTypeException(final Long moduleTypeId,
            final Long distributionSetTypeId) {
        super(SoftwareModuleType.class.getSimpleName() + " with id {" + moduleTypeId + "} is not part of "
                + DistributionSetType.class.getSimpleName() + " with id {" + distributionSetTypeId + "}.");
    }

}
