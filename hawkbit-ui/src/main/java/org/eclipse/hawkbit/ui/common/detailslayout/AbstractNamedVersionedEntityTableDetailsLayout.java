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
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 *
 *
 */
public abstract class AbstractNamedVersionedEntityTableDetailsLayout<T extends NamedVersionedEntity>
        extends AbstractTableDetailsLayout<T> {

    private static final long serialVersionUID = 1L;

    protected AbstractNamedVersionedEntityTableDetailsLayout(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker, final ManagementUIState managementUIState) {
        super(i18n, eventBus, permissionChecker, managementUIState);
    }

    @Override
    protected String getName() {
        return HawkbitCommonUtil.getFormattedNameVersion(getSelectedBaseEntity().getName(),
                getSelectedBaseEntity().getVersion());
    }
}
