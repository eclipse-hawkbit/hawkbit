/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
