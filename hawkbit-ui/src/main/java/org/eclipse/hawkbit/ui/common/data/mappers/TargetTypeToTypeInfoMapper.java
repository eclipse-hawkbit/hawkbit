/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
