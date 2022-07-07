/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import java.util.Collection;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.common.data.filters.TargetManagementFilterParams;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.grid.support.FilterSupport;
import org.eclipse.hawkbit.ui.common.layout.AbstractFooterSupport;
import org.eclipse.hawkbit.ui.common.layout.CountAwareComponent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.vaadin.data.provider.Query;
import com.vaadin.shared.ui.ContentMode;

/**
 * Count message label which display current filter details and details on
 * pinning.
 */
public class TargetCountMessageLabel extends AbstractFooterSupport implements CountAwareComponent {
    private final TargetManagement targetManagement;
    private final FilterSupport<ProxyTarget, TargetManagementFilterParams> gridFilterSupport;

    private int totalCount;
    private int filteredCount;

    private long targetsWithAssignedDsCount;
    private long targetsWithInstalledDsCount;

    /**
     * Constructor
     * 
     * @param i18n
     *            I18N
     */
    public TargetCountMessageLabel(final VaadinMessageSource i18n, final UINotification notification,
            final TargetManagement targetManagement,
            final FilterSupport<ProxyTarget, TargetManagementFilterParams> gridFilterSupport) {
        super(i18n, notification);

        this.targetManagement = targetManagement;
        this.gridFilterSupport = gridFilterSupport;
    }

    @Override
    protected void init() {
        super.init();

        countLabel.setContentMode(ContentMode.HTML);
        countLabel.setIcon(null);
        countLabel.setDescription(null);
    }

    public void updateTotalAndFilteredCount() {
        updateCountAsynchronously(this::fetchTotalAndFilteredCount, this::updateCountLabel);
    }

    private void fetchTotalAndFilteredCount() {
        fetchTotalCount();
        fetchFilteredCount();
    }

    private void fetchTotalCount() {
        totalCount = gridFilterSupport.getOriginalDataProvider().size(new Query<>());
    }

    private void fetchFilteredCount() {
        if (gridFilterSupport.getFilter().isAnyFilterSelected()) {
            filteredCount = gridFilterSupport.getFilterDataProvider().size(new Query<>());
        } else {
            filteredCount = 0;
        }
    }

    public void updateFilteredCount() {
        updateCountAsynchronously(this::fetchFilteredCount, this::updateCountLabel);
    }

    private void updateCountLabel() {
        final StringBuilder countMessageBuilder = getTotalTargetsMessage();

        final TargetManagementFilterParams targetFilterParams = gridFilterSupport.getFilter();
        if (targetFilterParams.isAnyFilterSelected()) {
            appendFilteredTargetsMessage(countMessageBuilder, targetFilterParams);
        }

        countLabel.setCaption(countMessageBuilder.toString());
    }

    private StringBuilder getTotalTargetsMessage() {
        final StringBuilder message = new StringBuilder(i18n.getMessage("label.target.filter.count"));
        message.append(": ");
        message.append(totalCount);

        return message;
    }

    private void appendFilteredTargetsMessage(final StringBuilder countMessageBuilder,
            final TargetManagementFilterParams targetFilterParams) {
        countMessageBuilder.append(HawkbitCommonUtil.SP_STRING_PIPE);
        countMessageBuilder.append(i18n.getMessage("label.filter.targets"));
        countMessageBuilder.append(filteredCount);
        countMessageBuilder.append(HawkbitCommonUtil.SP_STRING_PIPE);
        countMessageBuilder.append(getFilterParametersMessage(targetFilterParams));
    }

    private String getFilterParametersMessage(final TargetManagementFilterParams targetFilterParams) {
        final StringBuilder filterMessageBuilder = new StringBuilder(i18n.getMessage("label.filter"));

        filterMessageBuilder.append(" ");
        appendStatusMsg(filterMessageBuilder, targetFilterParams.getTargetUpdateStatusList());
        appendOverdueStateMsg(filterMessageBuilder, targetFilterParams.isOverdueState());
        appendTagsMsg(filterMessageBuilder, targetFilterParams.isNoTagClicked(), targetFilterParams.getTargetTags());
        appendSearchMsg(filterMessageBuilder, targetFilterParams.getSearchText());
        appendDsMsg(filterMessageBuilder, targetFilterParams.getDistributionId());
        appendCustomFilterQueryMsg(filterMessageBuilder, targetFilterParams.getTargetFilterQueryId());
        appendTargetTypeFilterMsg(filterMessageBuilder, targetFilterParams.isNoTargetTypeClicked(),
                targetFilterParams.getTargetTypeId());

        String filterMessage = filterMessageBuilder.toString().trim();
        if (filterMessage.endsWith(",")) {
            filterMessage = filterMessage.substring(0, filterMessage.length() - 1);
        }

        return filterMessage;
    }

