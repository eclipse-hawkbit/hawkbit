/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.mappers;

import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;

/**
 * Interface for mapping identifiable entities, fetched from backend, to the
 * proxy identifiable entities.
 *
 * @param <T>
 *          Generic type of ProxyIdentifiableEntity
 * @param <U>
 *          Generic type of Identifiable
 */
@FunctionalInterface
public interface IdentifiableEntityToProxyIdentifiableEntityMapper<T extends ProxyIdentifiableEntity, U extends Identifiable<Long>> {

    /**
     * Maps the provided {@link Identifiable} loaded from the backend to a new
     * instance of {@link ProxyIdentifiableEntity}.
     * 
     * @param identifiableEntity
     *            the {@link Identifiable} to map
     * @return a new instance of {@link ProxyIdentifiableEntity}
     */
    T map(U identifiableEntity);
}
