/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serial;
import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite key for {@link DistributionSetTypeElement}.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Data
@Embeddable
public class DistributionSetTypeElementCompositeKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "distribution_set_type", nullable = false, updatable = false)
    private Long dsType;

    @Column(name = "software_module_type", nullable = false, updatable = false)
    private Long smType;

    DistributionSetTypeElementCompositeKey(final JpaDistributionSetType dsType, final JpaSoftwareModuleType smType) {
        this.dsType = dsType.getId();
        this.smType = smType.getId();
    }
}