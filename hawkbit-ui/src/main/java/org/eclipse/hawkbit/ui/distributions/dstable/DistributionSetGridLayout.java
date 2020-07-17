/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
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
import org.eclipse.hawkbit.ui.common.layout.listener.SelectGridEntityListener;
import org.eclipse.hawkbit.ui.common.layout.listener.SelectionChangedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedGridRefreshAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedSelectionAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedTagTokenAwareSupport;
import org.eclipse.hawkbit.ui.common.state.GridLayoutUiState;
import org.eclipse.hawkbit.ui.common.state.TypeFilterLayoutUiState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * DistributionSet table layout.
 */
public class DistributionSetGridLayout extends AbstractGridComponentLayout {
    private static final long serialVersionUID = 1L;

    private final DistributionSetGridHeader distributionSetGridHeader;
    private final DistributionSetGrid distributionSetGrid;
    private final DistributionSetDetailsHeader distributionSetDetailsHeader;
    private final DistributionSetDetails distributionSetDetails;

    private final transient FilterChangedListener<ProxyDistributionSet> dsFilterListener;
    private final transient SelectionChangedListener<ProxyDistributionSet> masterDsChangedListener;
    private final transient SelectGridEntityListener<ProxyDistributionSet> selectDsListener;
    private final transient EntityModifiedListener<ProxyDistributionSet> dsModifiedListener;
    private final transient EntityModifiedListener<ProxyTag> tagModifiedListener;

    /**
     * Constructor for DistributionSetGridLayout
     *
     * @param i18n
     *         VaadinMessageSource
     * @param eventBus
     *          UIEventBus
     * @param permissionChecker
     *          SpPermissionChecker
     * @param uiNotification
     *          UINotification
     * @param entityFactory
     *          EntityFactory
     * @param targetManagement
     *          TargetManagement
     * @param targetFilterQueryManagement
     *          TargetFilterQueryManagement
     * @param distributionSetManagement
     *          DistributionSetManagement
     * @param smManagement
     *          SoftwareModuleManagement
     * @param distributionSetTypeManagement
     *          DistributionSetTypeManagement
     * @param distributionSetTagManagement
     *          DistributionSetTagManagement
     * @param smTypeManagement
     *          SoftwareModuleTypeManagement
     * @param systemManagement
     *          SystemManagement
     * @param configManagement
     *          TenantConfigurationManagement
     * @param systemSecurityContext
     *          SystemSecurityContext
     * @param dSTypeFilterLayoutUiState
     *          TypeFilterLayoutUiState
     * @param distributionSetGridLayoutUiState
     *          GridLayoutUiState
     */
    public DistributionSetGridLayout(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final SpPermissionChecker permissionChecker, final UINotification uiNotification,
            final EntityFactory entityFactory, final TargetManagement targetManagement,
            final TargetFilterQueryManagement targetFilterQueryManagement,
            final DistributionSetManagement distributionSetManagement, final SoftwareModuleManagement smManagement,
            final DistributionSetTypeManagement distributionSetTypeManagement,
            final DistributionSetTagManagement distributionSetTagManagement,
            final SoftwareModuleTypeManagement smTypeManagement, final SystemManagement systemManagement,
            final TenantConfigurationManagement configManagement, final SystemSecurityContext systemSecurityContext,
            final TypeFilterLayoutUiState dSTypeFilterLayoutUiState,
            final GridLayoutUiState distributionSetGridLayoutUiState) {
        final DsWindowBuilder dsWindowBuilder = new DsWindowBuilder(i18n, entityFactory, eventBus, uiNotification,
                systemManagement, systemSecurityContext, configManagement, distributionSetManagement,
                distributionSetTypeManagement, EventView.DISTRIBUTIONS);
        final DsMetaDataWindowBuilder dsMetaDataWindowBuilder = new DsMetaDataWindowBuilder(i18n, entityFactory,
                eventBus, uiNotification, permissionChecker, distributionSetManagement);

        this.distributionSetGridHeader = new DistributionSetGridHeader(i18n, permissionChecker, eventBus,
                dSTypeFilterLayoutUiState, distributionSetGridLayoutUiState, EventLayout.DS_TYPE_FILTER,
                EventView.DISTRIBUTIONS);
        this.distributionSetGridHeader.addAddHeaderSupport(dsWindowBuilder);
        this.distributionSetGridHeader.buildHeader();
        this.distributionSetGrid = new DistributionSetGrid(eventBus, i18n, permissionChecker, uiNotification,
                targetManagement, distributionSetManagement, smManagement, distributionSetTypeManagement,
                smTypeManagement, dSTypeFilterLayoutUiState, distributionSetGridLayoutUiState);

        this.distributionSetDetailsHeader = new DistributionSetDetailsHeader(i18n, permissionChecker, eventBus,
                uiNotification, dsWindowBuilder, dsMetaDataWindowBuilder);
        this.distributionSetDetails = new DistributionSetDetails(i18n, eventBus, permissionChecker, uiNotification,
                distributionSetManagement, smManagement, distributionSetTypeManagement, distributionSetTagManagement,
                configManagement, systemSecurityContext, dsMetaDataWindowBuilder);
        this.distributionSetDetails.setUnassignSmAllowed(true);
        this.distributionSetDetails.addTfqDetailsGrid(targetFilterQueryManagement);
        this.distributionSetDetails.buildDetails();

        this.dsFilterListener = new FilterChangedListener<>(eventBus, ProxyDistributionSet.class,
                new EventViewAware(EventView.DISTRIBUTIONS), distributionSetGrid.getFilterSupport());
        this.masterDsChangedListener = new SelectionChangedListener<>(eventBus,
                new EventLayoutViewAware(EventLayout.DS_LIST, EventView.DISTRIBUTIONS), getDsEntityAwareComponents());
        this.selectDsListener = new SelectGridEntityListener<>(eventBus,
                new EventLayoutViewAware(EventLayout.DS_LIST, EventView.DISTRIBUTIONS),
                distributionSetGrid.getSelectionSupport());
        this.dsModifiedListener = new EntityModifiedListener.Builder<>(eventBus, ProxyDistributionSet.class)
                .entityModifiedAwareSupports(getDsModifiedAwareSupports()).build();
        this.tagModifiedListener = new EntityModifiedListener.Builder<>(eventBus, ProxyTag.class)
                .entityModifiedAwareSupports(getTagModifiedAwareSupports()).parentEntityType(ProxyDistributionSet.class)
                .build();

        buildLayout(distributionSetGridHeader, distributionSetGrid, distributionSetDetailsHeader,
                distributionSetDetails);
    }

