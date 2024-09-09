/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import lombok.Getter;

/**
 * Sort fields for SoftwareModuleMetadata.
 */
@Getter
public enum SoftwareModuleMetadataFields implements RsqlQueryField {

    KEY("key"),
    VALUE("value"),
    TARGETVISIBLE("targetVisible");

    private final String jpaEntityFieldName;

    SoftwareModuleMetadataFields(final String jpaEntityFieldName) {
        this.jpaEntityFieldName = jpaEntityFieldName;
    }

    @Override
    public String identifierFieldName() {
        return KEY.getJpaEntityFieldName();
    }
}
