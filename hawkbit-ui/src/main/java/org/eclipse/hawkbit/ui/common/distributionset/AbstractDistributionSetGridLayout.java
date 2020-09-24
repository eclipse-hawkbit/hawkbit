/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.distributionset;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.layout.AbstractGridComponentLayout;
import org.eclipse.hawkbit.ui.common.layout.listener.TopicEventListener;
import org.eclipse.hawkbit.ui.distributions.dstable.DsMetaDataWindowBuilder;
import org.eclipse.hawkbit.ui.distributions.dstable.DsWindowBuilder;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Abstract base class for distribution set grid layouts.
 */
public abstract class AbstractDistributionSetGridLayout extends AbstractGridComponentLayout {
    private static final long serialVersionUID = 1L;

    private final transient Set<TopicEventListener> listeners = new HashSet<>();
    private final DsWindowBuilder dsWindowBuilder;
    private final DsMetaDataWindowBuilder dsMetaDataWindowBuilder;
    private final EventView eventView;

    /**
     * Constructor for AbstractDistributionSetGridLayout
     *
     * @param i18n
     *            VaadinMessageSource
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param notification
     *            UINotification
     * @param systemManagement
     *            SystemManagement
     * @param systemSecurityContext
     *            SystemSecurityContext
     * @param configManagement
     *            TenantConfigurationManagement
     * @param distributionSetManagement
     *            DistributionSetManagement
     * @param distributionSetTypeManagement
     *            DistributionSetTypeManagement
     * @param permissionChecker
     *            SpPermissionChecker
     * @param eventView
     *            EventView
     */
    public AbstractDistributionSetGridLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final UINotification notification, final SystemManagement systemManagement,
            final SystemSecurityContext systemSecurityContext, final TenantConfigurationManagement configManagement,
            final DistributionSetManagement distributionSetManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement,
            final SpPermissionChecker permissionChecker, final EventView eventView) {

        this.eventView = eventView;

        dsWindowBuilder = new DsWindowBuilder(i18n, entityFactory, eventBus, notification, systemManagement,
                systemSecurityContext, configManagement, distributionSetManagement, distributionSetTypeManagement,
                EventView.DEPLOYMENT);
        dsMetaDataWindowBuilder = new DsMetaDataWindowBuilder(i18n, entityFactory, eventBus, notification,
                permissionChecker, distributionSetManagement);
    }

    /**
     * Returns the {@link DsMetaDataWindowBuilder}.
     *
     * @return the dsMetaDataWindowBuilder
     */
    public DsMetaDataWindowBuilder getDsMetaDataWindowBuilder() {
        return dsMetaDataWindowBuilder;
    }

    /**
     * Returns the {@link DsWindowBuilder}.
     *
     * @return the dsWindowBuilder
     */
    public DsWindowBuilder getDsWindowBuilder() {
        return dsWindowBuilder;
    }

    /**
     * Returns the {@link EventView}
     *
     * @return the eventView
     */
    public EventView getEventView() {
        return eventView;
    }

    /**
     * Returns the {@link AbstractDsGrid}
     *
     * @return the distributionGrid
     */
    public abstract AbstractDsGrid<?> getDistributionGrid();

    /**
     * Returns the {@link DistributionSetGridHeader}
     *
     * @return the distributionGridHeader
     */
    public abstract DistributionSetGridHeader getDistributionSetGridHeader();

    /**
     * Show distribution set filter header icon
     */
    public void showDsFilterHeaderIcon() {
        getDistributionSetGridHeader().showFilterIcon();
    }

    /**
     * Hide distribution set filter header icon
     */
    public void hideDsFilterHeaderIcon() {
        getDistributionSetGridHeader().hideFilterIcon();
    }

    /**
     * Maximize the distribution grid
     */
    public void maximize() {
        getDistributionGrid().createMaximizedContent();
        hideDetailsLayout();
    }

    /**
     * Minimize the distribution grid
     */
    public void minimize() {
        getDistributionGrid().createMinimizedContent();
        showDetailsLayout();
    }

    /**
     * Restore the distribution grid state
     */
    public void restoreState() {
        getDistributionSetGridHeader().restoreState();
        getDistributionGrid().restoreState();
    }

    protected void addEventListener(final TopicEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Unsubscribe the event listeners.
     */
    public void unsubscribeListener() {
        listeners.forEach(listener -> listener.unsubscribe());
    }
}
