/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetInvalidationManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.builder.GridComponentBuilder;
import org.eclipse.hawkbit.ui.common.data.filters.DsManagementFilterParams;
import org.eclipse.hawkbit.ui.common.data.providers.DistributionSetManagementStateDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.distributionset.AbstractDsGrid;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.FilterType;
import org.eclipse.hawkbit.ui.common.event.PinningChangedEventPayload;
import org.eclipse.hawkbit.ui.common.event.PinningChangedEventPayload.PinningChangedEventType;
import org.eclipse.hawkbit.ui.common.grid.support.DragAndDropSupport;
import org.eclipse.hawkbit.ui.common.grid.support.FilterSupport;
import org.eclipse.hawkbit.ui.common.grid.support.PinSupport;
import org.eclipse.hawkbit.ui.common.grid.support.PinSupport.PinBehaviourType;
import org.eclipse.hawkbit.ui.common.grid.support.assignment.AssignmentSupport;
import org.eclipse.hawkbit.ui.common.grid.support.assignment.DsTagsToDistributionSetAssignmentSupport;
import org.eclipse.hawkbit.ui.common.grid.support.assignment.TargetTagsToDistributionSetAssignmentSupport;
import org.eclipse.hawkbit.ui.common.grid.support.assignment.TargetsToDistributionSetAssignmentSupport;
import org.eclipse.hawkbit.ui.common.state.TagFilterLayoutUiState;
import org.eclipse.hawkbit.ui.management.miscs.DeploymentAssignmentWindowController;
import org.eclipse.hawkbit.ui.management.targettable.TargetGridLayoutUiState;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.util.StringUtils;

import com.vaadin.data.ValueProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;


/**
 * Distribution set grid which is shown on the Deployment View.
 */
public class DistributionGrid extends AbstractDsGrid<DsManagementFilterParams> {
    private static final long serialVersionUID = 1L;

    private static final String DS_PIN_BUTTON_ID = "dsPinnButton";
    private static final String DS_INVALIDATE_BUTTON_ID = "dsInvalidateButton";

    private final TargetGridLayoutUiState targetGridLayoutUiState;
    private final DistributionGridLayoutUiState distributionGridLayoutUiState;
    private final TagFilterLayoutUiState distributionTagLayoutUiState;

    private final transient DeploymentManagement deploymentManagement;

    private final transient PinSupport<ProxyDistributionSet, String> pinSupport;

    private final transient InvalidateDistributionSetSupport invalidateDistributionSetSupport;

