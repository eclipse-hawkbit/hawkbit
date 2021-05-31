/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.suppliers.TargetManagementStateDataSupplier;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventLayoutViewAware;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.EventViewAware;
import org.eclipse.hawkbit.ui.common.event.TargetFilterTabChangedEventPayload;
import org.eclipse.hawkbit.ui.common.layout.AbstractGridComponentLayout;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.eclipse.hawkbit.ui.common.layout.listener.BulkUploadChangedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener.EntityModifiedAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.FilterChangedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.GenericEventListener;
import org.eclipse.hawkbit.ui.common.layout.listener.PinningChangedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.SelectGridEntityListener;
import org.eclipse.hawkbit.ui.common.layout.listener.SelectionChangedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedGridRefreshAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedPinAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedSelectionAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedTagTokenAwareSupport;
import org.eclipse.hawkbit.ui.management.bulkupload.BulkUploadWindowBuilder;
import org.eclipse.hawkbit.ui.management.bulkupload.TargetBulkUploadUiState;
import org.eclipse.hawkbit.ui.management.dstable.DistributionGridLayoutUiState;
import org.eclipse.hawkbit.ui.management.targettag.filter.TargetTagFilterLayoutUiState;

/**
 * Target table layout.
 */
public class TargetGridLayout extends AbstractGridComponentLayout {
    private static final long serialVersionUID = 1L;

    private final TargetGridHeader targetGridHeader;
    private final TargetGrid targetGrid;
    private final TargetDetailsHeader targetDetailsHeader;
    private final TargetDetails targetDetails;
    private final transient TargetCountMessageLabel countMessageLabel;

    private final transient GenericEventListener<TargetFilterTabChangedEventPayload> filterTabChangedListener;
    private final transient FilterChangedListener<ProxyTarget> targetFilterListener;
    private final transient PinningChangedListener<Long> pinningChangedListener;
    private final transient SelectionChangedListener<ProxyTarget> targetChangedListener;
    private final transient SelectGridEntityListener<ProxyTarget> selectTargetListener;
    private final transient EntityModifiedListener<ProxyTarget> targetModifiedListener;
    private final transient EntityModifiedListener<ProxyTag> tagModifiedListener;
    private final transient BulkUploadChangedListener bulkUploadListener;

    /**
     * Constructor for TargetGridLayout
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetManagement
     *            TargetManagement
     * @param deploymentManagement
     *            DeploymentManagement
     * @param uiProperties
     *            UiProperties
     * @param targetTagManagement
     *            TargetTagManagement
     * @param distributionSetManagement
     *            DistributionSetManagement
     * @param uiExecutor
     *            Executor
     * @param configManagement
     *            TenantConfigurationManagement
     * @param targetManagementStateDataSupplier
     *            target grid data supplier
     * @param systemSecurityContext
     *            SystemSecurityContext
     * @param targetTagFilterLayoutUiState
     *            TargetTagFilterLayoutUiState
     * @param targetGridLayoutUiState
     *            TargetGridLayoutUiState
     * @param targetBulkUploadUiState
     *            TargetBulkUploadUiState
     * @param distributionGridLayoutUiState
     *            DistributionGridLayoutUiState
     */
    public TargetGridLayout(final CommonUiDependencies uiDependencies, final TargetManagement targetManagement,
            final DeploymentManagement deploymentManagement, final UiProperties uiProperties,
            final TargetTagManagement targetTagManagement, final DistributionSetManagement distributionSetManagement,
            final Executor uiExecutor, final TenantConfigurationManagement configManagement,
            final TargetManagementStateDataSupplier targetManagementStateDataSupplier,
            final SystemSecurityContext systemSecurityContext,
            final TargetTagFilterLayoutUiState targetTagFilterLayoutUiState,
            final TargetGridLayoutUiState targetGridLayoutUiState,
            final TargetBulkUploadUiState targetBulkUploadUiState,
            final DistributionGridLayoutUiState distributionGridLayoutUiState) {
        final TargetWindowBuilder targetWindowBuilder = new TargetWindowBuilder(uiDependencies, targetManagement,
                EventView.DEPLOYMENT);
        final TargetMetaDataWindowBuilder targetMetaDataWindowBuilder = new TargetMetaDataWindowBuilder(uiDependencies,
                targetManagement);
        final BulkUploadWindowBuilder bulkUploadWindowBuilder = new BulkUploadWindowBuilder(uiDependencies,
                uiProperties, uiExecutor, targetManagement, deploymentManagement, targetTagManagement,
                distributionSetManagement, targetBulkUploadUiState);

        this.targetGridHeader = new TargetGridHeader(uiDependencies, targetWindowBuilder, bulkUploadWindowBuilder,
                targetTagFilterLayoutUiState, targetGridLayoutUiState, targetBulkUploadUiState);
        this.targetGridHeader.buildHeader();
        this.targetGridHeader.addDsDropArea();
        this.targetGrid = new TargetGrid(uiDependencies, targetManagement, deploymentManagement, configManagement,
                targetManagementStateDataSupplier, systemSecurityContext, uiProperties, targetGridLayoutUiState,
                distributionGridLayoutUiState, targetTagFilterLayoutUiState);

        this.targetDetailsHeader = new TargetDetailsHeader(uiDependencies, targetWindowBuilder,
                targetMetaDataWindowBuilder);
        this.targetDetails = new TargetDetails(uiDependencies, targetTagManagement, targetManagement,
                deploymentManagement, targetMetaDataWindowBuilder);

        this.countMessageLabel = new TargetCountMessageLabel(targetManagement, uiDependencies.getI18n());

        initGridDataUpdatedListener();

        this.filterTabChangedListener = new GenericEventListener<>(uiDependencies.getEventBus(),
                EventTopics.TARGET_FILTER_TAB_CHANGED, this::onTargetFilterTabChanged);
        this.targetFilterListener = new FilterChangedListener<>(uiDependencies.getEventBus(), ProxyTarget.class,
                new EventViewAware(EventView.DEPLOYMENT), targetGrid.getFilterSupport());
        this.pinningChangedListener = new PinningChangedListener<>(uiDependencies.getEventBus(),
                ProxyDistributionSet.class, targetGrid.getPinSupport());
        this.targetChangedListener = new SelectionChangedListener<>(uiDependencies.getEventBus(),
                new EventLayoutViewAware(EventLayout.TARGET_LIST, EventView.DEPLOYMENT),
                getMasterTargetAwareComponents());
        this.selectTargetListener = new SelectGridEntityListener<>(uiDependencies.getEventBus(),
                new EventLayoutViewAware(EventLayout.TARGET_LIST, EventView.DEPLOYMENT),
                targetGrid.getSelectionSupport());
        this.targetModifiedListener = new EntityModifiedListener.Builder<>(uiDependencies.getEventBus(),
                ProxyTarget.class).entityModifiedAwareSupports(getTargetModifiedAwareSupports()).build();
        this.tagModifiedListener = new EntityModifiedListener.Builder<>(uiDependencies.getEventBus(), ProxyTag.class)
                .entityModifiedAwareSupports(getTagModifiedAwareSupports()).parentEntityType(ProxyTarget.class).build();
        this.bulkUploadListener = new BulkUploadChangedListener(uiDependencies.getEventBus(),
                targetGridHeader::onBulkUploadChanged);

        buildLayout(targetGridHeader, targetGrid, targetDetailsHeader, targetDetails);
    }

