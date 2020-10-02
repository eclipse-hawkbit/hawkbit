/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.tag;

import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.management.tag.TagWindowLayout;

/**
 * Abstract base class for update tag window controller.
 */
public abstract class AbstractTagWindowController extends AbstractEntityWindowController<ProxyTag, ProxyTag> {

    protected final Class<? extends ProxyIdentifiableEntity> parentType;
    protected final TagWindowLayout<ProxyTag> layout;

    AbstractTagWindowController(final CommonUiDependencies uiDependencies, final TagWindowLayout<ProxyTag> layout,
            final Class<? extends ProxyIdentifiableEntity> parentType) {
        super(uiDependencies);
        this.layout = layout;
        this.parentType = parentType;
    }

    protected abstract boolean existsEntityInRepository(String trimmedName);

    @Override
    public EntityWindowLayout<ProxyTag> getLayout() {
        return layout;
    }

}
