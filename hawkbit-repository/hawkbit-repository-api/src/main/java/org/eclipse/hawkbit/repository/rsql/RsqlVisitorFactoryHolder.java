/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
