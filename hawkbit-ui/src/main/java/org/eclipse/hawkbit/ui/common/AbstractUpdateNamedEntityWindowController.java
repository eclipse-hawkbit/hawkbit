/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyNamedEntity;

/**
 * Window controller for entity updates.
 *
 * @param <T>
 *            Type of proxy entity
 * @param <E>
 *            Second type of proxy entity
 * @param <R>
 *            Type of repository entity
 */
public abstract class AbstractUpdateNamedEntityWindowController<T, E extends ProxyNamedEntity, R extends NamedEntity>
        extends AbstractUpdateEntityWindowController<T, E, R> {

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     */
    protected AbstractUpdateNamedEntityWindowController(final CommonUiDependencies uiDependencies) {
        super(uiDependencies);
    }

    @Override
    protected String getDisplayableName(final R entity) {
        return entity.getName();
    }

    @Override
    protected String getDisplayableNameForFailedMessage(final E entity) {
        return entity.getName();
    }

    @Override
    protected Long getId(final R entity) {
        return entity.getId();
    }
}
