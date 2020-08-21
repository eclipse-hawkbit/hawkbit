/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.Window;

/**
 * Builder for auto assignment window
 */
public class AutoAssignmentWindowBuilder extends AbstractEntityWindowBuilder<ProxyTargetFilterQuery> {
    private final UIEventBus eventBus;
    private final UINotification uiNotification;
    private final EntityFactory entityFactory;

    private final TargetManagement targetManagement;
    private final TargetFilterQueryManagement targetFilterQueryManagement;
    private final DistributionSetManagement dsManagement;

    /**
     * Constructor for AutoAssignmentWindowBuilder
     *
     * @param i18n
     *          VaadinMessageSource
     * @param entityFactory
     *          EntityFactory
     * @param eventBus
     *          UIEventBus
     * @param uiNotification
     *          UINotification
     * @param targetManagement
     *          TargetManagement
     * @param targetFilterQueryManagement
     *          TargetFilterQueryManagement
     * @param dsManagement
     *          DistributionSetManagement
     */
    public AutoAssignmentWindowBuilder(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final UINotification uiNotification, final EntityFactory entityFactory,
            final TargetManagement targetManagement, final TargetFilterQueryManagement targetFilterQueryManagement,
            final DistributionSetManagement dsManagement) {
        super(i18n);

        this.eventBus = eventBus;
        this.uiNotification = uiNotification;
        this.entityFactory = entityFactory;

        this.targetManagement = targetManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.dsManagement = dsManagement;
    }

    @Override
    protected String getWindowId() {
        return UIComponentIdProvider.DIST_SET_SELECT_WINDOW_ID;
    }

    /**
     * Gets the auto assigment window
     *
     * @param proxyTargetFilter
     *          ProxyTargetFilterQuery
     *
     * @return  Common dialog window
     */
    public Window getWindowForAutoAssignment(final ProxyTargetFilterQuery proxyTargetFilter) {
        return getWindowForEntity(proxyTargetFilter,
                new AutoAssignmentWindowController(i18n, eventBus, uiNotification, entityFactory, targetManagement,
                        targetFilterQueryManagement, new AutoAssignmentWindowLayout(i18n, dsManagement)));
    }

    @Override
    public Window getWindowForAdd() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Window getWindowForUpdate(final ProxyTargetFilterQuery entity) {
        throw new UnsupportedOperationException();
    }
}
