/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.builder.GenericTagUpdate;
import org.eclipse.hawkbit.repository.builder.TagBuilder;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.builder.TagUpdate;

/**
 * Builder implementation for {@link Tag}.
 *
 */
public class JpaTagBuilder implements TagBuilder {

    @Override
    public TagUpdate update(final long id) {
        return new GenericTagUpdate(id);
    }

    @Override
    public TagCreate create() {
        return new JpaTagCreate();
    }

}
