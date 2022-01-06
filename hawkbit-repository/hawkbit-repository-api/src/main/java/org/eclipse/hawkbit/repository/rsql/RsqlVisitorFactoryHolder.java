/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.rsql;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Helper class providing static access to the managed
 * {@link RsqlVisitorFactory} bean.
 */
public final class RsqlVisitorFactoryHolder {

    private static final RsqlVisitorFactoryHolder SINGLETON = new RsqlVisitorFactoryHolder();

    @Autowired
    private RsqlVisitorFactory rsqlVisitorFactory;

    private RsqlVisitorFactoryHolder() {

    }

    /**
     * @return The holder singleton instance.
     */
    public static RsqlVisitorFactoryHolder getInstance() {
        return SINGLETON;
    }

    /**
     * @return The managed RsqlVisitorFactory bean
     */
    public RsqlVisitorFactory getRsqlVisitorFactory() {
        return rsqlVisitorFactory;
    }

}
