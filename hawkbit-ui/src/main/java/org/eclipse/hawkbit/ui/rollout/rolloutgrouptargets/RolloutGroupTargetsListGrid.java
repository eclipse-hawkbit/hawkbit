/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rolloutgrouptargets;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupStatus;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlLabelRenderer;
import org.eclipse.hawkbit.ui.rollout.StatusFontIcon;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.util.converter.Converter;
import com.vaadin.server.FontAwesome;

/**
 * Grid component with targets of rollout group.
 */
public class RolloutGroupTargetsListGrid extends AbstractGrid<LazyQueryContainer> {

    private static final long serialVersionUID = -2244756637458984597L;

    private final RolloutUIState rolloutUIState;

    private static final Map<Status, StatusFontIcon> statusIconMap = new EnumMap<>(Status.class);

    static {
        statusIconMap.put(Status.FINISHED,
                new StatusFontIcon(FontAwesome.CHECK_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_GREEN));
        statusIconMap.put(Status.SCHEDULED,
                new StatusFontIcon(FontAwesome.HOURGLASS_1, SPUIStyleDefinitions.STATUS_ICON_PENDING));
        statusIconMap.put(Status.RUNNING,
                new StatusFontIcon(FontAwesome.ADJUST, SPUIStyleDefinitions.STATUS_ICON_YELLOW));
        statusIconMap.put(Status.RETRIEVED,
                new StatusFontIcon(FontAwesome.ADJUST, SPUIStyleDefinitions.STATUS_ICON_YELLOW));
        statusIconMap.put(Status.WARNING,
                new StatusFontIcon(FontAwesome.ADJUST, SPUIStyleDefinitions.STATUS_ICON_YELLOW));
        statusIconMap.put(Status.DOWNLOAD,
                new StatusFontIcon(FontAwesome.ADJUST, SPUIStyleDefinitions.STATUS_ICON_YELLOW));
        statusIconMap.put(Status.CANCELING,
                new StatusFontIcon(FontAwesome.TIMES_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_PENDING));
        statusIconMap.put(Status.CANCELED,
                new StatusFontIcon(FontAwesome.TIMES_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_GREEN));
        statusIconMap.put(Status.ERROR,
                new StatusFontIcon(FontAwesome.EXCLAMATION_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_RED));
    }

    /**
     * Constructor for RolloutGroupTargetsListGrid
     * 
     * @param i18n
     *            I18N
     * @param eventBus
     *            UIEventBus
     * @param rolloutUIState
     *            RolloutUIState
     */
    public RolloutGroupTargetsListGrid(final VaadinMessageSource i18n, final UIEventBus eventBus,
            final RolloutUIState rolloutUIState) {
        super(i18n, eventBus, null);
        this.rolloutUIState = rolloutUIState;

        init();
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final RolloutEvent event) {
        if (RolloutEvent.SHOW_ROLLOUT_GROUP_TARGETS != event) {
            return;
        }
        ((LazyQueryContainer) getContainerDataSource()).refresh();
        eventBus.publish(this, RolloutEvent.SHOW_ROLLOUT_GROUP_TARGETS_COUNT);
    }

