/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.builder.AbstractSoftwareModuleTypeUpdateCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;

/**
 * Create/build implementation.
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class JpaSoftwareModuleTypeCreate extends AbstractSoftwareModuleTypeUpdateCreate<SoftwareModuleTypeCreate>
        implements SoftwareModuleTypeCreate {

    @Override
    public JpaSoftwareModuleType build() {
        return new JpaSoftwareModuleType(key, name, description, maxAssignments, colour);
    }
}