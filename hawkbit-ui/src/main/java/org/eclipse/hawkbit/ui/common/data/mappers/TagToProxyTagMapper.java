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

import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;

/**
 * Maps {@link Tag} entities, fetched from backend, to the {@link ProxyTag}
 * entities.
 *
 * @param <T>
 *          Generic type of Tag
 */
public class TagToProxyTagMapper<T extends Tag> extends AbstractNamedEntityToProxyNamedEntityMapper<ProxyTag, T> {

    @Override
    public ProxyTag map(final T tag) {
        final ProxyTag proxyTag = new ProxyTag();

        mapNamedEntityAttributes(tag, proxyTag);

        proxyTag.setColour(tag.getColour());

        return proxyTag;
    }

}
