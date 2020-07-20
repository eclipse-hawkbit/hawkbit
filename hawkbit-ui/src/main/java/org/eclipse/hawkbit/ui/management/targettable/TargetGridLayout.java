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
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTag;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
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
import org.eclipse.hawkbit.ui.management.CountMessageLabel;
import org.eclipse.hawkbit.ui.management.bulkupload.BulkUploadWindowBuilder;
import org.eclipse.hawkbit.ui.management.bulkupload.TargetBulkUploadUiState;
import org.eclipse.hawkbit.ui.management.dstable.DistributionGridLayoutUiState;
import org.eclipse.hawkbit.ui.management.targettag.filter.TargetTagFilterLayoutUiState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Target table layout.
 */
public class TargetGridLayout extends AbstractGridComponentLayout {
    private static final long serialVersionUID = 1L;

    private final TargetGridHeader targetGridHeader;
    private final TargetGrid targetGrid;
    private final TargetDetailsHeader targetDetailsHeader;
    private final TargetDetails targetDetails;
    private final transient CountMessageLabel countMessageLabel;

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
     * @param eventBus
     *            UIEventBus
     * @param targetManagement
     *            TargetManagement
     * @param entityFactory
     *            EntityFactory
     * @param i18n
     *            VaadinMessageSource
     * @param uiNotification
     *            UINotification
     * @param deploymentManagement
     *            DeploymentManagement
     * @param uiProperties
     *            UiProperties
     * @param permissionChecker
     *            SpPermissionChecker
     * @param targetTagManagement
     *            TargetTagManagement
     * @param distributionSetManagement
     *            DistributionSetManagement
     * @param uiExecutor
     *            Executor
     * @param configManagement
     *            TenantConfigurationManagement
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
    public TargetGridLayout(final UIEventBus eventBus, final TargetManagement targetManagement,
            final EntityFactory entityFactory, final VaadinMessageSource i18n, final UINotification uiNotification,
            final DeploymentManagement deploymentManagement, final UiProperties uiProperties,
            final SpPermissionChecker permissionChecker, final TargetTagManagement targetTagManagement,
            final DistributionSetManagement distributionSetManagement, final Executor uiExecutor,
            final TenantConfigurationManagement configManagement, final SystemSecurityContext systemSecurityContext,
            final TargetTagFilterLayoutUiState targetTagFilterLayoutUiState,
            final TargetGridLayoutUiState targetGridLayoutUiState,
            final TargetBulkUploadUiState targetBulkUploadUiState,
            final DistributionGridLayoutUiState distributionGridLayoutUiState) {
        final TargetWindowBuilder targetWindowBuilder = new TargetWindowBuilder(i18n, entityFactory, eventBus,
                uiNotification, targetManagement, EventView.DEPLOYMENT);
        final TargetMetaDataWindowBuilder targetMetaDataWindowBuilder = new TargetMetaDataWindowBuilder(i18n,
                entityFactory, eventBus, uiNotification, permissionChecker, targetManagement);
        final BulkUploadWindowBuilder bulkUploadWindowBuilder = new BulkUploadWindowBuilder(i18n, eventBus,
                permissionChecker, uiNotification, uiProperties, uiExecutor, targetManagement, deploymentManagement,
                targetTagManagement, distributionSetManagement, entityFactory, targetBulkUploadUiState);

        this.targetGridHeader = new TargetGridHeader(i18n, permissionChecker, eventBus, uiNotification,
                targetWindowBuilder, bulkUploadWindowBuilder, targetTagFilterLayoutUiState, targetGridLayoutUiState,
                targetBulkUploadUiState);
        this.targetGridHeader.buildHeader();
        this.targetGridHeader.addDsDroArea();
        this.targetGrid = new TargetGrid(eventBus, i18n, uiNotification, targetManagement, permissionChecker,
                deploymentManagement, configManagement, systemSecurityContext, uiProperties, targetGridLayoutUiState,
                distributionGridLayoutUiState, targetTagFilterLayoutUiState);

        this.targetDetailsHeader = new TargetDetailsHeader(i18n, permissionChecker, eventBus, uiNotification,
                targetWindowBuilder, targetMetaDataWindowBuilder);
        this.targetDetails = new TargetDetails(i18n, eventBus, permissionChecker, uiNotification, targetTagManagement,
                targetManagement, deploymentManagement, targetMetaDataWindowBuilder);

        this.countMessageLabel = new CountMessageLabel(targetManagement, i18n);

        initGridDataUpdatedListener();

        this.filterTabChangedListener = new GenericEventListener<>(eventBus, EventTopics.TARGET_FILTER_TAB_CHANGED,
                this::onTargetFilterTabChanged);
        this.targetFilterListener = new FilterChangedListener<>(eventBus, ProxyTarget.class,
                new EventViewAware(EventView.DEPLOYMENT), targetGrid.getFilterSupport());
        this.pinningChangedListener = new PinningChangedListener<>(eventBus, ProxyDistributionSet.class,
                targetGrid.getPinSupport());
        this.targetChangedListener = new SelectionChangedListener<>(eventBus,
                new EventLayoutViewAware(EventLayout.TARGET_LIST, EventView.DEPLOYMENT),
                getMasterTargetAwareComponents());
        this.selectTargetListener = new SelectGridEntityListener<>(eventBus,
                new EventLayoutViewAware(EventLayout.TARGET_LIST, EventView.DEPLOYMENT),
                targetGrid.getSelectionSupport());
        this.targetModifiedListener = new EntityModifiedListener.Builder<>(eventBus, ProxyTarget.class)
                .entityModifiedAwareSupports(getTargetModifiedAwareSupports()).build();
        this.tagModifiedListener = new EntityModifiedListener.Builder<>(eventBus, ProxyTag.class)
                .entityModifiedAwareSupports(getTagModifiedAwareSupports()).parentEntityType(ProxyTarget.class).build();
        this.bulkUploadListener = new BulkUploadChangedListener(eventBus, targetGridHeader::onBulkUploadChanged);

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
    public CountMessageLabel getCountMessageLabel() {
        return countMessageLabel;
    }
}
