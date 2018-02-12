/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.footer;

import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.event.PinUnpinEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent;
import org.eclipse.hawkbit.ui.management.event.TargetTableEvent.TargetComponentEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.management.state.TargetTableFilters;
import org.eclipse.hawkbit.ui.management.targettable.TargetTable;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Label;

/**
 * Count message label which display current filter details and details on
 * pinning.
 */
public class CountMessageLabel extends Label {
    private static final long serialVersionUID = -1533826352473259653L;

    private final transient TargetManagement targetManagement;

    private final VaadinMessageSource i18n;

    private final ManagementUIState managementUIState;

    private final TargetTable targetTable;

    /**
     * Constructor
     * 
     * @param eventBus
     *            UIEventBus
     * @param targetManagement
     *            TargetManagement
     * @param i18n
     *            I18N
     * @param managementUIState
     *            ManagementUIState
     * @param targetTable
     *            TargetTable
     */
    public CountMessageLabel(final UIEventBus eventBus, final TargetManagement targetManagement,
            final VaadinMessageSource i18n, final ManagementUIState managementUIState, final TargetTable targetTable) {
        this.targetManagement = targetManagement;
        this.i18n = i18n;
        this.managementUIState = managementUIState;
        this.targetTable = targetTable;

        applyStyle();
        eventBus.subscribe(this);
    }

