/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.exception;

import java.io.Serial;

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

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     *
     * @param moduleTypeId that is not part of given {@link DistributionSetType}
     * @param distributionSetTypeId of the {@link DistributionSetType} where given {@link SoftwareModuleType} is not part of
     */
    public SoftwareModuleTypeNotInDistributionSetTypeException(final Long moduleTypeId,
            final Long distributionSetTypeId) {
        super(SoftwareModuleType.class.getSimpleName() + " with id {" + moduleTypeId + "} is not part of "
                + DistributionSetType.class.getSimpleName() + " with id {" + distributionSetTypeId + "}.");
    }
}