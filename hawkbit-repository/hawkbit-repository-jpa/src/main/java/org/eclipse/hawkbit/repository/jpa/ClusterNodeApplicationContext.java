/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 *
 */
@Service
public class ClusterNodeApplicationContext implements ClusterNodeContext {

    private final ApplicationContext applicationContext;

    /**
     * @param applicationContext
     */
    @Autowired
    public ClusterNodeApplicationContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public String getNodeId() {
        return applicationContext.getId();
    }

}
