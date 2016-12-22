/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rollout;

import static org.eclipse.hawkbit.ui.rollout.DistributionBarHelper.getTooltip;
import static org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil.HTML_LI_CLOSE_TAG;
import static org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil.HTML_LI_OPEN_TAG;
import static org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil.HTML_UL_CLOSE_TAG;
import static org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil.HTML_UL_OPEN_TAG;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.ACTION;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_CREATED_DATE;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_CREATED_USER;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_DESC;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_DIST_NAME_VERSION;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_ID;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_MODIFIED_BY;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_MODIFIED_DATE;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_NAME;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_STATUS;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_TOTAL_TARGETS;
import static org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.customrenderers.client.renderers.RolloutRendererData;
import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlButtonRenderer;
import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlLabelRenderer;
import org.eclipse.hawkbit.ui.customrenderers.renderers.RolloutRenderer;
import org.eclipse.hawkbit.ui.push.RolloutChangeEventContainer;
import org.eclipse.hawkbit.ui.push.event.RolloutChangeEvent;
import org.eclipse.hawkbit.ui.rollout.DistributionBarHelper;
import org.eclipse.hawkbit.ui.rollout.StatusFontIcon;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.collect.Maps;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.UI;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.renderers.HtmlRenderer;

/**
 * Rollout list grid component.
 */
public class RolloutListGrid extends AbstractGrid {

    // necessary for customizing the space to the right border of the table
    private static final String PLACEHOLDER = "placeholder";

    private static final long serialVersionUID = 4060904914954370524L;

    private static final String UPDATE_OPTION = "Update";

    private static final String PAUSE_OPTION = "Pause";

    private static final String RUN_OPTION = "Run";

    private static final String DS_TYPE = "type";

    private static final String SW_MODULES = "swModules";

    private static final String IS_REQUIRED_MIGRATION_STEP = "isRequiredMigrationStep";

    private static final String ROLLOUT_RENDERER_DATA = "rolloutRendererData";

    private final transient RolloutManagement rolloutManagement;

    private final AddUpdateRolloutWindowLayout addUpdateRolloutWindow;

    private final UINotification uiNotification;

    private final RolloutUIState rolloutUIState;

    private static final Map<RolloutStatus, StatusFontIcon> statusIconMap = new EnumMap<>(RolloutStatus.class);

    static {
        statusIconMap.put(RolloutStatus.FINISHED,
                new StatusFontIcon(FontAwesome.CHECK_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_GREEN));
        statusIconMap.put(RolloutStatus.PAUSED,
                new StatusFontIcon(FontAwesome.PAUSE, SPUIStyleDefinitions.STATUS_ICON_BLUE));
        statusIconMap.put(RolloutStatus.RUNNING, new StatusFontIcon(null, SPUIStyleDefinitions.STATUS_SPINNER_YELLOW));
        statusIconMap.put(RolloutStatus.READY,
                new StatusFontIcon(FontAwesome.DOT_CIRCLE_O, SPUIStyleDefinitions.STATUS_ICON_LIGHT_BLUE));
        statusIconMap.put(RolloutStatus.STOPPED,
                new StatusFontIcon(FontAwesome.STOP, SPUIStyleDefinitions.STATUS_ICON_RED));
        statusIconMap.put(RolloutStatus.CREATING, new StatusFontIcon(null, SPUIStyleDefinitions.STATUS_SPINNER_GREY));
        statusIconMap.put(RolloutStatus.STARTING, new StatusFontIcon(null, SPUIStyleDefinitions.STATUS_SPINNER_BLUE));
        statusIconMap.put(RolloutStatus.ERROR_CREATING,
                new StatusFontIcon(FontAwesome.EXCLAMATION_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_RED));
        statusIconMap.put(RolloutStatus.ERROR_STARTING,
                new StatusFontIcon(FontAwesome.EXCLAMATION_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_RED));
    }

