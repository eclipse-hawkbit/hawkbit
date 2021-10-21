/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.mappers;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;

/**
 * 
 * Use to map {@link NamedEntity} to {@link ProxyTypeInfo}
 *
 */
public class TargetTypeToTypeInfoMapper implements IdentifiableEntityToProxyIdentifiableEntityMapper<ProxyTypeInfo, TargetType> {

    @Override
    public ProxyTypeInfo map(TargetType entity) {
        return new ProxyTypeInfo(entity.getId(), entity.getName());
    }
}