    private void initGridDataUpdatedListener() {
        targetGrid.addDataChangedListener(event -> countMessageLabel.displayTargetCountStatus(targetGrid.getDataSize(),
                targetGrid.getFilter().orElse(null)));
    }

    private List<MasterEntityAwareComponent<ProxyTarget>> getMasterTargetAwareComponents() {
        return Arrays.asList(targetDetailsHeader, targetDetails);
    }

    private List<EntityModifiedAwareSupport> getTargetModifiedAwareSupports() {
        return Arrays.asList(
                EntityModifiedSelectionAwareSupport.of(targetGrid.getSelectionSupport(),
                        targetGrid::mapIdToProxyEntity),
                EntityModifiedPinAwareSupport.of(targetGrid.getPinSupport(), true, true),
                EntityModifiedGridRefreshAwareSupport.of(targetGrid::refreshAll));
    }

    private List<EntityModifiedAwareSupport> getTagModifiedAwareSupports() {
        return Collections.singletonList(EntityModifiedTagTokenAwareSupport.of(targetDetails.getTargetTagToken()));
    }

    /**
     * Show target tag header icon
     */
    public void showTargetTagHeaderIcon() {
        targetGridHeader.showFilterIcon();
    }

    /**
     * Hide target tag header icon
     */
    public void hideTargetTagHeaderIcon() {
        targetGridHeader.hideFilterIcon();
    }

    /**
     * Actions on target filter tab changed
     *
     * @param eventPayload
     *            event payload to identify which tab was selected
     */
    public void onTargetFilterTabChanged(final TargetFilterTabChangedEventPayload eventPayload) {
        final boolean isCustomFilterTabSelected = TargetFilterTabChangedEventPayload.CUSTOM == eventPayload;

        if (isCustomFilterTabSelected) {
            targetGridHeader.onSimpleFilterReset();
            targetGrid.onCustomTabSelected();
        } else {
            targetGridHeader.enableSearchIcon();
            targetGrid.onSimpleTabSelected();
        }
    }

    /**
     * Maximize the target grid
     */
    public void maximize() {
        targetGrid.createMaximizedContent();
        hideDetailsLayout();
    }

    /**
     * Minimize the target grid
     */
    public void minimize() {
        targetGrid.createMinimizedContent();
        showDetailsLayout();
    }

    /**
     * Restore the target grid state
     */
    public void restoreState() {
        targetGridHeader.restoreState();
        targetGrid.restoreState();
    }

    /**
     * Unsubscribe all the listeners
     */
    public void unsubscribeListener() {
        filterTabChangedListener.unsubscribe();
        targetFilterListener.unsubscribe();
        pinningChangedListener.unsubscribe();
        targetChangedListener.unsubscribe();
        selectTargetListener.unsubscribe();
        targetModifiedListener.unsubscribe();
        tagModifiedListener.unsubscribe();
        bulkUploadListener.unsubscribe();
    }

    /**
     * Gets the count message label
     *
     * @return Count message label
     */
    public TargetCountMessageLabel getCountMessageLabel() {
        return countMessageLabel;
    }
}
