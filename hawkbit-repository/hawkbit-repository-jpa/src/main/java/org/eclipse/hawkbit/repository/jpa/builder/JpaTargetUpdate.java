/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.builder.AbstractTargetUpdateCreate;
import org.eclipse.hawkbit.repository.builder.TargetUpdate;

public class JpaTargetUpdate extends AbstractTargetUpdateCreate<TargetUpdate> implements TargetUpdate {

    JpaTargetUpdate(final String controllerId) {
        super(controllerId);
    }

}