    /**
     * TenantAwareEvent Listener to show the message count.
     *
     * @param event
     */
    @EventBusListenerMethod(scope = EventScope.UI)
    public void onEvent(final ManagementUIEvent event) {
        if (event == ManagementUIEvent.TARGET_TABLE_FILTER || event == ManagementUIEvent.SHOW_COUNT_MESSAGE) {
            displayTargetCountStatus();

        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final TargetTableEvent event) {
        if (TargetTableEvent.TargetComponentEvent.SELECT_ALL == event.getTargetComponentEvent()
                || TargetComponentEvent.REFRESH_TARGETS == event.getTargetComponentEvent()) {
            displayTargetCountStatus();
        }

    }

    /**
     * TenantAwareEvent Listener for Pinning Distribution.
     *
     * @param event
     */
    @EventBusListenerMethod(scope = EventScope.UI)
    public void onEvent(final PinUnpinEvent event) {
        final Optional<Long> pinnedDist = managementUIState.getTargetTableFilters().getPinnedDistId();

        if (event == PinUnpinEvent.PIN_DISTRIBUTION && pinnedDist.isPresent()) {
            displayCountLabel(pinnedDist.get());
        } else {
            setValue("");
            displayTargetCountStatus();
        }
    }

    /**
    *
    */
    private void applyStyle() {
        /* Create label for Targets count message displaying below the table */
        addStyleName(SPUILabelDefinitions.SP_LABEL_MESSAGE_STYLE);
        setContentMode(ContentMode.HTML);
        setId(UIComponentIdProvider.COUNT_LABEL);
    }

    private void displayTargetCountStatus() {
        final TargetTableFilters targFilParams = managementUIState.getTargetTableFilters();
        final StringBuilder message = getTotalTargetMessage();

        if (targFilParams.hasFilter()) {
            message.append(HawkbitCommonUtil.SP_STRING_PIPE);
            message.append(i18n.getMessage("label.filter.targets"));
            if (managementUIState.getTargetsTruncated() != null) {
                message.append(targetTable.size() + managementUIState.getTargetsTruncated());
            } else {
                message.append(targetTable.size());
            }
            message.append(HawkbitCommonUtil.SP_STRING_PIPE);
            final String status = i18n.getMessage("label.filter.status");
            final String overdue = i18n.getMessage("label.filter.overdue");
            final String tags = i18n.getMessage("label.filter.tags");
            final String text = i18n.getMessage("label.filter.text");
            final String dists = i18n.getMessage("label.filter.dist");
            final String custom = i18n.getMessage("label.filter.custom");
            final StringBuilder filterMesgBuf = new StringBuilder(i18n.getMessage("label.filter"));
            filterMesgBuf.append(" ");
            filterMesgBuf.append(getStatusMsg(targFilParams.getClickedStatusTargetTags(), status));
            filterMesgBuf.append(getOverdueStateMsg(targFilParams.isOverdueFilterEnabled(), overdue));
            filterMesgBuf
                    .append(getTagsMsg(targFilParams.isNoTagSelected(), targFilParams.getClickedTargetTags(), tags));
            filterMesgBuf.append(targFilParams.getSearchText().map(search -> text).orElse(" "));
            filterMesgBuf.append(targFilParams.getDistributionSet().map(set -> dists).orElse(" "));
            filterMesgBuf.append(targFilParams.getTargetFilterQuery().map(query -> custom).orElse(" "));
            final String filterMesageChk = filterMesgBuf.toString().trim();
            String filterMesage = filterMesageChk;
            if (filterMesage.endsWith(",")) {
                filterMesage = filterMesageChk.substring(0, filterMesageChk.length() - 1);
            }
            message.append(filterMesage);
        }

        if ((targetTable.size() + Optional.ofNullable(managementUIState.getTargetsTruncated())
                .orElse(0L)) > SPUIDefinitions.MAX_TABLE_ENTRIES) {
            message.append(HawkbitCommonUtil.SP_STRING_PIPE);
            message.append(i18n.getMessage("label.filter.shown"));
            message.append(SPUIDefinitions.MAX_TABLE_ENTRIES);
        }

        setCaption(message.toString());
    }

    private StringBuilder getTotalTargetMessage() {
        if (managementUIState.getTargetsTruncated() != null) {
            // set the icon
            setIcon(FontAwesome.INFO_CIRCLE);
            setDescription(i18n.getMessage("label.target.filter.truncated", managementUIState.getTargetsTruncated(),
                    SPUIDefinitions.MAX_TABLE_ENTRIES));
        } else {
            setIcon(null);
            setDescription(null);
        }

        final StringBuilder message = new StringBuilder(i18n.getMessage("label.target.filter.count"));
        message.append(managementUIState.getTargetsCountAll());

        return message;
    }

    /**
     * Display message.
     *
     * @param distId
     *            as dist ID
     */
    private void displayCountLabel(final Long distId) {
        final Long targetsWithAssigedDsCount = targetManagement.countByAssignedDistributionSet(distId);
        final Long targetsWithInstalledDsCount = targetManagement.countByInstalledDistributionSet(distId);
        final StringBuilder message = new StringBuilder(i18n.getMessage("label.target.count"));
        message.append("<span class=\"assigned-count-message\">");
        message.append(i18n.getMessage("label.assigned.count", new Object[] { targetsWithAssigedDsCount }));
        message.append("</span>, <span class=\"installed-count-message\"> ");
        message.append(i18n.getMessage("label.installed.count", new Object[] { targetsWithInstalledDsCount }));
        message.append("</span>");
        setValue(message.toString());
    }

    /**
     * Get Status Message.
     *
     * @param status
     *            as status
     * @return String as msg.
     */
    private static String getStatusMsg(final List<TargetUpdateStatus> status, final String param) {
        return status.isEmpty() ? " " : param;
    }

    /**
     * Get Overdue State Message.
     *
     * @param overdueState
     *            as flag
     * @return String as msg.
     */
    private static String getOverdueStateMsg(final boolean overdueState, final String param) {
        return !overdueState ? " " : param;
    }

    /**
     * Get Tags Message.
     *
     * @param noTargetTagSelected
     * @param tags
     *            as tags
     * @return String as msg.
     */
    private static String getTagsMsg(final Boolean noTargetTagSelected, final List<String> tags, final String param) {
        return tags.isEmpty() && (noTargetTagSelected == null || !noTargetTagSelected.booleanValue())
                ? " " : param;
    }
}