    /**
     * Constructor for DistributionGrid
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetManagement
     *            TargetManagement
     * @param distributionSetManagement
     *            DistributionSetManagement
     * @param dsInvalidationManagement
     *            {@link DistributionSetInvalidationManagement}
     * @param deploymentManagement
     *            DeploymentManagement
     * @param uiProperties
     *            UiProperties
     * @param distributionGridLayoutUiState
     *            DistributionGridLayoutUiState
     * @param targetGridLayoutUiState
     *            TargetGridLayoutUiState
     * @param distributionTagLayoutUiState
     *            TagFilterLayoutUiState
     */
    public DistributionGrid(final CommonUiDependencies uiDependencies, final TargetManagement targetManagement,
            final DistributionSetManagement distributionSetManagement,
            final DistributionSetInvalidationManagement dsInvalidationManagement,
            final DeploymentManagement deploymentManagement, final UiProperties uiProperties,
            final DistributionGridLayoutUiState distributionGridLayoutUiState,
            final TargetGridLayoutUiState targetGridLayoutUiState,
            final TagFilterLayoutUiState distributionTagLayoutUiState, final TenantConfigHelper tenantConfigHelper) {
        super(uiDependencies, distributionSetManagement, distributionGridLayoutUiState, EventView.DEPLOYMENT);

        this.targetGridLayoutUiState = targetGridLayoutUiState;
        this.distributionGridLayoutUiState = distributionGridLayoutUiState;
        this.distributionTagLayoutUiState = distributionTagLayoutUiState;
        this.deploymentManagement = deploymentManagement;

        this.pinSupport = new PinSupport<>(this::refreshItem, this::publishPinningChangedEvent,
                this::updatePinnedUiState, this::getPinFilter, this::updatePinFilter, this::getAssignedToTargetDsIds,
                this::getInstalledToTargetDsIds);

        final Map<String, AssignmentSupport<?, ProxyDistributionSet>> sourceTargetAssignmentStrategies = new HashMap<>();
        
        final DeploymentAssignmentWindowController assignmentController = new DeploymentAssignmentWindowController(
                uiDependencies, uiProperties, deploymentManagement, tenantConfigHelper);
        final TargetsToDistributionSetAssignmentSupport targetsToDsAssignment = new TargetsToDistributionSetAssignmentSupport(
                uiDependencies, assignmentController);
        final TargetTagsToDistributionSetAssignmentSupport targetTagsToDsAssignment = new TargetTagsToDistributionSetAssignmentSupport(
                uiDependencies, targetManagement, targetsToDsAssignment);
        final DsTagsToDistributionSetAssignmentSupport dsTagsToDsAssignment = new DsTagsToDistributionSetAssignmentSupport(
                uiDependencies, distributionSetManagement);

        sourceTargetAssignmentStrategies.put(UIComponentIdProvider.TARGET_TABLE_ID, targetsToDsAssignment);
        sourceTargetAssignmentStrategies.put(UIComponentIdProvider.TARGET_TAG_TABLE_ID, targetTagsToDsAssignment);
        sourceTargetAssignmentStrategies.put(UIComponentIdProvider.DISTRIBUTION_TAG_TABLE_ID, dsTagsToDsAssignment);

        setDragAndDropSupportSupport(
                new DragAndDropSupport<>(this, i18n, notification, sourceTargetAssignmentStrategies, eventBus));
        if (!distributionGridLayoutUiState.isMaximized()) {
            getDragAndDropSupportSupport().addDragAndDrop();
        }

        setFilterSupport(new FilterSupport<>(
                new DistributionSetManagementStateDataProvider(distributionSetManagement, dsToProxyDistributionMapper),
                DsManagementFilterParams::new, getSelectionSupport()::deselectAll));
        initFilterMappings();
        getFilterSupport().setFilter(new DsManagementFilterParams());
        this.invalidateDistributionSetSupport = new InvalidateDistributionSetSupport(this, i18n, uiProperties,
                notification, permissionChecker, dsInvalidationManagement);

        initStyleGenerator();
        init();
    }

    private void initFilterMappings() {
        getFilterSupport().addMapping(FilterType.SEARCH, DsManagementFilterParams::setSearchText,
                distributionGridLayoutUiState.getSearchFilter());
        getFilterSupport().addMapping(FilterType.NO_TAG, DsManagementFilterParams::setNoTagClicked,
                distributionTagLayoutUiState.isNoTagClicked());
        getFilterSupport().addMapping(FilterType.TAG, DsManagementFilterParams::setDistributionSetTags,
                distributionTagLayoutUiState.getClickedTagIdsWithName().values());
    }

    private void publishPinningChangedEvent(final PinBehaviourType pinType, final ProxyDistributionSet pinnedItem) {
        eventBus.publish(EventTopics.PINNING_CHANGED, this,
                new PinningChangedEventPayload<Long>(
                        pinType == PinBehaviourType.PINNED ? PinningChangedEventType.ENTITY_PINNED
                                : PinningChangedEventType.ENTITY_UNPINNED,
                        ProxyDistributionSet.class, pinnedItem.getId()));
    }

    private void updatePinnedUiState(final ProxyDistributionSet pinnedItem) {
        distributionGridLayoutUiState.setPinnedDsId(pinnedItem != null ? pinnedItem.getId() : null);
    }

    private Optional<String> getPinFilter() {
        return getFilter().map(DsManagementFilterParams::getPinnedTargetControllerId);
    }

    private void updatePinFilter(final String pinnedControllerId) {
        getFilterSupport().updateFilter(DsManagementFilterParams::setPinnedTargetControllerId, pinnedControllerId);
    }

