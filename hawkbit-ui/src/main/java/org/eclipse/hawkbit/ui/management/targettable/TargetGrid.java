/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.builder.GridComponentBuilder;
import org.eclipse.hawkbit.ui.common.builder.StatusIconBuilder.TargetPollingStatusIconSupplier;
import org.eclipse.hawkbit.ui.common.builder.StatusIconBuilder.TargetStatusIconSupplier;
import org.eclipse.hawkbit.ui.common.data.filters.TargetManagementFilterParams;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetToProxyTargetMapper;
import org.eclipse.hawkbit.ui.common.data.providers.TargetManagementStateDataProvider;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.FilterType;
import org.eclipse.hawkbit.ui.common.event.PinningChangedEventPayload;
import org.eclipse.hawkbit.ui.common.event.PinningChangedEventPayload.PinningChangedEventType;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.common.grid.support.DeleteSupport;
import org.eclipse.hawkbit.ui.common.grid.support.DragAndDropSupport;
import org.eclipse.hawkbit.ui.common.grid.support.FilterSupport;
import org.eclipse.hawkbit.ui.common.grid.support.PinSupport;
import org.eclipse.hawkbit.ui.common.grid.support.PinSupport.PinBehaviourType;
import org.eclipse.hawkbit.ui.common.grid.support.SelectionSupport;
import org.eclipse.hawkbit.ui.common.grid.support.assignment.AssignmentSupport;
import org.eclipse.hawkbit.ui.common.grid.support.assignment.DistributionSetsToTargetAssignmentSupport;
import org.eclipse.hawkbit.ui.common.grid.support.assignment.TargetTagsToTargetAssignmentSupport;
import org.eclipse.hawkbit.ui.management.dstable.DistributionGridLayoutUiState;
import org.eclipse.hawkbit.ui.management.miscs.DeploymentAssignmentWindowController;
import org.eclipse.hawkbit.ui.management.targettag.filter.TargetTagFilterLayoutUiState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.vaadin.data.ValueProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;

/**
 * Concrete implementation of Target grid which is displayed on the Deployment
 * View.
 */
public class TargetGrid extends AbstractGrid<ProxyTarget, TargetManagementFilterParams> {
    private static final long serialVersionUID = 1L;

    private static final String TARGET_STATUS_ID = "targetStatus";
    private static final String TARGET_CONTROLLER_ID = "targetControllerId";
    private static final String TARGET_NAME_ID = "targetName";
    private static final String TARGET_POLLING_STATUS_ID = "targetPolling";
    private static final String TARGET_DESC_ID = "targetDescription";
    private static final String TARGET_PIN_BUTTON_ID = "targetPinButton";
    private static final String TARGET_DELETE_BUTTON_ID = "targetDeleteButton";

    private final TargetGridLayoutUiState targetGridLayoutUiState;
    private final TargetTagFilterLayoutUiState targetTagFilterLayoutUiState;
    private final DistributionGridLayoutUiState distributionGridLayoutUiState;
    private final transient TargetManagement targetManagement;

    private final TargetStatusIconSupplier<ProxyTarget> targetStatusIconSupplier;
    private final TargetPollingStatusIconSupplier targetPollingStatusIconSupplier;

    private final transient TargetToProxyTargetMapper targetToProxyTargetMapper;

    private final transient PinSupport<ProxyTarget, Long> pinSupport;
    private final transient DeleteSupport<ProxyTarget> targetDeleteSupport;

