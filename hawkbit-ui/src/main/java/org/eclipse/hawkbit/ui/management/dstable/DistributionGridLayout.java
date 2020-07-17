/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.detailslayout.DistributionSetDetails;
import org.eclipse.hawkbit.ui.common.detailslayout.DistributionSetDetailsHeader;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventLayoutViewAware;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.EventViewAware;
import org.eclipse.hawkbit.ui.common.layout.AbstractGridComponentLayout;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener.EntityModifiedAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.FilterChangedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.PinningChangedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.SelectionChangedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedGridRefreshAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedPinAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedSelectionAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedTagTokenAwareSupport;
import org.eclipse.hawkbit.ui.common.state.TagFilterLayoutUiState;
import org.eclipse.hawkbit.ui.distributions.dstable.DistributionSetGridHeader;
import org.eclipse.hawkbit.ui.distributions.dstable.DsMetaDataWindowBuilder;
import org.eclipse.hawkbit.ui.distributions.dstable.DsWindowBuilder;
import org.eclipse.hawkbit.ui.management.targettable.TargetGridLayoutUiState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Distribution Set table layout which is shown on the Distribution View
 */
public class DistributionGridLayout extends AbstractGridComponentLayout {
    private static final long serialVersionUID = 1L;

    private final DistributionSetGridHeader distributionGridHeader;
    private final DistributionGrid distributionGrid;
    private final DistributionSetDetailsHeader distributionSetDetailsHeader;
    private final DistributionSetDetails distributionDetails;

    private final transient FilterChangedListener<ProxyDistributionSet> dsFilterListener;
    private final transient PinningChangedListener<String> pinningChangedListener;
    private final transient SelectionChangedListener<ProxyDistributionSet> masterDsChangedListener;
    private final transient EntityModifiedListener<ProxyDistributionSet> dsModifiedListener;
    private final transient EntityModifiedListener<ProxyTag> tagModifiedListener;

    /**
     * Constructor for DistributionGridLayout
     *
     * @param eventBus
     *          UIEventBus
     * @param i18n
     *          VaadinMessageSource
     * @param permissionChecker
     *          SpPermissionChecker
     * @param entityFactory
     *          EntityFactory
     * @param notification
     *          UINotification
     * @param targetManagement
     *          TargetManagement
     * @param distributionSetManagement
     *          DistributionSetManagement
     * @param smManagement
     *          SoftwareModuleManagement
     * @param distributionSetTypeManagement
     *          DistributionSetTypeManagement
     * @param distributionSetTagManagement
     *          DistributionSetTagManagement
     * @param systemManagement
     *          SystemManagement
     * @param deploymentManagement
     *          DeploymentManagement
     * @param configManagement
     *          TenantConfigurationManagement
     * @param systemSecurityContext
     *          SystemSecurityContext
     * @param uiProperties
     *          UiProperties
     * @param distributionGridLayoutUiState
     *          DistributionGridLayoutUiState
     * @param distributionTagLayoutUiState
     *          TagFilterLayoutUiState
     * @param targetGridLayoutUiState
     *          TargetGridLayoutUiState
     */
    public DistributionGridLayout(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker, final EntityFactory entityFactory,
            final UINotification notification, final TargetManagement targetManagement,
            final DistributionSetManagement distributionSetManagement, final SoftwareModuleManagement smManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement,
            final DistributionSetTagManagement distributionSetTagManagement, final SystemManagement systemManagement,
            final DeploymentManagement deploymentManagement, final TenantConfigurationManagement configManagement,
            final SystemSecurityContext systemSecurityContext, final UiProperties uiProperties,
            final DistributionGridLayoutUiState distributionGridLayoutUiState,
            final TagFilterLayoutUiState distributionTagLayoutUiState,
            final TargetGridLayoutUiState targetGridLayoutUiState) {

        final DsWindowBuilder dsWindowBuilder = new DsWindowBuilder(i18n, entityFactory, eventBus, notification,
                systemManagement, systemSecurityContext, configManagement, distributionSetManagement,
                distributionSetTypeManagement, EventView.DEPLOYMENT);
        final DsMetaDataWindowBuilder dsMetaDataWindowBuilder = new DsMetaDataWindowBuilder(i18n, entityFactory,
                eventBus, notification, permissionChecker, distributionSetManagement);

        this.distributionGridHeader = new DistributionSetGridHeader(i18n, permissionChecker, eventBus,
                distributionTagLayoutUiState, distributionGridLayoutUiState, EventLayout.DS_TAG_FILTER,
                EventView.DEPLOYMENT);
        this.distributionGridHeader.buildHeader();
        this.distributionGrid = new DistributionGrid(eventBus, i18n, permissionChecker, notification, targetManagement,
                distributionSetManagement, deploymentManagement, uiProperties, distributionGridLayoutUiState,
                targetGridLayoutUiState, distributionTagLayoutUiState);

        this.distributionSetDetailsHeader = new DistributionSetDetailsHeader(i18n, permissionChecker, eventBus,
                notification, dsWindowBuilder, dsMetaDataWindowBuilder);
        this.distributionDetails = new DistributionSetDetails(i18n, eventBus, permissionChecker, notification,
                distributionSetManagement, smManagement, distributionSetTypeManagement, distributionSetTagManagement,
                configManagement, systemSecurityContext, dsMetaDataWindowBuilder);
        this.distributionDetails.setUnassignSmAllowed(false);
        this.distributionDetails.buildDetails();

        this.dsFilterListener = new FilterChangedListener<>(eventBus, ProxyDistributionSet.class,
                new EventViewAware(EventView.DEPLOYMENT), distributionGrid.getFilterSupport());
        this.pinningChangedListener = new PinningChangedListener<>(eventBus, ProxyTarget.class,
                distributionGrid.getPinSupport());
        this.masterDsChangedListener = new SelectionChangedListener<>(eventBus,
                new EventLayoutViewAware(EventLayout.DS_LIST, EventView.DEPLOYMENT), getMasterDsAwareComponents());
        this.dsModifiedListener = new EntityModifiedListener.Builder<>(eventBus, ProxyDistributionSet.class)
                .entityModifiedAwareSupports(getDsModifiedAwareSupports()).build();
        this.tagModifiedListener = new EntityModifiedListener.Builder<>(eventBus, ProxyTag.class)
                .entityModifiedAwareSupports(getTagModifiedAwareSupports()).parentEntityType(ProxyDistributionSet.class)
                .build();

        buildLayout(distributionGridHeader, distributionGrid, distributionSetDetailsHeader, distributionDetails);
    }

