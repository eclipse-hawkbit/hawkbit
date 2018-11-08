/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * 
 * Abstract pop up layout
 *
 * @param <E>
 *            E id the entity for which metadata is displayed
 * @param <M>
 *            M is the metadata
 * 
 */
public abstract class AbstractMetadataPopupLayoutVersioned<E extends NamedVersionedEntity, M extends MetaData>
        extends AbstractMetadataPopupLayout<E, M> {

    private static final long serialVersionUID = 1L;

    protected AbstractMetadataPopupLayoutVersioned(final VaadinMessageSource i18n, final UINotification uiNotification,
            final UIEventBus eventBus, final SpPermissionChecker permChecker) {
        super(i18n, uiNotification, eventBus, permChecker);
    }

    @Override
    protected String getElementTitle() {
        return HawkbitCommonUtil.getFormattedNameVersion(getSelectedEntity().getName(),
                getSelectedEntity().getVersion());
    }
}
