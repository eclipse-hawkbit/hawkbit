/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;

/**
 *
 *
 */
public abstract class AbstractNamedVersionedEntityTableDetailsLayout<T extends NamedVersionedEntity>
        extends AbstractTableDetailsLayout<T> {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getName() {
        return HawkbitCommonUtil.getFormattedNameVersion(selectedBaseEntity.getName(), selectedBaseEntity.getVersion());
    }

}