    private List<MasterEntityAwareComponent<ProxyDistributionSet>> getMasterDsAwareComponents() {
        return Arrays.asList(distributionSetDetailsHeader, distributionDetails);
    }

    private List<EntityModifiedAwareSupport> getDsModifiedAwareSupports() {
        return Arrays.asList(EntityModifiedGridRefreshAwareSupport.of(distributionGrid::refreshAll),
                EntityModifiedSelectionAwareSupport.of(distributionGrid.getSelectionSupport(),
                        distributionGrid::mapIdToProxyEntity, this::isIncomplete),
                EntityModifiedPinAwareSupport.of(distributionGrid.getPinSupport(), distributionGrid::mapIdToProxyEntity,
                        this::isIncomplete));
    }

    private boolean isIncomplete(final ProxyDistributionSet ds) {
        return ds != null && !ds.getIsComplete();
    }

    private List<EntityModifiedAwareSupport> getTagModifiedAwareSupports() {
        return Collections
                .singletonList(EntityModifiedTagTokenAwareSupport.of(distributionDetails.getDistributionTagToken()));
    }

    /**
     * Show distribution set tag header icon
     */
    public void showDsTagHeaderIcon() {
        distributionGridHeader.showFilterIcon();
    }

    /**
     * Hide distribution set tag header icon
     */
    public void hideDsTagHeaderIcon() {
        distributionGridHeader.hideFilterIcon();
    }

    /**
     * Maximize the distribution grid
     */
    public void maximize() {
        distributionGrid.createMaximizedContent();
        hideDetailsLayout();
    }

    /**
     * Minimize the distribution grid
     */
    public void minimize() {
        distributionGrid.createMinimizedContent();
        showDetailsLayout();
    }

    /**
     * Restore the distribution grid state
     */
    public void restoreState() {
        distributionGridHeader.restoreState();
        distributionGrid.restoreState();
    }

    /**
     * Unsubscribe the changed listener
     */
    public void unsubscribeListener() {
        dsFilterListener.unsubscribe();
        pinningChangedListener.unsubscribe();
        masterDsChangedListener.unsubscribe();
        dsModifiedListener.unsubscribe();
        tagModifiedListener.unsubscribe();
    }
}
