/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.mappers;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyNamedEntity;

/**
 * Interface for mapping named entities, fetched from backend, to the proxy
 * named entities.
 *
 * @param <T>
 *          Generic type of ProxyNamedEntity
 * @param <U>
 *          Generic type of NamedEntity
 */
public interface NamedEntityToProxyNamedEntityMapper<T extends ProxyNamedEntity, U extends NamedEntity>
        extends IdentifiableEntityToProxyIdentifiableEntityMapper<T, U> {

    /**
     * Maps the provided {@link NamedEntity} loaded from the backend to a new
     * instance of {@link ProxyNamedEntity}.
     * 
     * @param namedEntity
     *            the {@link NamedEntity} to map
     * @return a new instance of {@link ProxyNamedEntity}
     */
    @Override
    T map(U namedEntity);
}
