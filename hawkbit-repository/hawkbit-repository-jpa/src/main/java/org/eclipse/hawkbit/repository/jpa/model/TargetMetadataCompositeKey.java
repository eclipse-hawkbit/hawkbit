/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others
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
import java.util.Objects;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The Target Metadata composite key which contains the meta-data key and the ID of the Target itself.
 */
@NoArgsConstructor // Default constructor for JPA
@Data
public final class TargetMetadataCompositeKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String key;
    private Long target;

    /**
     * @param target the target Id for this meta-data
     * @param key the key of the meta-data
     */
    public TargetMetadataCompositeKey(final Long target, final String key) {
        this.target = target;
        this.key = key;
    }
}