    RolloutListGrid(final I18N i18n, final UIEventBus eventBus, final RolloutManagement rolloutManagement,
            final UINotification uiNotification, final RolloutUIState rolloutUIState,
            final SpPermissionChecker permissionChecker, final TargetManagement targetManagement,
            final EntityFactory entityFactory, final UiProperties uiProperties) {
        super(i18n, eventBus, permissionChecker);
        this.rolloutManagement = rolloutManagement;
        this.addUpdateRolloutWindow = new AddUpdateRolloutWindowLayout(rolloutManagement, targetManagement,
                uiNotification, uiProperties, entityFactory, i18n, eventBus);
        this.uiNotification = uiNotification;
        this.rolloutUIState = rolloutUIState;
    }

    /**
     * Handles the RolloutEvent to refresh Grid.
     *
     */
    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final RolloutEvent event) {
        switch (event) {
        case FILTER_BY_TEXT:
        case CREATE_ROLLOUT:
        case UPDATE_ROLLOUT:
        case SHOW_ROLLOUTS:
            refreshContainer();
            break;
        default:
            return;
        }
    }

    /**
     * Handles the RolloutChangeEvent to refresh the item in the grid.
     *
     * @param rolloutChangeEvent
     *            the event which contains the rollout which has been changed
     */
    @SuppressWarnings("unchecked")
    @EventBusListenerMethod(scope = EventScope.UI)
    public void onRolloutChangeEvent(final RolloutChangeEventContainer eventContainer) {
        eventContainer.getEvents().forEach(this::handleEvent);
    }

    private void handleEvent(final RolloutChangeEvent rolloutChangeEvent) {
        if (!rolloutUIState.isShowRollOuts()) {
            return;
        }
        final Rollout rollout = rolloutManagement.findRolloutWithDetailedStatus(rolloutChangeEvent.getRolloutId());
        final TotalTargetCountStatus totalTargetCountStatus = rollout.getTotalTargetCountStatus();
        final LazyQueryContainer rolloutContainer = (LazyQueryContainer) getContainerDataSource();
        final Item item = rolloutContainer.getItem(rolloutChangeEvent.getRolloutId());
        if (item == null) {
            refreshContainer();
            return;
        }
        item.getItemProperty(VAR_STATUS).setValue(rollout.getStatus());
        item.getItemProperty(VAR_TOTAL_TARGETS_COUNT_STATUS).setValue(totalTargetCountStatus);
        final Long groupCount = (Long) item.getItemProperty(VAR_NUMBER_OF_GROUPS).getValue();
        final int groupsCreated = rollout.getRolloutGroupsCreated();
        if (groupsCreated != 0) {
            item.getItemProperty(VAR_NUMBER_OF_GROUPS).setValue(Long.valueOf(groupsCreated));
        } else if (rollout.getRolloutGroups() != null && groupCount != rollout.getRolloutGroups().size()) {
            item.getItemProperty(VAR_NUMBER_OF_GROUPS).setValue(Long.valueOf(rollout.getRolloutGroups().size()));
        }
        item.getItemProperty(ROLLOUT_RENDERER_DATA)
                .setValue(new RolloutRendererData(rollout.getName(), rollout.getStatus().toString()));
    }

    @Override
    protected Container createContainer() {
        final BeanQueryFactory<RolloutBeanQuery> rolloutQf = new BeanQueryFactory<>(RolloutBeanQuery.class);
        return new LazyQueryContainer(new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, VAR_ID), rolloutQf);
    }

    @Override
    protected void addContainerProperties() {
        final LazyQueryContainer rolloutGridContainer = (LazyQueryContainer) getContainerDataSource();
        rolloutGridContainer.addContainerProperty(VAR_NAME, String.class, "", false, false);
        rolloutGridContainer.addContainerProperty(DS_TYPE, String.class, null, false, false);
        rolloutGridContainer.addContainerProperty(SW_MODULES, Set.class, null, false, false);
        rolloutGridContainer.addContainerProperty(ROLLOUT_RENDERER_DATA, RolloutRendererData.class, null, false, false);
        rolloutGridContainer.addContainerProperty(VAR_DESC, String.class, null, false, false);
        rolloutGridContainer.addContainerProperty(IS_REQUIRED_MIGRATION_STEP, boolean.class, null, false, false);
        rolloutGridContainer.addContainerProperty(VAR_STATUS, RolloutStatus.class, null, false, false);
        rolloutGridContainer.addContainerProperty(VAR_DIST_NAME_VERSION, String.class, null, false, false);
        rolloutGridContainer.addContainerProperty(VAR_CREATED_DATE, String.class, null, false, false);

        rolloutGridContainer.addContainerProperty(VAR_MODIFIED_DATE, String.class, null, false, false);
        rolloutGridContainer.addContainerProperty(VAR_CREATED_USER, String.class, null, false, false);
        rolloutGridContainer.addContainerProperty(VAR_MODIFIED_BY, String.class, null, false, false);
        rolloutGridContainer.addContainerProperty(VAR_NUMBER_OF_GROUPS, Long.class, 0, false, false);
        rolloutGridContainer.addContainerProperty(VAR_TOTAL_TARGETS, String.class, "0", false, false);
        rolloutGridContainer.addContainerProperty(VAR_TOTAL_TARGETS_COUNT_STATUS, TotalTargetCountStatus.class, null,
                false, false);

        rolloutGridContainer.addContainerProperty(RUN_OPTION, String.class, FontAwesome.PLAY.getHtml(), false, false);

        rolloutGridContainer.addContainerProperty(PAUSE_OPTION, String.class, FontAwesome.PAUSE.getHtml(), false,
                false);

        if (permissionChecker.hasRolloutUpdatePermission()) {
            rolloutGridContainer.addContainerProperty(UPDATE_OPTION, String.class, FontAwesome.EDIT.getHtml(), false,
                    false);
        }

        rolloutGridContainer.addContainerProperty(PLACEHOLDER, String.class, "", false, false);
    }

    @Override
    protected void setColumnExpandRatio() {

        getColumn(ROLLOUT_RENDERER_DATA).setMinimumWidth(40);
        getColumn(ROLLOUT_RENDERER_DATA).setMaximumWidth(150);

        getColumn(VAR_DIST_NAME_VERSION).setMinimumWidth(40);
        getColumn(VAR_DIST_NAME_VERSION).setMaximumWidth(150);

        getColumn(VAR_STATUS).setMinimumWidth(75);
        getColumn(VAR_STATUS).setMaximumWidth(75);

        getColumn(VAR_TOTAL_TARGETS).setMinimumWidth(40);
        getColumn(VAR_TOTAL_TARGETS).setMaximumWidth(100);

        getColumn(VAR_NUMBER_OF_GROUPS).setMinimumWidth(40);
        getColumn(VAR_NUMBER_OF_GROUPS).setMaximumWidth(100);

        getColumn(RUN_OPTION).setMinimumWidth(25);
        getColumn(RUN_OPTION).setMaximumWidth(25);

        getColumn(PAUSE_OPTION).setMinimumWidth(25);
        getColumn(PAUSE_OPTION).setMaximumWidth(25);

        if (permissionChecker.hasRolloutUpdatePermission()) {
            getColumn(UPDATE_OPTION).setMinimumWidth(25);
            getColumn(UPDATE_OPTION).setMaximumWidth(25);
        }

        getColumn(PLACEHOLDER).setMinimumWidth(25);
        getColumn(PLACEHOLDER).setMaximumWidth(25);

        getColumn(VAR_TOTAL_TARGETS_COUNT_STATUS).setMinimumWidth(280);
        // getColumn(VAR_TOTAL_TARGETS_COUNT_STATUS).setMaximumWidth(1000);

        setFrozenColumnCount(getColumns().size());
    }

    @Override
    protected void setColumnHeaderNames() {
        getColumn(ROLLOUT_RENDERER_DATA).setHeaderCaption(i18n.get("header.name"));
        getColumn(DS_TYPE).setHeaderCaption(i18n.get("header.type"));
        getColumn(SW_MODULES).setHeaderCaption(i18n.get("header.swmodules"));
        getColumn(IS_REQUIRED_MIGRATION_STEP).setHeaderCaption(i18n.get("header.migrations.step"));
        getColumn(VAR_DIST_NAME_VERSION).setHeaderCaption(i18n.get("header.distributionset"));
        getColumn(VAR_NUMBER_OF_GROUPS).setHeaderCaption(i18n.get("header.numberofgroups"));
        getColumn(VAR_TOTAL_TARGETS).setHeaderCaption(i18n.get("header.total.targets"));
        getColumn(VAR_CREATED_DATE).setHeaderCaption(i18n.get("header.createdDate"));
        getColumn(VAR_CREATED_USER).setHeaderCaption(i18n.get("header.createdBy"));
        getColumn(VAR_MODIFIED_DATE).setHeaderCaption(i18n.get("header.modifiedDate"));
        getColumn(VAR_MODIFIED_BY).setHeaderCaption(i18n.get("header.modifiedBy"));
        getColumn(VAR_DESC).setHeaderCaption(i18n.get("header.description"));
        getColumn(VAR_TOTAL_TARGETS_COUNT_STATUS).setHeaderCaption(i18n.get("header.detail.status"));
        getColumn(VAR_STATUS).setHeaderCaption(i18n.get("header.status"));

        getColumn(RUN_OPTION).setHeaderCaption(i18n.get("header.action.run"));
        getColumn(PAUSE_OPTION).setHeaderCaption(i18n.get("header.action.pause"));

        if (permissionChecker.hasRolloutUpdatePermission()) {
            getColumn(UPDATE_OPTION).setHeaderCaption(i18n.get("header.action.update"));
        }

        getColumn(PLACEHOLDER).setHeaderCaption(PLACEHOLDER);

        final HeaderCell join = getDefaultHeaderRow().join(RUN_OPTION, PAUSE_OPTION, UPDATE_OPTION, PLACEHOLDER);
        join.setText(i18n.get("header.action"));
    }

    @Override
    protected String getGridId() {
        return UIComponentIdProvider.ROLLOUT_LIST_GRID_ID;
    }

    @Override
    protected void setColumnProperties() {
        final List<Object> columnList = new ArrayList<>();
        columnList.add(ROLLOUT_RENDERER_DATA);
        columnList.add(VAR_DIST_NAME_VERSION);
        columnList.add(DS_TYPE);
        columnList.add(SW_MODULES);
        columnList.add(IS_REQUIRED_MIGRATION_STEP);
        columnList.add(VAR_STATUS);
        columnList.add(VAR_TOTAL_TARGETS_COUNT_STATUS);
        columnList.add(VAR_NUMBER_OF_GROUPS);
        columnList.add(VAR_TOTAL_TARGETS);

        columnList.add(RUN_OPTION);
        columnList.add(PAUSE_OPTION);

        if (permissionChecker.hasRolloutUpdatePermission()) {
            columnList.add(UPDATE_OPTION);
        }

        columnList.add(PLACEHOLDER);

        columnList.add(VAR_CREATED_DATE);
        columnList.add(VAR_CREATED_USER);
        columnList.add(VAR_MODIFIED_DATE);
        columnList.add(VAR_MODIFIED_BY);
        columnList.add(VAR_DESC);
        setColumnOrder(columnList.toArray());
        alignColumns();
    }

    @Override
    protected void setHiddenColumns() {
        final List<Object> columnsToBeHidden = new ArrayList<>();
        columnsToBeHidden.add(VAR_NAME);
        columnsToBeHidden.add(VAR_CREATED_DATE);
        columnsToBeHidden.add(VAR_CREATED_USER);
        columnsToBeHidden.add(VAR_MODIFIED_DATE);
        columnsToBeHidden.add(VAR_MODIFIED_BY);
        columnsToBeHidden.add(VAR_DESC);
        columnsToBeHidden.add(IS_REQUIRED_MIGRATION_STEP);
        columnsToBeHidden.add(DS_TYPE);
        columnsToBeHidden.add(SW_MODULES);
        for (final Object propertyId : columnsToBeHidden) {
            getColumn(propertyId).setHidden(true);
        }
    }

    @Override
    protected CellDescriptionGenerator getDescriptionGenerator() {
        return this::getDescription;
    }

    @Override
    protected void addColumnRenderes() {
        getColumn(VAR_NUMBER_OF_GROUPS).setRenderer(new HtmlRenderer(), new TotalTargetGroupsConverter());
        getColumn(VAR_TOTAL_TARGETS_COUNT_STATUS).setRenderer(new HtmlRenderer(),
                new TotalTargetCountStatusConverter());

        getColumn(VAR_STATUS).setRenderer(new HtmlLabelRenderer(), new RolloutStatusConverter());

        final RolloutRenderer customObjectRenderer = new RolloutRenderer(RolloutRendererData.class);
        customObjectRenderer.addClickListener(this::onClickOfRolloutName);
        getColumn(ROLLOUT_RENDERER_DATA).setRenderer(customObjectRenderer);

        getColumn(RUN_OPTION)
                .setRenderer(new HtmlButtonRenderer(clickEvent -> startOrResumeRollout((Long) clickEvent.getItemId())));

        getColumn(PAUSE_OPTION)
                .setRenderer(new HtmlButtonRenderer(clickEvent -> pauseRollout((Long) clickEvent.getItemId())));

        if (permissionChecker.hasRolloutUpdatePermission()) {
            getColumn(UPDATE_OPTION)
                    .setRenderer(new HtmlButtonRenderer(clickEvent -> updateRollout((Long) clickEvent.getItemId())));
        }

    }

    private void alignColumns() {
        setCellStyleGenerator(new RollouStatusCellStyleGenerator(getContainerDataSource()));
    }

    private void onClickOfRolloutName(final RendererClickEvent event) {
        rolloutUIState.setRolloutId((long) event.getItemId());
        final String rolloutName = (String) getContainerDataSource().getItem(event.getItemId())
                .getItemProperty(VAR_NAME).getValue();
        rolloutUIState.setRolloutName(rolloutName);
        final String ds = (String) getContainerDataSource().getItem(event.getItemId())
                .getItemProperty(VAR_DIST_NAME_VERSION).getValue();
        rolloutUIState.setRolloutDistributionSet(ds);
        eventBus.publish(this, RolloutEvent.SHOW_ROLLOUT_GROUPS);
    }

    private void pauseRollout(final Long rolloutId) {
        final Item row = getContainerDataSource().getItem(rolloutId);

        final RolloutStatus rolloutStatus = (RolloutStatus) row.getItemProperty(VAR_STATUS).getValue();

        if (!RolloutStatus.RUNNING.equals(rolloutStatus)) {
            return;
        }

        final String rolloutName = (String) row.getItemProperty(VAR_NAME).getValue();

        rolloutManagement.pauseRollout(rolloutId);
        uiNotification.displaySuccess(i18n.get("message.rollout.paused", rolloutName));
    }

    private void startOrResumeRollout(final Long rolloutId) {
        final Item row = getContainerDataSource().getItem(rolloutId);

        final RolloutStatus rolloutStatus = (RolloutStatus) row.getItemProperty(VAR_STATUS).getValue();
        final String rolloutName = (String) row.getItemProperty(VAR_NAME).getValue();

        if (RolloutStatus.READY.equals(rolloutStatus)) {
            rolloutManagement.startRollout(rolloutId);
            uiNotification.displaySuccess(i18n.get("message.rollout.started", rolloutName));
            return;
        }

        if (RolloutStatus.PAUSED.equals(rolloutStatus)) {
            rolloutManagement.resumeRollout(rolloutId);
            uiNotification.displaySuccess(i18n.get("message.rollout.resumed", rolloutName));
            return;
        }
    }

    private void updateRollout(final Long rolloutId) {
        final CommonDialogWindow addTargetWindow = addUpdateRolloutWindow.getWindow(rolloutId);
        addTargetWindow.setCaption(i18n.get("caption.update.rollout"));
        UI.getCurrent().addWindow(addTargetWindow);
        addTargetWindow.setVisible(Boolean.TRUE);
    }

    private String getDescription(final CellReference cell) {

        String description = null;

        if (VAR_STATUS.equals(cell.getPropertyId())) {
            description = cell.getProperty().getValue().toString().toLowerCase();
        } else if (ACTION.equals(cell.getPropertyId())) {
            description = ACTION.toLowerCase();
        } else if (ROLLOUT_RENDERER_DATA.equals(cell.getPropertyId())) {
            description = ((RolloutRendererData) cell.getProperty().getValue()).getName();
        } else if (VAR_TOTAL_TARGETS_COUNT_STATUS.equals(cell.getPropertyId())) {
            description = getTooltip(((TotalTargetCountStatus) cell.getValue()).getStatusTotalCountMap());
        } else if (VAR_DIST_NAME_VERSION.equals(cell.getPropertyId())) {
            description = getDSDetails(cell.getItem());
        }

        return description;
    }

    private static String getDSDetails(final Item rolloutItem) {
        final StringBuilder swModuleNames = new StringBuilder();
        final StringBuilder swModuleVendors = new StringBuilder();
        final Set<SoftwareModule> swModules = (Set<SoftwareModule>) rolloutItem.getItemProperty(SW_MODULES).getValue();
        swModules.forEach(swModule -> {
            swModuleNames.append(swModule.getName());
            swModuleNames.append(" , ");
            swModuleVendors.append(swModule.getVendor());
            swModuleVendors.append(" , ");
        });
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(HTML_UL_OPEN_TAG);
        stringBuilder.append(HTML_LI_OPEN_TAG);
        stringBuilder.append(" DistributionSet Description : ")
                .append((String) rolloutItem.getItemProperty(VAR_DESC).getValue());
        stringBuilder.append(HTML_LI_CLOSE_TAG);
        stringBuilder.append(HTML_LI_OPEN_TAG);
        stringBuilder.append(" DistributionSet Type : ")
                .append((String) rolloutItem.getItemProperty(DS_TYPE).getValue());
        stringBuilder.append(HTML_LI_CLOSE_TAG);
        stringBuilder.append(HTML_LI_OPEN_TAG);
        stringBuilder.append("Required Migration step : ")
                .append((boolean) rolloutItem.getItemProperty(IS_REQUIRED_MIGRATION_STEP).getValue() ? "Yes" : "No");
        stringBuilder.append(HTML_LI_CLOSE_TAG);

        stringBuilder.append(HTML_LI_OPEN_TAG);
        stringBuilder.append("SoftWare Modules : ").append(swModuleNames.toString());
        stringBuilder.append(HTML_LI_CLOSE_TAG);

        stringBuilder.append(HTML_LI_OPEN_TAG);
        stringBuilder.append("Vendor(s) : ").append(swModuleVendors.toString());
        stringBuilder.append(HTML_LI_CLOSE_TAG);

        stringBuilder.append(HTML_UL_CLOSE_TAG);
        return stringBuilder.toString();
    }

    private static class RollouStatusCellStyleGenerator implements CellStyleGenerator {

        private static final long serialVersionUID = 1L;
        /**
         * Contains all expected rollout status per column to enable or disable
         * the button.
         */
        private static final Map<String, RolloutStatus> EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON = Maps
                .newHashMapWithExpectedSize(2);
        private final Container.Indexed containerDataSource;

        static {
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(RUN_OPTION, RolloutStatus.READY);
            EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.put(PAUSE_OPTION, RolloutStatus.RUNNING);
        }

        /**
         * Constructor
         *
         * @param containerDataSource
         *            the container
         */
        public RollouStatusCellStyleGenerator(final Container.Indexed containerDataSource) {
            this.containerDataSource = containerDataSource;
        }

        @Override
        public String getStyle(final CellReference cellReference) {
            if (VAR_STATUS.equals(cellReference.getPropertyId())) {
                return "centeralign";
            }
            return convertRolloutStatusToString(cellReference);
        }

        private String convertRolloutStatusToString(final CellReference cellReference) {
            final Object propertyId = cellReference.getPropertyId();
            final RolloutStatus expectedRolloutStatus = EXPECTED_ROLLOUT_STATUS_ENABLE_BUTTON.get(propertyId);
            if (expectedRolloutStatus == null) {
                return null;
            }

            if (RUN_OPTION.equals(cellReference.getPropertyId())) {
                return getStatus(cellReference, RolloutStatus.READY, RolloutStatus.PAUSED);
            }

            if (PAUSE_OPTION.equals(cellReference.getPropertyId())) {
                return getStatus(cellReference, RolloutStatus.RUNNING);
            }

            return null;
        }

        private String getStatus(final CellReference cellReference, final RolloutStatus... expectedRolloutStatus) {
            final RolloutStatus currentRolloutStatus = getRolloutStatus(cellReference.getItemId());

            if (Arrays.asList(expectedRolloutStatus).contains(currentRolloutStatus)) {
                return null;
            }

            return org.eclipse.hawkbit.ui.customrenderers.client.renderers.HtmlButtonRenderer.DISABLE_VALUE;
        }

        private RolloutStatus getRolloutStatus(final Object itemId) {
            final Item row = containerDataSource.getItem(itemId);
            return (RolloutStatus) row.getItemProperty(VAR_STATUS).getValue();
        }
    }

    /**
     *
     * Converter to convert {@link RolloutStatus} to string.
     *
     */
    class RolloutStatusConverter implements Converter<String, RolloutStatus> {

        private static final long serialVersionUID = -1217685750825632678L;

        @Override
        public RolloutStatus convertToModel(final String value, final Class<? extends RolloutStatus> targetType,
                final Locale locale) {
            return null;
        }

        @Override
        public String convertToPresentation(final RolloutStatus value, final Class<? extends String> targetType,
                final Locale locale) {
            return convertRolloutStatusToString(value);
        }

        @Override
        public Class<RolloutStatus> getModelType() {
            return RolloutStatus.class;
        }

        @Override
        public Class<String> getPresentationType() {
            return String.class;
        }

        private String convertRolloutStatusToString(final RolloutStatus value) {
            final StatusFontIcon statusFontIcon = statusIconMap.get(value);
            final String codePoint = HawkbitCommonUtil.getCodePoint(statusFontIcon);
            return HawkbitCommonUtil.getStatusLabelDetailsInString(codePoint, statusFontIcon.getStyle(),
                    UIComponentIdProvider.ROLLOUT_STATUS_LABEL_ID);
        }
    }

    /**
     * Converter to convert {@link TotalTargetCountStatus} to formatted string
     * with status and count details.
     *
     */
    class TotalTargetCountStatusConverter implements Converter<String, TotalTargetCountStatus> {

        private static final long serialVersionUID = -5794528427855153924L;

        @Override
        public TotalTargetCountStatus convertToModel(final String value,
                final Class<? extends TotalTargetCountStatus> targetType, final Locale locale) {
            return null;
        }

        @Override
        public String convertToPresentation(final TotalTargetCountStatus value,
                final Class<? extends String> targetType, final Locale locale) {
            return DistributionBarHelper.getDistributionBarAsHTMLString(value.getStatusTotalCountMap());
        }

        @Override
        public Class<TotalTargetCountStatus> getModelType() {
            return TotalTargetCountStatus.class;
        }

        @Override
        public Class<String> getPresentationType() {
            return String.class;
        }
    }

    /**
     * Converter to convert 0 to empty, if total target groups is zero.
     *
     */
    class TotalTargetGroupsConverter implements Converter<String, Long> {

        private static final long serialVersionUID = 6589305227035220369L;

        @Override
        public Long convertToModel(final String value, final Class<? extends Long> targetType, final Locale locale) {
            return null;
        }

        @Override
        public String convertToPresentation(final Long value, final Class<? extends String> targetType,
                final Locale locale) {
            if (value == 0) {
                return "";
            }
            return value.toString();
        }

        @Override
        public Class<Long> getModelType() {
            return Long.class;
        }

        @Override
        public Class<String> getPresentationType() {
            return String.class;
        }
    }

}
