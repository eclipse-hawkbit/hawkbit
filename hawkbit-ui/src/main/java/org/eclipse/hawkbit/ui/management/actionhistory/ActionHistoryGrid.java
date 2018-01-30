/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.actionhistory;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.customrenderers.renderers.AbstractGridButtonConverter;
import org.eclipse.hawkbit.ui.customrenderers.renderers.AbstractHtmlLabelConverter;
import org.eclipse.hawkbit.ui.customrenderers.renderers.GridButtonRenderer;
import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlLabelRenderer;
import org.eclipse.hawkbit.ui.management.actionhistory.ProxyAction.IsActiveDecoration;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.PinUnpinEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.rollout.StatusFontIcon;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.collect.Maps;
import com.vaadin.data.Item;
import com.vaadin.data.util.GeneratedPropertyContainer;
import com.vaadin.data.util.PropertyValueGenerator;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.UI;

/**
 * This grid presents the action history for a selected target.
 */
public class ActionHistoryGrid extends AbstractGrid<LazyQueryContainer> {
    private static final long serialVersionUID = 4324796883957831443L;

    private static final Logger LOG = LoggerFactory.getLogger(ActionHistoryGrid.class);
    private static final String BUTTON_CANCEL = "button.cancel";
    private static final String BUTTON_OK = "button.ok";
    private static final double FIXED_PIX_MIN = 25;
    private static final double FIXED_PIX_MAX = 32;

    private static final String STATUS_ICON_GREEN = "statusIconGreen";
    private static final String STATUS_ICON_RED = "statusIconRed";
    private static final String STATUS_ICON_ORANGE = "statusIconOrange";
    private static final String STATUS_ICON_PENDING = "statusIconPending";
    private static final String STATUS_ICON_NEUTRAL = "statusIconNeutral";
    private static final String STATUS_ICON_ACTIVE = "statusIconActive";
    private static final String STATUS_ICON_FORCED = "statusIconForced";

    private static final String VIRT_PROP_FORCED = "forced";
    private static final String VIRT_PROP_TIMEFORCED = "timeForced";
    private static final String VIRT_PROP_ACTION_CANCEL = "cancel-action";
    private static final String VIRT_PROP_ACTION_FORCE = "force-action";
    private static final String VIRT_PROP_ACTION_FORCE_QUIT = "force-quit-action";

    private static final Object[] maxColumnOrder = new Object[] { ProxyAction.PXY_ACTION_IS_ACTIVE_DECO,
            ProxyAction.PXY_ACTION_ID, ProxyAction.PXY_ACTION_DS_NAME_VERSION, ProxyAction.PXY_ACTION_LAST_MODIFIED_AT,
            ProxyAction.PXY_ACTION_STATUS, ProxyAction.PXY_ACTION_ROLLOUT_NAME, VIRT_PROP_FORCED, VIRT_PROP_TIMEFORCED,
            VIRT_PROP_ACTION_CANCEL, VIRT_PROP_ACTION_FORCE, VIRT_PROP_ACTION_FORCE_QUIT };

    private static final Object[] minColumnOrder = new Object[] { ProxyAction.PXY_ACTION_IS_ACTIVE_DECO,
            ProxyAction.PXY_ACTION_DS_NAME_VERSION, ProxyAction.PXY_ACTION_LAST_MODIFIED_AT,
            ProxyAction.PXY_ACTION_STATUS, VIRT_PROP_FORCED, VIRT_PROP_TIMEFORCED, VIRT_PROP_ACTION_CANCEL,
            VIRT_PROP_ACTION_FORCE, VIRT_PROP_ACTION_FORCE_QUIT };

    private static final String[] leftAlignedColumns = new String[] { VIRT_PROP_TIMEFORCED };

    private static final String[] centerAlignedColumns = new String[] { ProxyAction.PXY_ACTION_IS_ACTIVE_DECO,
            ProxyAction.PXY_ACTION_STATUS };

    private static final String[] rightAlignedColumns = new String[] { VIRT_PROP_FORCED, ProxyAction.PXY_ACTION_ID };

    private final transient DeploymentManagement deploymentManagement;
    private final UINotification notification;
    private final ManagementUIState managementUIState;