    /**
     * Constructor for TargetGrid
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetManagement
     *            TargetManagement
     * @param deploymentManagement
     *            DeploymentManagement
     * @param configManagement
     *            TenantConfigurationManagement
     * @param systemSecurityContext
     *            SystemSecurityContext
     * @param uiProperties
     *            UiProperties
     * @param targetGridLayoutUiState
     *            TargetGridLayoutUiState
     * @param distributionGridLayoutUiState
     *            DistributionGridLayoutUiState
     * @param targetTagFilterLayoutUiState
     *            TargetTagFilterLayoutUiState
     */
    public TargetGrid(final CommonUiDependencies uiDependencies, final TargetManagement targetManagement,
            final DeploymentManagement deploymentManagement, final TenantConfigurationManagement configManagement,
            final SystemSecurityContext systemSecurityContext, final UiProperties uiProperties,
            final TargetGridLayoutUiState targetGridLayoutUiState,
            final DistributionGridLayoutUiState distributionGridLayoutUiState,
            final TargetTagFilterLayoutUiState targetTagFilterLayoutUiState) {
        super(uiDependencies.getI18n(), uiDependencies.getEventBus(), uiDependencies.getPermChecker());

        this.targetManagement = targetManagement;
        this.targetGridLayoutUiState = targetGridLayoutUiState;
        this.targetTagFilterLayoutUiState = targetTagFilterLayoutUiState;
        this.distributionGridLayoutUiState = distributionGridLayoutUiState;
        this.targetToProxyTargetMapper = new TargetToProxyTargetMapper(i18n);

        setSelectionSupport(new SelectionSupport<ProxyTarget>(this, eventBus, EventLayout.TARGET_LIST,
                EventView.DEPLOYMENT, this::mapIdToProxyEntity, this::getSelectedEntityIdFromUiState,
                this::setSelectedEntityIdToUiState));
        if (targetGridLayoutUiState.isMaximized()) {
            getSelectionSupport().disableSelection();
        } else {
            getSelectionSupport().enableMultiSelection();
        }

        this.targetDeleteSupport = new DeleteSupport<>(this, i18n, uiDependencies.getUiNotification(),
                "target.details.header", "caption.targets", ProxyTarget::getName, this::deleteTargets,
                UIComponentIdProvider.TARGET_DELETE_CONFIRMATION_DIALOG);

        this.pinSupport = new PinSupport<>(this::refreshItem, this::publishPinningChangedEvent,
                this::updatePinnedUiState, this::getPinFilter, this::updatePinFilter, this::getAssignedToDsTargetIds,
                this::getInstalledToDsTargetIds);

        final Map<String, AssignmentSupport<?, ProxyTarget>> sourceTargetAssignmentStrategies = new HashMap<>();

        final DeploymentAssignmentWindowController assignmentController = new DeploymentAssignmentWindowController(
                uiDependencies, uiProperties, deploymentManagement);
        final DistributionSetsToTargetAssignmentSupport distributionsToTargetAssignment = new DistributionSetsToTargetAssignmentSupport(
                uiDependencies, systemSecurityContext, configManagement, assignmentController);
        final TargetTagsToTargetAssignmentSupport targetTagsToTargetAssignment = new TargetTagsToTargetAssignmentSupport(
                uiDependencies, targetManagement);

        sourceTargetAssignmentStrategies.put(UIComponentIdProvider.DIST_TABLE_ID, distributionsToTargetAssignment);
        sourceTargetAssignmentStrategies.put(UIComponentIdProvider.TARGET_TAG_TABLE_ID, targetTagsToTargetAssignment);

        setDragAndDropSupportSupport(new DragAndDropSupport<>(this, i18n, uiDependencies.getUiNotification(),
                sourceTargetAssignmentStrategies, eventBus));
        if (!targetGridLayoutUiState.isMaximized()) {
            getDragAndDropSupportSupport().addDragAndDrop();
        }

        setFilterSupport(
                new FilterSupport<>(new TargetManagementStateDataProvider(targetManagement, targetToProxyTargetMapper),
                        getSelectionSupport()::deselectAll));
        initFilterMappings();
        getFilterSupport().setFilter(new TargetManagementFilterParams());

        initDsPinningStyleGenerator();
        targetStatusIconSupplier = new TargetStatusIconSupplier<>(i18n, ProxyTarget::getUpdateStatus,
                UIComponentIdProvider.TARGET_TABLE_STATUS_LABEL_ID);
        targetPollingStatusIconSupplier = new TargetPollingStatusIconSupplier(i18n,
                UIComponentIdProvider.TARGET_TABLE_POLLING_STATUS_LABEL_ID);
        init();
    }

    @Override
    public void init() {
        super.init();

        addStyleName("grid-row-border");
    }

    /**
     * Map entity id to proxy entity
     *
     * @param entityId
     *            Entity id
     *
     * @return Target
     */
    public Optional<ProxyTarget> mapIdToProxyEntity(final long entityId) {
        return targetManagement.get(entityId).map(targetToProxyTargetMapper::map);
    }

