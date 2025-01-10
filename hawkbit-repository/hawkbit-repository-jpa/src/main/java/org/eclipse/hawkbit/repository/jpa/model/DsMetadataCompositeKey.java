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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The DistributionSet Metadata composite key which contains the meta-data key and the ID of the DistributionSet itself.
 */
@NoArgsConstructor // Default constructor for JPA
@Setter
@Getter
public final class DsMetadataCompositeKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String key;
    private Long distributionSet;

    /**
     * @param distributionSet the distribution set for this meta data
     * @param key the key of the meta data
     */
    public DsMetadataCompositeKey(final Long distributionSet, final String key) {
        this.distributionSet = distributionSet;
        this.key = key;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (distributionSet == null ? 0 : distributionSet.hashCode());
        result = prime * result + (key == null ? 0 : key.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DsMetadataCompositeKey other = (DsMetadataCompositeKey) obj;
        if (distributionSet == null) {
            if (other.distributionSet != null) {
                return false;
            }
        } else if (!distributionSet.equals(other.distributionSet)) {
            return false;
        }
        if (key == null) {
            return other.key == null;
        } else {
            return key.equals(other.key);
        }
    }
}