    private Target selectedTarget;
    private final AlignCellStyleGenerator alignGenerator;
    private final ModifiedTimeTooltipGenerator modTimetooltipGenerator;

    private final Map<Action.Status, StatusFontIcon> states;
    private final Map<IsActiveDecoration, StatusFontIcon> activeStates;

    private final BeanQueryFactory<ActionBeanQuery> targetQF = new BeanQueryFactory<>(ActionBeanQuery.class);

    boolean forceClientRefreshToggle = true;

    /**
     * Constructor.
     *
     * @param i18n
     * @param deploymentManagement
     * @param eventBus
     * @param notification
     * @param managementUIState
     */
    protected ActionHistoryGrid(final VaadinMessageSource i18n, final DeploymentManagement deploymentManagement,
            final UIEventBus eventBus, final UINotification notification, final ManagementUIState managementUIState) {
        super(i18n, eventBus, null);
        this.deploymentManagement = deploymentManagement;
        this.notification = notification;
        this.managementUIState = managementUIState;

        setMaximizeSupport(new ActionHistoryMaximizeSupport());
        setSingleSelectionSupport(new SingleSelectionSupport());

        if (!managementUIState.isActionHistoryMaximized()) {
            getSingleSelectionSupport().disable();
        }

        setGeneratedPropertySupport(new ActionHistoryGeneratedPropertySupport());
        setDetailsSupport(new DetailsSupport());

        final LabelConfig conf = new LabelConfig();
        states = conf.createStatusLabelConfig(i18n, UIComponentIdProvider.ACTION_HISTORY_TABLE_STATUS_LABEL_ID);
        activeStates = conf
                .createActiveStatusLabelConfig(UIComponentIdProvider.ACTION_HISTORY_TABLE_ACTIVESTATE_LABEL_ID);
        alignGenerator = new AlignCellStyleGenerator(leftAlignedColumns, centerAlignedColumns, rightAlignedColumns);
        modTimetooltipGenerator = new ModifiedTimeTooltipGenerator(ProxyAction.PXY_ACTION_LAST_MODIFIED_AT);

        init();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final ManagementUIEvent mgmtUIEvent) {
        if (mgmtUIEvent == ManagementUIEvent.MAX_ACTION_HISTORY) {
            UI.getCurrent().access(this::createMaximizedContent);
        }
        if (mgmtUIEvent == ManagementUIEvent.MIN_ACTION_HISTORY) {
            UI.getCurrent().access(this::createMinimizedContent);
        }
    }

    @Override
    protected void init() {
        super.init();
        restorePreviousState();
    }

    /**
     * Set target as member of this grid (as all presented grid-data is related
     * to this target) and recalculate grid-data for this target.
     *
     * @param selectedTarget
     *            reference of target
     */
    public void populateSelectedTarget(final Target selectedTarget) {
        this.selectedTarget = selectedTarget;
        getDetailsSupport()
                .populateMasterDataAndRecalculateContainer(selectedTarget != null ? selectedTarget.getId() : null);
    }

