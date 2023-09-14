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

import org.eclipse.hawkbit.repository.model.Type;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;

/**
 * Maps {@link Type} entities, fetched from backend, to the {@link ProxyType}
 * entities.
 * 
 * @param <T>
 *            type of input type (software module type or distribution set type)
 */
public class TypeToProxyTypeMapper<T extends Type> extends AbstractNamedEntityToProxyNamedEntityMapper<ProxyType, T> {

    @Override
    public ProxyType map(final T type) {
        final ProxyType proxyType = new ProxyType();

        mapNamedEntityAttributes(type, proxyType);

        proxyType.setKey(type.getKey());
        proxyType.setColour(type.getColour());
        proxyType.setDeleted(type.isDeleted());

        return proxyType;
    }

}
