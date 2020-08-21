/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management;

import java.util.Collection;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.ui.common.data.filters.TargetManagementFilterParams;
import org.eclipse.hawkbit.ui.common.layout.AbstractFooterSupport;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;

/**
 * Count message label which display current filter details and details on
 * pinning.
 */
public class CountMessageLabel extends AbstractFooterSupport {
    private final VaadinMessageSource i18n;

    private final TargetManagement targetManagement;

    private final Label targetCountLabel;

    /**
     * Constructor
     * 
     * @param targetManagement
     *            TargetManagement
     * @param i18n
     *            I18N
     */
    public CountMessageLabel(final TargetManagement targetManagement, final VaadinMessageSource i18n) {
        this.targetManagement = targetManagement;
        this.i18n = i18n;
        this.targetCountLabel = new Label();

        init();
    }

    private void init() {
        targetCountLabel.setId(UIComponentIdProvider.COUNT_LABEL);
        targetCountLabel.addStyleName(SPUIStyleDefinitions.SP_LABEL_MESSAGE_STYLE);
        targetCountLabel.setContentMode(ContentMode.HTML);
        targetCountLabel.setIcon(null);
        targetCountLabel.setDescription(null);
    }

    /**
     * Display target count status on label
     *
     * @param count
     *          Total targets
     * @param targetFilterParams
     *          TargetManagementFilterParams
     */
    public void displayTargetCountStatus(final long count, final TargetManagementFilterParams targetFilterParams) {
        if (targetFilterParams == null) {
            return;
        }

        targetCountLabel.setCaption(getTargetCountStatusMessage(count, targetFilterParams));

        updatePinningDetails(targetFilterParams.getPinnedDistId());
    }

    private String getTargetCountStatusMessage(final long count,
            final TargetManagementFilterParams targetFilterParams) {
        if (!targetFilterParams.isAnyFilterSelected()) {
            return getTotalTargetMessage(count).toString();
        }

        final StringBuilder message = getTotalTargetMessage();
        message.append(HawkbitCommonUtil.SP_STRING_PIPE);
        message.append(i18n.getMessage("label.filter.targets"));
        message.append(count);
        message.append(HawkbitCommonUtil.SP_STRING_PIPE);
        final String status = i18n.getMessage("label.filter.status");
        final String overdue = i18n.getMessage("label.filter.overdue");
        final String tags = i18n.getMessage("label.filter.tags");
        final String text = i18n.getMessage("label.filter.text");
        final String dists = i18n.getMessage("label.filter.dist");
        final String custom = i18n.getMessage("label.filter.custom");
        final StringBuilder filterMesgBuf = new StringBuilder(i18n.getMessage("label.filter"));
        filterMesgBuf.append(" ");
        filterMesgBuf.append(getStatusMsg(targetFilterParams.getTargetUpdateStatusList(), status));
        filterMesgBuf.append(getOverdueStateMsg(targetFilterParams.isOverdueState(), overdue));
        filterMesgBuf.append(getTagsMsg(targetFilterParams.isNoTagClicked(), targetFilterParams.getTargetTags(), tags));
        filterMesgBuf.append(!StringUtils.isEmpty(targetFilterParams.getSearchText()) ? text : " ");
        filterMesgBuf.append(targetFilterParams.getDistributionId() != null ? dists : " ");
        filterMesgBuf.append(targetFilterParams.getTargetFilterQueryId() != null ? custom : " ");
        final String filterMesageChk = filterMesgBuf.toString().trim();
        String filterMesage = filterMesageChk;
        if (filterMesage.endsWith(",")) {
            filterMesage = filterMesageChk.substring(0, filterMesageChk.length() - 1);
        }
        message.append(filterMesage);

        return message.toString();
    }

    private StringBuilder getTotalTargetMessage(final long count) {
        final StringBuilder message = new StringBuilder(i18n.getMessage("label.target.filter.count"));
        message.append(": ");
        message.append(count);

        return message;
    }

    private StringBuilder getTotalTargetMessage() {
        return getTotalTargetMessage(targetManagement.count());
    }

    private static String getStatusMsg(final Collection<TargetUpdateStatus> status, final String param) {
        return status.isEmpty() ? " " : param;
    }

    private static String getOverdueStateMsg(final boolean overdueState, final String param) {
        return !overdueState ? " " : param;
    }

    private static String getTagsMsg(final Boolean noTargetTagSelected, final Collection<String> tags,
            final String param) {
        return CollectionUtils.isEmpty(tags) && Boolean.FALSE.equals(noTargetTagSelected) ? " " : param;
    }

    /**
     * Update pinning details
     *
     * @param pinnedDsId
     *          Pinned distribution set id
     */
    public void updatePinningDetails(final Long pinnedDsId) {
        if (pinnedDsId == null) {
            targetCountLabel.setValue("");
            return;
        }

        final Long targetsWithAssigedDsCount = targetManagement.countByAssignedDistributionSet(pinnedDsId);
        final Long targetsWithInstalledDsCount = targetManagement.countByInstalledDistributionSet(pinnedDsId);

        final StringBuilder message = new StringBuilder(i18n.getMessage("label.target.count"));
        message.append(" : ");
        message.append("<span class=\"assigned-count-message\">");
        message.append(i18n.getMessage("label.assigned.count", targetsWithAssigedDsCount));
        message.append("</span>, <span class=\"installed-count-message\"> ");
        message.append(i18n.getMessage("label.installed.count", targetsWithInstalledDsCount));
        message.append("</span>");

        targetCountLabel.setValue(message.toString());
    }

    @Override
    protected Label getFooterMessageLabel() {
        return targetCountLabel;
    }
}