    private List<MasterEntityAwareComponent<ProxyDistributionSet>> getDsEntityAwareComponents() {
        return Arrays.asList(distributionSetDetailsHeader, distributionSetDetails);
    }

    private List<EntityModifiedAwareSupport> getDsModifiedAwareSupports() {
        return Arrays.asList(EntityModifiedGridRefreshAwareSupport.of(distributionSetGrid::refreshAll),
                EntityModifiedSelectionAwareSupport.of(distributionSetGrid.getSelectionSupport(),
                        distributionSetGrid::mapIdToProxyEntity));
    }

    private List<EntityModifiedAwareSupport> getTagModifiedAwareSupports() {
        return Collections
                .singletonList(EntityModifiedTagTokenAwareSupport.of(distributionSetDetails.getDistributionTagToken()));
    }

    /**
     * Show distribution set type header icon
     */
    public void showDsTypeHeaderIcon() {
        distributionSetGridHeader.showFilterIcon();
    }

    /**
     * Hide distribution set type header icon
     */
    public void hideDsTypeHeaderIcon() {
        distributionSetGridHeader.hideFilterIcon();
    }

    /**
     * Maximize the distribution set grid
     */
    public void maximize() {
        distributionSetGrid.createMaximizedContent();
        hideDetailsLayout();
    }

    /**
     * Minimize the distribution set grid
     */
    public void minimize() {
        distributionSetGrid.createMinimizedContent();
        showDetailsLayout();
    }

    /**
     * Restore the distribution set header and grid
     */
    public void restoreState() {
        distributionSetGridHeader.restoreState();
        distributionSetGrid.restoreState();
    }

    /**
     * Unsubscribe the eent listener
     */
    public void unsubscribeListener() {
        dsFilterListener.unsubscribe();
        masterDsChangedListener.unsubscribe();
        selectDsListener.unsubscribe();
        dsModifiedListener.unsubscribe();
        tagModifiedListener.unsubscribe();
    }
}