    private Collection<Long> getAssignedToTargetDsIds(final String controllerId) {
        // currently we do not support getting all assigned distribution sets
        // even in multi-assignment mode
        final Long assignedDsId = deploymentManagement.getAssignedDistributionSet(controllerId)
                .map(DistributionSet::getId).orElse(null);

        return assignedDsId != null ? Collections.singletonList(assignedDsId) : Collections.emptyList();
    }

    private Collection<Long> getInstalledToTargetDsIds(final String controllerId) {
        final Long installedDsId = deploymentManagement.getInstalledDistributionSet(controllerId)
                .map(DistributionSet::getId).orElse(null);

        return installedDsId != null ? Collections.singletonList(installedDsId) : Collections.emptyList();
    }

    private void initStyleGenerator() {
        setStyleGenerator(this::getRowStyle);
    }

    private String getRowStyle(final ProxyDistributionSet distributionSet) {
        final StringBuilder style = new StringBuilder();

        final String assignedInstalledStyle = pinSupport.getAssignedOrInstalledRowStyle(distributionSet.getId());
        if (assignedInstalledStyle != null) {
            style.append(assignedInstalledStyle);
        }

        if (!distributionSet.getIsValid()) {
            style.append(" ");
            style.append(SPUIDefinitions.INVALID_DISTRIBUTION);
        }

        return style.toString();
    }

    @Override
    public String getGridId() {
        return UIComponentIdProvider.DIST_TABLE_ID;
    }

    @Override
    public void addColumns() {
        addNameColumn();
        addVersionColumn();

        GridComponentBuilder.joinToActionColumn(i18n, getDefaultHeaderRow(),
                Arrays.asList(addPinColumn(), addDeleteColumn(), addInvalidateColumn()));
    }

    private Column<ProxyDistributionSet, Button> addPinColumn() {
        final ValueProvider<ProxyDistributionSet, Button> buttonProvider = ds -> GridComponentBuilder.buildActionButton(
                i18n, clickEvent -> pinSupport.changeItemPinning(ds), VaadinIcons.PIN,
                UIMessageIdProvider.TOOLTIP_DISTRIBUTION_SET_PIN, SPUIStyleDefinitions.STATUS_ICON_NEUTRAL,
                UIComponentIdProvider.DIST_PIN_ICON + "." + ds.getId(), true);
        return GridComponentBuilder.addIconColumn(this, buttonProvider, DS_PIN_BUTTON_ID, null,
                pinSupport::getPinningStyle);
    }


    private Column<ProxyDistributionSet, Button> addInvalidateColumn() {
        final ValueProvider<ProxyDistributionSet, Button> buttonProvider = ds -> GridComponentBuilder.buildActionButton(
                i18n, clickEvent -> invalidateDistributionSetSupport.openConsequencesWindowOnInvalidateAction(ds),
                VaadinIcons.BAN, UIMessageIdProvider.TOOLTIP_INVALIDATE_DISTRIBUTIONSET,
                SPUIStyleDefinitions.STATUS_ICON_NEUTRAL, UIComponentIdProvider.DIST_INVALIDATE_ICON + "." + ds.getId(),
                ds.getIsValid() && permissionChecker.hasDistributionSetInvalidatePermission());
        return GridComponentBuilder.addIconColumn(this, buttonProvider, DS_INVALIDATE_BUTTON_ID, null);
    }

    @Override
    public void restoreState() {
        final Long pinnedDsId = distributionGridLayoutUiState.getPinnedDsId();
        if (pinnedDsId != null) {
            final ProxyDistributionSet pinnedDs = new ProxyDistributionSet();
            pinnedDs.setId(pinnedDsId);
            pinSupport.restorePinning(pinnedDs);
        }

        final String pinnedControllerId = targetGridLayoutUiState.getPinnedControllerId();
        if (!StringUtils.isEmpty(pinnedControllerId)) {
            pinSupport.repopulateAssignedAndInstalled(pinnedControllerId);
            getFilter().ifPresent(filter -> filter.setPinnedTargetControllerId(pinnedControllerId));
        }

        super.restoreState();
    }

    /**
     * @return Pin support
     */
    public PinSupport<ProxyDistributionSet, String> getPinSupport() {
        return pinSupport;
    }
}