    private void appendStatusMsg(final StringBuilder filterMessageBuilder,
            final Collection<TargetUpdateStatus> status) {
        if (!status.isEmpty()) {
            appendFilterMsg(filterMessageBuilder, i18n.getMessage("label.filter.status"));
        }
    }

    private static void appendFilterMsg(final StringBuilder filterMessageBuilder, final String filter) {
        filterMessageBuilder.append(filter);
        filterMessageBuilder.append(", ");
    }

    private void appendOverdueStateMsg(final StringBuilder filterMessageBuilder, final boolean overdueState) {
        if (overdueState) {
            appendFilterMsg(filterMessageBuilder, i18n.getMessage("label.filter.overdue"));
        }
    }

    private void appendTagsMsg(final StringBuilder filterMessageBuilder, final boolean noTargetTagSelected,
            final Collection<String> tags) {
        if (noTargetTagSelected || !CollectionUtils.isEmpty(tags)) {
            appendFilterMsg(filterMessageBuilder, i18n.getMessage("label.filter.tags"));
        }
    }

    private void appendSearchMsg(final StringBuilder filterMessageBuilder, final String search) {
        if (!StringUtils.isEmpty(search)) {
            appendFilterMsg(filterMessageBuilder, i18n.getMessage("label.filter.text"));
        }
    }

    private void appendDsMsg(final StringBuilder filterMessageBuilder, final Long dsId) {
        if (dsId != null) {
            appendFilterMsg(filterMessageBuilder, i18n.getMessage("label.filter.dist"));
        }
    }

    private void appendCustomFilterQueryMsg(final StringBuilder filterMessageBuilder, final Long customFilterQueryId) {
        if (customFilterQueryId != null) {
            appendFilterMsg(filterMessageBuilder, i18n.getMessage("label.filter.custom"));
        }
    }

    private void appendTargetTypeFilterMsg(final StringBuilder filterMessageBuilder, final boolean noTargetTypeClicked,
            final Long targetTypeId) {
        if (targetTypeId != null || noTargetTypeClicked) {
            appendFilterMsg(filterMessageBuilder, i18n.getMessage("label.filter.target.type"));
        }
    }

    /**
     * Update pinning details
     *
     */
    public void updatePinningDetails() {
        final Long pinnedDsId = gridFilterSupport.getFilter().getPinnedDistId();
        if (pinnedDsId == null) {
            countLabel.setValue("");
            return;
        }

        updateCountDetailsAsynchronously(() -> fetchPinningCounts(pinnedDsId), this::updatePinningCountLabel);
    }

    private void fetchPinningCounts(final Long pinnedDsId) {
        targetsWithAssignedDsCount = targetManagement.countByAssignedDistributionSet(pinnedDsId);
        targetsWithInstalledDsCount = targetManagement.countByInstalledDistributionSet(pinnedDsId);
    }

    private void updatePinningCountLabel() {
        final StringBuilder message = new StringBuilder(i18n.getMessage("label.target.count"));
        message.append(" : ");
        message.append("<span class=\"assigned-count-message\">");
        message.append(i18n.getMessage("label.assigned.count", targetsWithAssignedDsCount));
        message.append("</span>, <span class=\"installed-count-message\"> ");
        message.append(i18n.getMessage("label.installed.count", targetsWithInstalledDsCount));
        message.append("</span>");

        countLabel.setValue(message.toString());
    }

    @Override
    public void updateCountOnEntitiesAdded(final int count) {
        updateTotalAndFilteredCount();
    }

    @Override
    public void updateCountOnEntitiesUpdated() {
        updateFilteredCount();
        updatePinningDetails();
    }

    @Override
    public void updateCountOnEntitiesDeleted(final int count) {
        updateTotalAndFilteredCount();
        updatePinningDetails();
    }
}
