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

import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;

/**
 * Maps {@link TargetType} entities, fetched from backend, to the {@link ProxyTargetType}
 * entities.
 *
 * @param <T>
 *          Generic type of TargetType
 */
public class TargetTypeToProxyTargetTypeMapper<T extends TargetType> extends AbstractNamedEntityToProxyNamedEntityMapper<ProxyTargetType, T> {

    @Override
    public ProxyTargetType map(final T targetType) {
        final ProxyTargetType proxyTargetType = new ProxyTargetType();

        mapNamedEntityAttributes(targetType, proxyTargetType);

        proxyTargetType.setColour(targetType.getColour());

        return proxyTargetType;
    }

}