    @Override
    protected LazyQueryContainer createContainer() {
        configureQueryFactory();
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, ProxyAction.PXY_ACTION_ID), targetQF);
    }

    @Override
    public void refreshContainer() {
        configureQueryFactory();
        super.refreshContainer();
    }

    protected void configureQueryFactory() {
        // ADD all the filters to the query config
        final Map<String, Object> queryConfig = Maps.newHashMapWithExpectedSize(1);
        queryConfig.put(SPUIDefinitions.ACTIONS_BY_TARGET,
                selectedTarget != null ? selectedTarget.getControllerId() : null);
        // Create ActionBeanQuery factory with the query config.
        targetQF.setQueryConfiguration(queryConfig);
    }

    @Override
    protected void addContainerProperties() {
        final LazyQueryContainer rawCont = getGeneratedPropertySupport().getRawContainer();

        rawCont.addContainerProperty(ProxyAction.PXY_ACTION_IS_ACTIVE_DECO, IsActiveDecoration.class, null, true,
                false);
        rawCont.addContainerProperty(ProxyAction.PXY_ACTION, Action.class, null, true, false);
        rawCont.addContainerProperty(ProxyAction.PXY_ACTION_DS_NAME_VERSION, String.class, null, true, false);
        rawCont.addContainerProperty(ProxyAction.PXY_ACTION_LAST_MODIFIED_AT, Long.class, null, true, true);
        rawCont.addContainerProperty(ProxyAction.PXY_ACTION_STATUS, Action.Status.class, null, true, false);

        rawCont.addContainerProperty(ProxyAction.PXY_ACTION_ID, String.class, null, true, true);
        rawCont.addContainerProperty(ProxyAction.PXY_ACTION_ROLLOUT_NAME, String.class, null, true, true);
    }

    @Override
    protected String getGridId() {
        return UIComponentIdProvider.ACTION_HISTORY_GRID_ID;
    }

    @Override
    protected void addColumnRenderes() {
        getColumn(ProxyAction.PXY_ACTION_LAST_MODIFIED_AT).setConverter(new LongToFormattedDateStringConverter());
        getColumn(ProxyAction.PXY_ACTION_STATUS).setRenderer(new HtmlLabelRenderer(),
                new HtmlStatusLabelConverter(this::createStatusLabelMetadata));
        getColumn(ProxyAction.PXY_ACTION_IS_ACTIVE_DECO).setRenderer(new HtmlLabelRenderer(),
                new HtmlIsActiveLabelConverter(this::createIsActiveLabelMetadata));
        getColumn(VIRT_PROP_FORCED).setRenderer(new HtmlLabelRenderer(),
                new HtmlVirtPropLabelConverter(ActionHistoryGrid::createForcedLabelMetadata));
        getColumn(VIRT_PROP_TIMEFORCED).setRenderer(new HtmlLabelRenderer(),
                new HtmlVirtPropLabelConverter(this::createTimeForcedLabelMetadata));
        getColumn(VIRT_PROP_ACTION_CANCEL).setRenderer(
                new GridButtonRenderer(clickEvent -> confirmAndCancelAction((Long) clickEvent.getItemId())),
                new ActionGridButtonConverter(this::createCancelButtonMetadata));
        getColumn(VIRT_PROP_ACTION_FORCE).setRenderer(
                new GridButtonRenderer(clickEvent -> confirmAndForceAction((Long) clickEvent.getItemId())),
                new ActionGridButtonConverter(this::createForceButtonMetadata));
        getColumn(VIRT_PROP_ACTION_FORCE_QUIT).setRenderer(
                new GridButtonRenderer(clickEvent -> confirmAndForceQuitAction((Long) clickEvent.getItemId())),
                new ActionGridButtonConverter(this::createForceQuitButtonMetadata));
    }

    private StatusFontIcon createCancelButtonMetadata(final Action action) {
        final boolean isDisabled = !action.isActive() || action.isCancelingOrCanceled();
        return new StatusFontIcon(FontAwesome.TIMES, STATUS_ICON_NEUTRAL, i18n.getMessage("message.cancel.action"),
                UIComponentIdProvider.ACTION_HISTORY_TABLE_CANCEL_ID, isDisabled);
    }

    private StatusFontIcon createForceButtonMetadata(final Action action) {
        final boolean isDisabled = !action.isActive() || action.isForce() || action.isCancelingOrCanceled();
        return new StatusFontIcon(FontAwesome.BOLT, STATUS_ICON_NEUTRAL, i18n.getMessage("message.force.action"),
                UIComponentIdProvider.ACTION_HISTORY_TABLE_FORCE_ID, isDisabled);
    }

    private StatusFontIcon createForceQuitButtonMetadata(final Action action) {
        final boolean isDisabled = !action.isActive() || !action.isCancelingOrCanceled();
        return new StatusFontIcon(FontAwesome.TIMES, STATUS_ICON_RED, i18n.getMessage("message.forcequit.action"),
                UIComponentIdProvider.ACTION_HISTORY_TABLE_FORCE_QUIT_ID, isDisabled);
    }

    private StatusFontIcon createStatusLabelMetadata(final Action.Status status) {
        return states.get(status);
    }

    private StatusFontIcon createIsActiveLabelMetadata(final IsActiveDecoration isActiveDeco) {
        return activeStates.get(isActiveDeco);
    }

    private static StatusFontIcon createForcedLabelMetadata(final Action action) {
        StatusFontIcon result = null;
        if (ActionType.FORCED.equals(action.getActionType()) || ActionType.TIMEFORCED.equals(action.getActionType())) {
            result = new StatusFontIcon(FontAwesome.BOLT, STATUS_ICON_FORCED, "Forced",
                    UIComponentIdProvider.ACTION_HISTORY_TABLE_FORCED_LABEL_ID);
        }
        return result;
    }

    private StatusFontIcon createTimeForcedLabelMetadata(final Action action) {
        StatusFontIcon result = null;

        if (ActionType.TIMEFORCED.equals(action.getActionType())) {
            final long currentTimeMillis = System.currentTimeMillis();
            String style;
            String title;
            if (action.isHitAutoForceTime(currentTimeMillis)) {
                style = STATUS_ICON_GREEN;
                title = "auto forced since "
                        + SPDateTimeUtil.getDurationFormattedString(action.getForcedTime(), currentTimeMillis, i18n);
            } else {
                style = STATUS_ICON_PENDING;
                title = "auto forcing in "
                        + SPDateTimeUtil.getDurationFormattedString(currentTimeMillis, action.getForcedTime(), i18n);
            }
            result = new StatusFontIcon(FontAwesome.HISTORY, style, title,
                    UIComponentIdProvider.ACTION_HISTORY_TABLE_TIMEFORCED_LABEL_ID);
        }
        return result;
    }

    /**
     * Show confirmation window and if ok then only, force the action.
     *
     * @param actionId
     *            as Id if the action needs to be forced.
     */
    private void confirmAndForceAction(final Long actionId) {
        /* Display the confirmation */
        final ConfirmationDialog confirmDialog = new ConfirmationDialog(
                i18n.getMessage("caption.force.action.confirmbox"), i18n.getMessage("message.force.action.confirm"),
                i18n.getMessage(BUTTON_OK), i18n.getMessage(BUTTON_CANCEL), ok -> {
                    if (!ok) {
                        return;
                    }
                    deploymentManagement.forceTargetAction(actionId);
                    populateAndUpdateTargetDetails(selectedTarget);
                    notification.displaySuccess(i18n.getMessage("message.force.action.success"));
                });
        UI.getCurrent().addWindow(confirmDialog.getWindow());

        confirmDialog.getWindow().bringToFront();
    }

    /**
     * Show confirmation window and if ok then only, force quit action.
     *
     * @param actionId
     *            as Id if the action needs to be forced.
     */
    private void confirmAndForceQuitAction(final Long actionId) {
        /* Display the confirmation */
        final ConfirmationDialog confirmDialog = new ConfirmationDialog(
                i18n.getMessage("caption.forcequit.action.confirmbox"),
                i18n.getMessage("message.forcequit.action.confirm"), i18n.getMessage(BUTTON_OK),
                i18n.getMessage(BUTTON_CANCEL), ok -> {
                    if (!ok) {
                        return;
                    }
                    final boolean cancelResult = forceQuitActiveAction(actionId);
                    if (cancelResult) {
                        populateAndUpdateTargetDetails(selectedTarget);
                        notification.displaySuccess(i18n.getMessage("message.forcequit.action.success"));
                    } else {
                        notification.displayValidationError(i18n.getMessage("message.forcequit.action.failed"));
                    }
                }, FontAwesome.WARNING);
        UI.getCurrent().addWindow(confirmDialog.getWindow());

        confirmDialog.getWindow().bringToFront();
    }

    /**
     * Show confirmation window and if ok then only, cancel the action.
     *
     * @param actionId
     *            as Id if the action needs to be cancelled.
     */
    private void confirmAndCancelAction(final Long actionId) {
        if (actionId == null) {
            return;
        }

        final ConfirmationDialog confirmDialog = new ConfirmationDialog(
                i18n.getMessage("caption.cancel.action.confirmbox"), i18n.getMessage("message.cancel.action.confirm"),
                i18n.getMessage(BUTTON_OK), i18n.getMessage(BUTTON_CANCEL), ok -> {
                    if (!ok) {
                        return;
                    }
                    final boolean cancelResult = cancelActiveAction(actionId);
                    if (cancelResult) {
                        populateAndUpdateTargetDetails(selectedTarget);
                        notification.displaySuccess(i18n.getMessage("message.cancel.action.success"));
                    } else {
                        notification.displayValidationError(i18n.getMessage("message.cancel.action.failed"));
                    }
                });
        UI.getCurrent().addWindow(confirmDialog.getWindow());
        confirmDialog.getWindow().bringToFront();
    }

    private void populateAndUpdateTargetDetails(final Target target) {
        // show the updated target action history details
        populateSelectedTarget(target);
        // update the target table and its pinning details
        updateTargetAndDsTable();
    }

    private void updateTargetAndDsTable() {
        eventBus.publish(this, new TargetTableEvent(BaseEntityEventType.UPDATED_ENTITY, selectedTarget));
        updateDistributionTableStyle();
    }

    /**
     * Update the colors of Assigned and installed distribution set in Target
     * Pinning.
     */
    private void updateDistributionTableStyle() {
        managementUIState.getDistributionTableFilters().getPinnedTarget().ifPresent(pinnedTarget -> {
            if (pinnedTarget.getTargetId().equals(selectedTarget.getId())) {
                eventBus.publish(this, PinUnpinEvent.PIN_TARGET);
            }
        });
        if (!managementUIState.getDistributionTableFilters().getPinnedTarget().isPresent()) {
            return;
        }
    }

    // service call to cancel the active action
    private boolean cancelActiveAction(final Long actionId) {
        if (actionId != null) {
            try {
                deploymentManagement.cancelAction(actionId);
                return true;
            } catch (final CancelActionNotAllowedException e) {
                LOG.info("Cancel action not allowed exception :{}", e);
                return false;
            }
        }
        return false;
    }

    // service call to cancel the active action
    private boolean forceQuitActiveAction(final Long actionId) {
        if (actionId != null) {
            try {
                deploymentManagement.forceQuitAction(actionId);
                return true;
            } catch (final CancelActionNotAllowedException e) {
                LOG.info("Force Cancel action not allowed exception :{}", e);
                return false;
            }
        }
        return false;
    }

    @Override
    protected void setHiddenColumns() {
        getColumn(VIRT_PROP_FORCED).setHidable(false);
        getColumn(VIRT_PROP_TIMEFORCED).setHidable(false);
        getColumn(VIRT_PROP_ACTION_CANCEL).setHidable(false);
        getColumn(VIRT_PROP_ACTION_FORCE).setHidable(false);
        getColumn(VIRT_PROP_ACTION_FORCE_QUIT).setHidable(false);
    }

    @Override
    protected CellDescriptionGenerator getDescriptionGenerator() {
        return modTimetooltipGenerator;
    }

    @Override
    protected void setColumnHeaderNames() {
        final HeaderRow newHeaderRow = resetHeaderDefaultRow();

        getColumn(ProxyAction.PXY_ACTION_IS_ACTIVE_DECO).setHeaderCaption(SPUIDefinitions.ACTION_HIS_TBL_ACTIVE);
        getColumn(ProxyAction.PXY_ACTION_DS_NAME_VERSION).setHeaderCaption(SPUIDefinitions.ACTION_HIS_TBL_DIST);
        getColumn(ProxyAction.PXY_ACTION_LAST_MODIFIED_AT).setHeaderCaption(SPUIDefinitions.ACTION_HIS_TBL_DATETIME);
        getColumn(ProxyAction.PXY_ACTION_STATUS).setHeaderCaption(SPUIDefinitions.ACTION_HIS_TBL_STATUS);
        getColumn(VIRT_PROP_FORCED).setHeaderCaption(String.valueOf(forceClientRefreshToggle));
        forceClientRefreshToggle = !forceClientRefreshToggle;

        newHeaderRow.join(VIRT_PROP_FORCED, VIRT_PROP_TIMEFORCED).setText(SPUIDefinitions.ACTION_HIS_TBL_FORCED);
        newHeaderRow.join(VIRT_PROP_ACTION_CANCEL, VIRT_PROP_ACTION_FORCE, VIRT_PROP_ACTION_FORCE_QUIT)
                .setText(SPUIDefinitions.ACTIONS_COLUMN);
    }

    @Override
    protected void setColumnExpandRatio() {
        setColumnsSize(50.0, 50.0, ProxyAction.PXY_ACTION_IS_ACTIVE_DECO);
        setColumnsSize(107.0, 500.0, ProxyAction.PXY_ACTION_DS_NAME_VERSION);
        setColumnsSize(100.0, 120.0, ProxyAction.PXY_ACTION_LAST_MODIFIED_AT);
        setColumnsSize(53.0, 55.0, ProxyAction.PXY_ACTION_STATUS);
        setColumnsSize(FIXED_PIX_MIN, FIXED_PIX_MIN, VIRT_PROP_FORCED, VIRT_PROP_TIMEFORCED, VIRT_PROP_ACTION_CANCEL,
                VIRT_PROP_ACTION_FORCE, VIRT_PROP_ACTION_FORCE_QUIT);
    }

    /**
     * Conveniently sets min- and max-width for a bunch of columns.
     *
     * @param min
     *            minimum width
     * @param max
     *            maximum width
     * @param columnPropertyIds
     *            all the columns the min and max should be set for.
     */
    private void setColumnsSize(final double min, final double max, final String... columnPropertyIds) {
        for (final String columnPropertyId : columnPropertyIds) {
            getColumn(columnPropertyId).setMinimumWidth(min);
            getColumn(columnPropertyId).setMaximumWidth(max);
        }
    }

    /**
     * Creates the grid content for maximized-state.
     */
    private void createMaximizedContent() {
        getSingleSelectionSupport().enable();
        getDetailsSupport().populateSelection();
        getMaximizeSupport().createMaximizedContent();
        recalculateColumnWidths();
    }

    /**
     * Creates the grid content for normal (minimized) state.
     */
    private void createMinimizedContent() {
        getSingleSelectionSupport().disable();
        getMaximizeSupport().createMinimizedContent();
        recalculateColumnWidths();
    }

    @Override
    protected void setColumnProperties() {
        clearSortOrder();
        setColumns(minColumnOrder);
        alignColumns();
    }

    /**
     * Restores the maximized state if the action history was left in
     * maximized-state and is now re-entered.
     */
    private void restorePreviousState() {
        if (managementUIState.isActionHistoryMaximized()) {
            createMaximizedContent();
        }
    }

    /**
     * Sets the alignment cell-style-generator that handles the alignment for
     * the grid cells.
     */
    private void alignColumns() {
        setCellStyleGenerator(alignGenerator);
    }

    /**
     * Adds support for virtual properties (aka generated properties)
     */
    class ActionHistoryGeneratedPropertySupport extends AbstractGeneratedPropertySupport {

        @Override
        public GeneratedPropertyContainer getDecoratedContainer() {
            return (GeneratedPropertyContainer) getContainerDataSource();
        }

        @Override
        public LazyQueryContainer getRawContainer() {
            return (LazyQueryContainer) (getDecoratedContainer()).getWrappedContainer();
        }

        @Override
        protected GeneratedPropertyContainer addGeneratedContainerProperties() {
            final GeneratedPropertyContainer decoratedContainer = getDecoratedContainer();

            decoratedContainer.addGeneratedProperty(VIRT_PROP_FORCED, new GenericPropertyValueGenerator());
            decoratedContainer.addGeneratedProperty(VIRT_PROP_TIMEFORCED, new GenericPropertyValueGenerator());
            decoratedContainer.addGeneratedProperty(VIRT_PROP_ACTION_CANCEL, new GenericPropertyValueGenerator());
            decoratedContainer.addGeneratedProperty(VIRT_PROP_ACTION_FORCE, new GenericPropertyValueGenerator());
            decoratedContainer.addGeneratedProperty(VIRT_PROP_ACTION_FORCE_QUIT, new GenericPropertyValueGenerator());

            return decoratedContainer;
        }
    }

    /**
     * Adds support to maximize the grid.
     */
    class ActionHistoryMaximizeSupport extends AbstractMaximizeSupport {

        /**
         * Sets the property-ids available in maximized-state.
         */
        @Override
        protected void setMaximizedColumnProperties() {
            clearSortOrder();
            setColumns(maxColumnOrder);
            alignColumns();
        }

        @Override
        protected void setMaximizedHiddenColumns() {
            getColumn(ProxyAction.PXY_ACTION_ID).setHidden(false);
            getColumn(ProxyAction.PXY_ACTION_ID).setHidable(true);
            getColumn(ProxyAction.PXY_ACTION_ROLLOUT_NAME).setHidden(false);
            getColumn(ProxyAction.PXY_ACTION_ROLLOUT_NAME).setHidable(true);
        }

        /**
         * Sets additional headers for the maximized-state.
         */
        @Override
        protected void setMaximizedHeaders() {
            getColumn(ProxyAction.PXY_ACTION_ID).setHeaderCaption(SPUIDefinitions.ACTION_HIS_TBL_ACTION_ID);
            getColumn(ProxyAction.PXY_ACTION_ROLLOUT_NAME)
                    .setHeaderCaption(SPUIDefinitions.ACTION_HIS_TBL_ROLLOUT_NAME);
        }

        /**
         * Sets the expand ratio for the maximized-state.
         */
        @Override
        protected void setMaximizedColumnExpandRatio() {
            /* set messages column can expand the rest of the available space */
            setColumnsSize(50.0, 50.0, ProxyAction.PXY_ACTION_IS_ACTIVE_DECO);
            setColumnsSize(FIXED_PIX_MIN, 100.0, ProxyAction.PXY_ACTION_ID);
            setColumnsSize(107.0, 500.0, ProxyAction.PXY_ACTION_DS_NAME_VERSION);
            setColumnsSize(100.0, 150.0, ProxyAction.PXY_ACTION_LAST_MODIFIED_AT);
            setColumnsSize(53.0, 55.0, ProxyAction.PXY_ACTION_STATUS);
            setColumnsSize(FIXED_PIX_MIN, FIXED_PIX_MAX, VIRT_PROP_FORCED, VIRT_PROP_TIMEFORCED,
                    VIRT_PROP_ACTION_CANCEL, VIRT_PROP_ACTION_FORCE, VIRT_PROP_ACTION_FORCE_QUIT);
            setColumnsSize(FIXED_PIX_MIN, 500.0, ProxyAction.PXY_ACTION_ROLLOUT_NAME);
        }
    }

    /**
     * Concrete html-label converter that handles IsActiveDecoration enum.
     */
    class HtmlIsActiveLabelConverter extends AbstractHtmlLabelConverter<IsActiveDecoration> {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor that sets the appropriate adapter.
         *
         * @param adapter
         *            adapts <code>IsActiveDecoration</code> to
         *            <code>String</code>
         */
        public HtmlIsActiveLabelConverter(final LabelAdapter<IsActiveDecoration> adapter) {
            addAdapter(adapter);
        }

        @Override
        public Class<IsActiveDecoration> getModelType() {
            return IsActiveDecoration.class;
        }
    }

    /**
     * Concrete html-label converter that handles Actions.
     */
    class HtmlVirtPropLabelConverter extends AbstractHtmlLabelConverter<Action> {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor that sets the appropriate adapter.
         *
         * @param adapter
         *            adapts <code>Action</code> to <code>String</code>
         */
        public HtmlVirtPropLabelConverter(final LabelAdapter<Action> adapter) {
            addAdapter(adapter);
        }

        @Override
        public Class<Action> getModelType() {
            return Action.class;
        }
    }

    /**
     * Concrete grid-button converter that handles Actions.
     */
    class ActionGridButtonConverter extends AbstractGridButtonConverter<Action> {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor that sets the appropriate adapter.
         *
         * @param adapter
         *            adapts <code>Action</code> to <code>StatusFontIcon</code>
         */
        public ActionGridButtonConverter(final GridButtonAdapter<Action> adapter) {
            addAdapter(adapter);
        }

        @Override
        public Class<Action> getModelType() {
            return Action.class;
        }
    }

    /**
     * Generator class responsible to retrieve an Action from the grid data in
     * order to generate a virtual property.
     */
    class GenericPropertyValueGenerator extends PropertyValueGenerator<Action> {
        private static final long serialVersionUID = 1L;

        @Override
        public Action getValue(final Item item, final Object itemId, final Object propertyId) {
            return (Action) item.getItemProperty(ProxyAction.PXY_ACTION).getValue();
        }

        @Override
        public Class<Action> getType() {
            return Action.class;
        }
    }

    /**
     * Configuration that holds the styling properties for label- and
     * button-renderers that are included in grid-cells.
     */
    public static class LabelConfig {

        /**
         * Initializes a map with all available status label metadata.
         *
         * @param i18n
         * @param statusLabelId
         * @return the configured map
         */
        public Map<Action.Status, StatusFontIcon> createStatusLabelConfig(final VaadinMessageSource i18n,
                final String statusLabelId) {
            final HashMap<Action.Status, StatusFontIcon> stateMap = Maps.newHashMapWithExpectedSize(9);
            stateMap.put(Action.Status.FINISHED, new StatusFontIcon(FontAwesome.CHECK_CIRCLE, STATUS_ICON_GREEN,
                    i18n.getMessage("label.finished"), statusLabelId));
            stateMap.put(Action.Status.ERROR, new StatusFontIcon(FontAwesome.EXCLAMATION_CIRCLE, STATUS_ICON_RED,
                    i18n.getMessage("label.error"), statusLabelId));
            stateMap.put(Action.Status.WARNING, new StatusFontIcon(FontAwesome.EXCLAMATION_CIRCLE, STATUS_ICON_ORANGE,
                    i18n.getMessage("label.warning"), statusLabelId));
            stateMap.put(Action.Status.RUNNING, new StatusFontIcon(FontAwesome.ADJUST, STATUS_ICON_PENDING,
                    i18n.getMessage("label.running"), statusLabelId));
            stateMap.put(Action.Status.CANCELING, new StatusFontIcon(FontAwesome.TIMES_CIRCLE, STATUS_ICON_PENDING,
                    i18n.getMessage("label.cancelling"), statusLabelId));
            stateMap.put(Action.Status.CANCELED, new StatusFontIcon(FontAwesome.TIMES_CIRCLE, STATUS_ICON_GREEN,
                    i18n.getMessage("label.cancelled"), statusLabelId));
            stateMap.put(Action.Status.RETRIEVED, new StatusFontIcon(FontAwesome.CIRCLE_O, STATUS_ICON_PENDING,
                    i18n.getMessage("label.retrieved"), statusLabelId));
            stateMap.put(Action.Status.DOWNLOAD, new StatusFontIcon(FontAwesome.CLOUD_DOWNLOAD, STATUS_ICON_PENDING,
                    i18n.getMessage("label.download"), statusLabelId));
            stateMap.put(Action.Status.SCHEDULED, new StatusFontIcon(FontAwesome.HOURGLASS_1, STATUS_ICON_PENDING,
                    i18n.getMessage("label.scheduled"), statusLabelId));
            return stateMap;
        }

        /**
         * Initializes a map with all available active-state metadata.
         *
         * @param activeStateId
         * @return the configured map
         */
        public Map<IsActiveDecoration, StatusFontIcon> createActiveStatusLabelConfig(final String activeStateId) {
            final HashMap<IsActiveDecoration, StatusFontIcon> activeStateMap = Maps.newHashMapWithExpectedSize(4);
            activeStateMap.put(IsActiveDecoration.SCHEDULED,
                    new StatusFontIcon(FontAwesome.HOURGLASS_1, STATUS_ICON_PENDING, "Scheduled", activeStateId));
            activeStateMap.put(IsActiveDecoration.ACTIVE,
                    new StatusFontIcon(null, STATUS_ICON_ACTIVE, "Active", activeStateId));
            activeStateMap.put(IsActiveDecoration.IN_ACTIVE,
                    new StatusFontIcon(FontAwesome.CHECK_CIRCLE, STATUS_ICON_NEUTRAL, "In-active", activeStateId));
            activeStateMap.put(IsActiveDecoration.IN_ACTIVE_ERROR,
                    new StatusFontIcon(FontAwesome.CHECK_CIRCLE, STATUS_ICON_RED, "In-active", activeStateId));
            return activeStateMap;
        }
    }
}
