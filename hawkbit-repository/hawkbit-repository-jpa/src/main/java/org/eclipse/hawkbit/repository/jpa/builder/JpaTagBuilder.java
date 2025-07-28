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

import org.eclipse.hawkbit.repository.builder.GenericTagUpdate;
import org.eclipse.hawkbit.repository.builder.TagBuilder;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.builder.TagUpdate;
import org.eclipse.hawkbit.repository.jpa.model.JpaTag;
import org.eclipse.hawkbit.repository.model.Tag;

/**
 * Builder implementation for {@link Tag}.
 */
public class JpaTagBuilder implements TagBuilder<JpaTag> {

    @Override
    public TagUpdate update(final long id) {
        return new GenericTagUpdate(id);
    }

    @Override
    public TagCreate<JpaTag> create() {
        return new JpaTagCreate();
    }
}