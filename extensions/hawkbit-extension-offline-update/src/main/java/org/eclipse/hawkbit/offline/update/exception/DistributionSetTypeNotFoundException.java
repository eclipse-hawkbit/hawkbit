/**
 * Copyright (c) Siemens AG, 2017
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.offline.update.exception;

import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * The {@link DistributionSetTypeNotFoundException} is thrown when a
 * {@link DistributionSetType} is not found for a set of
 * {@link SoftwareModuleType}s.
 */
public class DistributionSetTypeNotFoundException extends EntityNotFoundException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor to create {@link DistributionSetTypeNotFoundException}.
     *
     * @param moduleTypeList
     *            list of keys for {@link SoftwareModuleType}s for which
     *            appropriate distribution set type was not found.
     */
    public DistributionSetTypeNotFoundException(String moduleTypeList) {
        super(String.format("No compatible distribution set type found for software module type(s): %s."
                + "Create new distribution set type including all module types.", moduleTypeList));
    }
}
