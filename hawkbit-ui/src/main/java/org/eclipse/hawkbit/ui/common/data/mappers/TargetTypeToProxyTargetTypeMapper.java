/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.mappers;

import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;

/**
 * Maps {@link Tag} entities, fetched from backend, to the {@link ProxyTag}
 * entities.
 *
 * @param <T>
 *          Generic type of Tag
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
