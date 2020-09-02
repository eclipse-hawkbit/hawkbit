/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.mappers;

import org.eclipse.hawkbit.repository.model.Type;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;

/**
 * 
 * Use to map {@link Type} to {@link ProxyTypeInfo}
 *
 * @param <T>
 *            type of input type
 */
public class TypeToTypeInfoMapper<T extends Type>
        implements IdentifiableEntityToProxyIdentifiableEntityMapper<ProxyTypeInfo, T> {

    @Override
    public ProxyTypeInfo map(final T entity) {
        return new ProxyTypeInfo(entity.getId(), entity.getName(), entity.getKey());
    }

}
