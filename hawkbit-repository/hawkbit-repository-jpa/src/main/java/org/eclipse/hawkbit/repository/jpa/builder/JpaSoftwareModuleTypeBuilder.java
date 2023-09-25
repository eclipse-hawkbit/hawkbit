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

import org.eclipse.hawkbit.repository.builder.GenericSoftwareModuleTypeUpdate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeBuilder;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeUpdate;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Builder implementation for {@link SoftwareModuleType}.
 *
 */
public class JpaSoftwareModuleTypeBuilder implements SoftwareModuleTypeBuilder {

    @Override
    public SoftwareModuleTypeUpdate update(final long id) {
        return new GenericSoftwareModuleTypeUpdate(id);
    }

    @Override
    public SoftwareModuleTypeCreate create() {
        return new JpaSoftwareModuleTypeCreate();
    }

}
