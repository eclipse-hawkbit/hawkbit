/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Composite key for {@link DistributionSetTypeElement}.
 *
 *
 *
 *
 */
@Embeddable
public class DistributionSetTypeElementCompositeKey implements Serializable {

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    @Column(name = "distribution_set_type", nullable = false)
    private Long dsType;

    @Column(name = "software_module_type", nullable = false)
    private Long smType;

    /**
     * Default constructor.
     */
    DistributionSetTypeElementCompositeKey() {
    }

    /**
     * Constructor.
     *
     * @param dsType
     *            in the key
     * @param smType
     *            in the key
     */
    DistributionSetTypeElementCompositeKey(final DistributionSetType dsType, final SoftwareModuleType smType) {
        super();
        this.dsType = dsType.getId();
        this.smType = smType.getId();
    }

    /**
     * @return the dsType
     */
    public Long getDsType() {
        return dsType;
    }

    /**
     * @param dsType
     *            the dsType to set
     */
    public void setDsType(final Long dsType) {
        this.dsType = dsType;
    }

    /**
     * @return the smType
     */
    public Long getSmType() {
        return smType;
    }

    /**
     * @param smType
     *            the smType to set
     */
    public void setSmType(final Long smType) {
        this.smType = smType;
    }

}
