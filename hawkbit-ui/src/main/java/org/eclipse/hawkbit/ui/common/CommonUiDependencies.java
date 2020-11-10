/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Container for commonly used dependencies in the UI.
 */
public class CommonUiDependencies {

    private final VaadinMessageSource i18n;
    private final EntityFactory entityFactory;
    private final UIEventBus eventBus;
    private final UINotification uiNotification;
    private final SpPermissionChecker permChecker;

    /**
     * Public constructor.
     *
     * @param i18n
     *            VaadinMessageSource
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param uiNotification
     *            UINotification
     * @param permChecker
     *            {@link SpPermissionChecker}
     */
    public CommonUiDependencies(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final UINotification uiNotification, final SpPermissionChecker permChecker) {
        super();
        this.i18n = i18n;
        this.entityFactory = entityFactory;
        this.eventBus = eventBus;
        this.uiNotification = uiNotification;
        this.permChecker = permChecker;
    }

    /**
     * Returns {@link VaadinMessageSource}
     *
     * @return the i18n
     */
    public VaadinMessageSource getI18n() {
        return i18n;
    }

    /**
     * Returns {@link EntityFactory}
     *
     * @return the entityFactory
     */
    public EntityFactory getEntityFactory() {
        return entityFactory;
    }

    /**
     * Returns {@link UIEventBus}
     *
     * @return the eventBus
     */
    public UIEventBus getEventBus() {
        return eventBus;
    }

    /**
     * Returns {@link UINotification}
     *
     * @return the uiNotification
     */
    public UINotification getUiNotification() {
        return uiNotification;
    }

    /**
     * Returns {@link SpPermissionChecker}
     *
     * @return the permChecker
     */
    public SpPermissionChecker getPermChecker() {
        return permChecker;
    }

}