    @Override
    protected LazyQueryContainer createContainer() {
        final BeanQueryFactory<RolloutGroupTargetsBeanQuery> rolloutgrouBeanQueryFactory = new BeanQueryFactory<>(
                RolloutGroupTargetsBeanQuery.class);
        return new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID),
                rolloutgrouBeanQueryFactory);
    }

    @Override
    protected void addContainerProperties() {
        final LazyQueryContainer rolloutGroupTargetGridContainer = (LazyQueryContainer) getContainerDataSource();
        rolloutGroupTargetGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", false,
                true);
        rolloutGroupTargetGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_STATUS, Status.class,
                Status.RETRIEVED, false, false);
        rolloutGroupTargetGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_BY, String.class, null,
                false, true);
        rolloutGroupTargetGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, String.class,
                null, false, true);
        rolloutGroupTargetGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, String.class, null,
                false, true);
        rolloutGroupTargetGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE, String.class,
                null, false, true);
        rolloutGroupTargetGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, "", false,
                true);
    }

    @Override
    protected void setColumnExpandRatio() {
        getColumn(SPUILabelDefinitions.VAR_NAME).setMinimumWidth(20);
        getColumn(SPUILabelDefinitions.VAR_NAME).setMaximumWidth(280);

        getColumn(SPUILabelDefinitions.VAR_STATUS).setMinimumWidth(50);
        getColumn(SPUILabelDefinitions.VAR_STATUS).setMaximumWidth(80);

        getColumn(SPUILabelDefinitions.VAR_CREATED_DATE).setMaximumWidth(180);
        getColumn(SPUILabelDefinitions.VAR_CREATED_DATE).setMinimumWidth(30);

        getColumn(SPUILabelDefinitions.VAR_CREATED_BY).setMaximumWidth(180);
        getColumn(SPUILabelDefinitions.VAR_CREATED_BY).setMinimumWidth(50);

        getColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE).setMaximumWidth(180);
        getColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE).setMinimumWidth(30);

        getColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY).setMaximumWidth(180);
        getColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY).setMinimumWidth(50);
    }

    @Override
    protected void setColumnHeaderNames() {
        getColumn(SPUILabelDefinitions.VAR_NAME).setHeaderCaption(i18n.getMessage("header.name"));
        getColumn(SPUILabelDefinitions.VAR_STATUS).setHeaderCaption(i18n.getMessage("header.status"));
        getColumn(SPUILabelDefinitions.VAR_CREATED_DATE).setHeaderCaption(i18n.getMessage("header.createdDate"));
        getColumn(SPUILabelDefinitions.VAR_CREATED_BY).setHeaderCaption(i18n.getMessage("header.createdBy"));
        getColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE).setHeaderCaption(i18n.getMessage("header.modifiedDate"));
        getColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY).setHeaderCaption(i18n.getMessage("header.modifiedBy"));
        getColumn(SPUILabelDefinitions.VAR_DESC).setHeaderCaption(i18n.getMessage("header.description"));
    }

    @Override
    protected String getGridId() {
        return UIComponentIdProvider.ROLLOUT_GROUP_TARGETS_LIST_GRID_ID;
    }

    @Override
    protected void setColumnProperties() {
        final Object[] columnsToShowInOrder = new Object[] { SPUILabelDefinitions.VAR_NAME,
                SPUILabelDefinitions.VAR_CREATED_DATE, SPUILabelDefinitions.VAR_CREATED_BY,
                SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE, SPUILabelDefinitions.VAR_LAST_MODIFIED_BY,
                SPUILabelDefinitions.VAR_STATUS, SPUILabelDefinitions.VAR_DESC };
        setColumns(columnsToShowInOrder);
        alignColumns();
    }

    @Override
    protected void addColumnRenderes() {
        getColumn(SPUILabelDefinitions.VAR_STATUS).setRenderer(new HtmlLabelRenderer(), new StatusConverter());
    }

    @Override
    protected void setHiddenColumns() {
        // No hidden columns
    }

    @Override
    protected CellDescriptionGenerator getDescriptionGenerator() {
        return this::getDescription;
    }

    private void alignColumns() {
        setCellStyleGenerator(new CellStyleGenerator() {
            private static final long serialVersionUID = 5573570647129792429L;

            @Override
            public String getStyle(final CellReference cellReference) {
                if (SPUILabelDefinitions.VAR_STATUS.equals(cellReference.getPropertyId())) {
                    return "centeralign";
                }
                return null;
            }
        });
    }

    /**
     *
     * Converts {@link Status} into string with status icon details.
     *
     */
    private class StatusConverter implements Converter<String, Status> {
        private static final long serialVersionUID = -7467206089699548808L;

        @Override
        public Status convertToModel(final String value, final Class<? extends Status> targetType,
                final Locale locale) {
            return null;
        }

        @Override
        public String convertToPresentation(final Status status, final Class<? extends String> targetType,
                final Locale locale) {
            if (status == null) {
                // Actions are not created for targets when rollout's status is
                // READY and when duplicate assignment is done. In these cases
                // display a appropriate status with description
                return getStatus();
            }
            return processActionStatus(status);
        }

        @Override
        public Class<Status> getModelType() {
            return Status.class;
        }

        @Override
        public Class<String> getPresentationType() {
            return String.class;
        }

        private String processActionStatus(final Status status) {
            final StatusFontIcon statusFontIcon = statusIconMap.get(status);
            final String codePoint = HawkbitCommonUtil.getCodePoint(statusFontIcon);
            return HawkbitCommonUtil.getStatusLabelDetailsInString(codePoint, statusFontIcon.getStyle(), null);
        }

        private String getStatus() {
            final RolloutGroup rolloutGroup = rolloutUIState.getRolloutGroup().orElse(null);
            if (rolloutGroup != null && rolloutGroup.getStatus() == RolloutGroupStatus.READY) {
                return HawkbitCommonUtil.getStatusLabelDetailsInString(
                        Integer.toString(FontAwesome.DOT_CIRCLE_O.getCodepoint()), "statusIconLightBlue", null);
            }
            if (rolloutGroup != null && rolloutGroup.getStatus() == RolloutGroupStatus.FINISHED) {
                return HawkbitCommonUtil.getStatusLabelDetailsInString(
                        Integer.toString(FontAwesome.MINUS_CIRCLE.getCodepoint()), "statusIconBlue", null);
            }
            return HawkbitCommonUtil.getStatusLabelDetailsInString(
                    Integer.toString(FontAwesome.QUESTION_CIRCLE.getCodepoint()), "statusIconBlue", null);
        }

    }

    private String getDescription(final CellReference cell) {
        if (!SPUILabelDefinitions.VAR_STATUS.equals(cell.getPropertyId())) {
            return null;
        }
        if (cell.getProperty().getValue() == null) {
            // status could be null when there is no action.
            return getDescriptionWhenNoAction();
        }
        return cell.getProperty().getValue().toString().toLowerCase();
    }

    private String getDescriptionWhenNoAction() {
        final RolloutGroup rolloutGroup = rolloutUIState.getRolloutGroup().orElse(null);
        if (rolloutGroup != null && rolloutGroup.getStatus() == RolloutGroupStatus.READY) {
            return RolloutGroupStatus.READY.toString().toLowerCase();
        } else if (rolloutGroup != null && rolloutGroup.getStatus() == RolloutGroupStatus.FINISHED) {
            final String ds = rolloutUIState.getRolloutDistributionSet().orElse("");
            return i18n.getMessage("message.dist.already.assigned", new Object[] { ds });
        }
        return "unknown";
    }

}