    private Long getSelectedEntityIdFromUiState() {
        return targetGridLayoutUiState.getSelectedEntityId();
    }

    private void setSelectedEntityIdToUiState(final Long entityId) {
        targetGridLayoutUiState.setSelectedEntityId(entityId);
    }

    private boolean deleteTargets(final Collection<ProxyTarget> targetsToBeDeleted) {
        final Collection<Long> targetToBeDeletedIds = targetsToBeDeleted.stream().map(ProxyIdentifiableEntity::getId)
                .collect(Collectors.toList());
        targetManagement.delete(targetToBeDeletedIds);

        eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_REMOVED, ProxyTarget.class, targetToBeDeletedIds));

        return true;
    }

    private void publishPinningChangedEvent(final PinBehaviourType pinType, final ProxyTarget pinnedItem) {
        eventBus.publish(EventTopics.PINNING_CHANGED, this,
                new PinningChangedEventPayload<String>(
                        pinType == PinBehaviourType.PINNED ? PinningChangedEventType.ENTITY_PINNED
                                : PinningChangedEventType.ENTITY_UNPINNED,
                        ProxyTarget.class, pinnedItem.getControllerId()));
    }

    private void updatePinnedUiState(final ProxyTarget pinnedItem) {
        targetGridLayoutUiState.setPinnedTargetId(pinnedItem != null ? pinnedItem.getId() : null);
        targetGridLayoutUiState.setPinnedControllerId(pinnedItem != null ? pinnedItem.getControllerId() : null);
    }

    private Optional<Long> getPinFilter() {
        return getFilter().map(TargetManagementFilterParams::getPinnedDistId);
    }

    private void updatePinFilter(final Long pinnedDsId) {
        getFilterSupport().updateFilter(TargetManagementFilterParams::setPinnedDistId, pinnedDsId);
    }

    private Collection<Long> getAssignedToDsTargetIds(final Long pinnedDsId) {
        return getTargetIdsByFunction(query -> targetManagement.findByAssignedDistributionSet(query, pinnedDsId));
    }

    private Collection<Long> getTargetIdsByFunction(final Function<Pageable, Page<Target>> findTargetsFunction) {
        return HawkbitCommonUtil.getEntitiesByPageableProvider(findTargetsFunction::apply).stream().map(Target::getId)
                .collect(Collectors.toList());
    }

    private Collection<Long> getInstalledToDsTargetIds(final Long pinnedDsId) {
        return getTargetIdsByFunction(query -> targetManagement.findByInstalledDistributionSet(query, pinnedDsId));
    }

    private void initFilterMappings() {
        getFilterSupport().addMapping(FilterType.SEARCH, TargetManagementFilterParams::setSearchText,
                targetGridLayoutUiState.getSearchFilter());
        getFilterSupport().addMapping(FilterType.STATUS, TargetManagementFilterParams::setTargetUpdateStatusList,
                targetTagFilterLayoutUiState.getClickedTargetUpdateStatusFilters());
        getFilterSupport().addMapping(FilterType.OVERDUE, TargetManagementFilterParams::setOverdueState,
                targetTagFilterLayoutUiState.isOverdueFilterClicked());
        getFilterSupport().addMapping(FilterType.NO_TAG, TargetManagementFilterParams::setNoTagClicked,
                targetTagFilterLayoutUiState.isNoTagClicked());
        getFilterSupport().addMapping(FilterType.TAG, TargetManagementFilterParams::setTargetTags,
                targetTagFilterLayoutUiState.getClickedTagIdsWithName().values());
        getFilterSupport().addMapping(FilterType.QUERY, TargetManagementFilterParams::setTargetFilterQueryId,
                targetTagFilterLayoutUiState.getClickedTargetFilterQueryId());
        getFilterSupport().addMapping(FilterType.DISTRIBUTION, TargetManagementFilterParams::setDistributionId,
                targetGridLayoutUiState.getFilterDsInfo() != null ? targetGridLayoutUiState.getFilterDsInfo().getId()
                        : null);
    }

    private void initDsPinningStyleGenerator() {
        setStyleGenerator(target -> pinSupport.getAssignedOrInstalledRowStyle(target.getId()));
    }

    @Override
    public String getGridId() {
        return UIComponentIdProvider.TARGET_TABLE_ID;
    }

    /**
     * Update filter on custom tab selection
     */
    public void onCustomTabSelected() {
        getFilter().ifPresent(filter -> {
            filter.setDistributionId(null);
            filter.setNoTagClicked(false);
            filter.setOverdueState(false);
            filter.setSearchText(null);
            filter.setTargetTags(Collections.emptyList());
            filter.setTargetUpdateStatusList(Collections.emptyList());

            getFilterSupport().refreshFilter();
        });
    }

    /**
     * Update filter on simple tab selection
     */
    public void onSimpleTabSelected() {
        getFilterSupport().updateFilter(TargetManagementFilterParams::setTargetFilterQueryId, null);
    }

    @Override
    public void addColumns() {
        addNameColumn();

        GridComponentBuilder.joinToIconColumn(getDefaultHeaderRow(), i18n.getMessage("header.status"),
                Arrays.asList(addTargetPollingStatusColumn(), addTargetStatusColumn()));

        GridComponentBuilder.joinToActionColumn(i18n, getDefaultHeaderRow(),
                Arrays.asList(addPinColumn(), addDeleteColumn()));
    }

    private Column<ProxyTarget, String> addControllerIdColumn() {
        return GridComponentBuilder.addControllerIdColumn(this, i18n, TARGET_CONTROLLER_ID);
    }

    private Column<ProxyTarget, String> addNameColumn() {
        return GridComponentBuilder.addNameColumn(this, i18n, TARGET_NAME_ID);
    }

    private Column<ProxyTarget, Label> addTargetPollingStatusColumn() {
        return GridComponentBuilder.addIconColumn(this, targetPollingStatusIconSupplier::getLabel,
                TARGET_POLLING_STATUS_ID, null);
    }

    private Column<ProxyTarget, Label> addTargetStatusColumn() {
        return GridComponentBuilder.addIconColumn(this, targetStatusIconSupplier::getLabel, TARGET_STATUS_ID, null);
    }

    private Column<ProxyTarget, Button> addPinColumn() {
        final ValueProvider<ProxyTarget, Button> buttonProvider = target -> GridComponentBuilder.buildActionButton(i18n,
                clickEvent -> pinSupport.changeItemPinning(target), VaadinIcons.PIN,
                UIMessageIdProvider.TOOLTIP_TARGET_PIN, SPUIStyleDefinitions.STATUS_ICON_NEUTRAL,
                UIComponentIdProvider.TARGET_PIN_ICON + "." + target.getId(), true);
        return GridComponentBuilder.addIconColumn(this, buttonProvider, TARGET_PIN_BUTTON_ID, null,
                pinSupport::getPinningStyle);
    }

    private Column<ProxyTarget, Button> addDeleteColumn() {
        return GridComponentBuilder.addDeleteColumn(this, i18n, TARGET_DELETE_BUTTON_ID, targetDeleteSupport,
                UIComponentIdProvider.TARGET_DELET_ICON, e -> permissionChecker.hasDeleteTargetPermission());
    }

    @Override
    protected void addMaxColumns() {
        addNameColumn().setExpandRatio(2);
        addControllerIdColumn().setExpandRatio(2);

        GridComponentBuilder.addDescriptionColumn(this, i18n, TARGET_DESC_ID).setExpandRatio(2);
        GridComponentBuilder.addCreatedAndModifiedColumns(this, i18n);

        addDeleteColumn();

        getColumns().forEach(column -> column.setHidable(true));
    }

    @Override
    public void restoreState() {
        final Long pinnedTargetId = targetGridLayoutUiState.getPinnedTargetId();
        if (pinnedTargetId != null) {
            final ProxyTarget pinnedTarget = new ProxyTarget();
            pinnedTarget.setId(pinnedTargetId);
            pinSupport.restorePinning(pinnedTarget);
        }

        final Long pinnedDsId = distributionGridLayoutUiState.getPinnedDsId();
        if (pinnedDsId != null) {
            pinSupport.repopulateAssignedAndInstalled(pinnedDsId);
            getFilter().ifPresent(filter -> filter.setPinnedDistId(pinnedDsId));
        }

        super.restoreState();
    }

    /**
     * Gets the pin support
     *
     * @return Pin support
     */
    public PinSupport<ProxyTarget, Long> getPinSupport() {
        return pinSupport;
    }
}
