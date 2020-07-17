/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.window.components;

import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.CollectionUtils;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Displays a legend for the Groups of a Rollout with the count of targets in
 * each group. On top of the group list, the total targets are displayed. If
 * there are unassigned targets, they get display on top of the groups list.
 */
@SuppressWarnings({ "squid:MaximumInheritanceDepth", "squid:S2160" })
public class GroupsLegendLayout extends VerticalLayout {
    private static final long serialVersionUID = 1L;

    private static final int MAX_GROUPS_TO_BE_DISPLAYED = 7;

    private final VaadinMessageSource i18n;

    private Label totalTargetsLabel;
    private Label loadingLabel;
    private Label unassignedTargetsLabel;

    private VerticalLayout groupsLegend;

    /**
     * Initializes a new GroupsLegendLayout
     *
     * @param i18n
     *          VaadinMessageSource
     */
    public GroupsLegendLayout(final VaadinMessageSource i18n) {
        this.i18n = i18n;

        init();
    }

    private void init() {

        totalTargetsLabel = createTotalTargetsLabel();
        unassignedTargetsLabel = createUnassignedTargetsLabel();
        loadingLabel = createLoadingLabel();
        loadingLabel.setVisible(false);

        groupsLegend = new VerticalLayout();
        groupsLegend.setMargin(false);
        groupsLegend.setSpacing(false);
        groupsLegend.setStyleName("groups-legend");

        addComponent(totalTargetsLabel);
        addComponent(loadingLabel);
        addComponent(unassignedTargetsLabel);
        addComponent(groupsLegend);

        for (int i = 0; i < MAX_GROUPS_TO_BE_DISPLAYED; i++) {
            groupsLegend.addComponent(createGroupTargetsLabel());
        }

        groupsLegend.addComponent(createToBeContinuedLabel());
    }

    /**
     * Resets the display of the legend and total targets.
     */
    public void reset() {
        totalTargetsLabel.setVisible(false);
        populateGroupsLegend(Collections.emptyList());
        if (groupsLegend.getComponentCount() > MAX_GROUPS_TO_BE_DISPLAYED) {
            groupsLegend.getComponent(MAX_GROUPS_TO_BE_DISPLAYED).setVisible(false);
        }
    }

    private static Label createTotalTargetsLabel() {
        final Label label = new LabelBuilder().visible(false).name("").buildLabel();
        label.addStyleName("rollout-target-count-title");
        label.setSizeUndefined();
        return label;
    }

    private Label createLoadingLabel() {
        final Label label = new LabelBuilder().visible(false).name("").buildLabel();
        label.addStyleName("rollout-target-count-loading");
        label.setSizeUndefined();
        label.setValue(i18n.getMessage("label.rollout.calculating"));
        return label;
    }

    private static Label createUnassignedTargetsLabel() {
        final Label label = new LabelBuilder().visible(false).name("").buildLabel();
        label.addStyleName("rollout-group-unassigned");
        label.setSizeUndefined();
        return label;
    }

    private static Label createGroupTargetsLabel() {
        final Label label = new LabelBuilder().visible(false).name("").buildLabel();
        label.addStyleName("rollout-group-count");
        label.setSizeUndefined();
        return label;
    }

    private static Label createToBeContinuedLabel() {
        return new LabelBuilder().caption("...").visible(false).buildLabel();
    }

    private String getTotalTargetMessage(final long totalTargetsCount) {
        return i18n.getMessage("label.target.filter.count") + ": " + totalTargetsCount;
    }

    /**
     * Display an indication that the legend is being calculated. When the
     * loading process is done one of the populate methods should be called.
     */
    public void displayLoading() {
        populateGroupsLegend(Collections.emptyList());
        loadingLabel.setVisible(true);
    }

    /**
     * Displays the total targets or hides the label when null is supplied.
     * 
     * @param totalTargets
     *            null to hide the label or a count to be displayed as total
     *            targets message
     */
    public void setTotalTargets(final Long totalTargets) {
        if (totalTargets == null) {
            totalTargetsLabel.setVisible(false);
        } else {
            totalTargetsLabel.setVisible(true);
            totalTargetsLabel.setValue(getTotalTargetMessage(totalTargets));
        }
    }

    /**
     * Populates the legend based on a list of anonymous groups. They can't have
     * unassigned targets.
     * 
     * @param listOfTargetCountPerGroup
     *            list of target counts
     */
    public void populateGroupsLegend(final List<Long> listOfTargetCountPerGroup) {
        loadingLabel.setVisible(false);

        for (int i = 0; i < getGroupsWithoutToBeContinuedLabel(listOfTargetCountPerGroup.size()); i++) {
            final Component component = groupsLegend.getComponent(i);
            final Label label = (Label) component;
            if (listOfTargetCountPerGroup.size() > i) {
                final Long targetCount = listOfTargetCountPerGroup.get(i);
                label.setValue(getTargetsInGroupMessage(targetCount,
                        i18n.getMessage("textfield.rollout.group.default.name", i + 1)));
                label.setVisible(true);
            } else {
                label.setValue("");
                label.setVisible(false);
            }
        }

        showOrHideToBeContinueLabel(listOfTargetCountPerGroup);

        unassignedTargetsLabel.setValue("");
        unassignedTargetsLabel.setVisible(false);
    }

    private void showOrHideToBeContinueLabel(final List<?> listOfTargetCountPerGroup) {
        if (hasMoreGroupsToShowAsLimit(listOfTargetCountPerGroup)) {
            groupsLegend.getComponent(MAX_GROUPS_TO_BE_DISPLAYED).setVisible(true);
        } else if (hasLessGroupsToShowAsLimit(listOfTargetCountPerGroup)) {
            groupsLegend.getComponent(groupsLegend.getComponentCount() - 1).setVisible(false);
        }
    }

    private boolean hasLessGroupsToShowAsLimit(final List<?> listOfTargetCountPerGroup) {
        return groupsLegend.getComponentCount() > listOfTargetCountPerGroup.size();
    }

    private static boolean hasMoreGroupsToShowAsLimit(final List<?> listOfTargetCountPerGroup) {
        return listOfTargetCountPerGroup.size() > MAX_GROUPS_TO_BE_DISPLAYED;
    }

    private int getGroupsWithoutToBeContinuedLabel(final int amountOfRolloutGroups) {
        if (amountOfRolloutGroups < groupsLegend.getComponentCount()) {
            return groupsLegend.getComponentCount();
        }

        return groupsLegend.getComponentCount() - 1;
    }

    /**
     * Populates the legend based on total targets, a list of targets per group
     * and a list of their names. Positions of the groups in the groups targets
     * list and the names list need to be in correct order. Can have unassigned
     * targets that are displayed on top of the groups list which results in one
     * group less to be displayed.
     * 
     * @param totalTargets
     *            Total targets
     * @param targetsPerGroup
     *            List of targets per group
     * @param groupNames
     *            List of group names
     */
    public void populateGroupsLegend(final Long totalTargets, final List<Long> targetsPerGroup,
            final List<String> groupNames) {
        loadingLabel.setVisible(false);

        if (!HawkbitCommonUtil.atLeastOnePresent(totalTargets) || CollectionUtils.isEmpty(targetsPerGroup)
                || CollectionUtils.isEmpty(groupNames) || targetsPerGroup.size() != groupNames.size()) {
            return;
        }

        final long remainingTargets = totalTargets - HawkbitCommonUtil.getSumOf(targetsPerGroup);
        final int labelsToUpdate = (remainingTargets > 0)
                ? (getGroupsWithoutToBeContinuedLabel(targetsPerGroup.size()) - 1)
                : groupsLegend.getComponentCount();

        for (int i = 0; i < getGroupsWithoutToBeContinuedLabel(targetsPerGroup.size()); i++) {
            final Component component = groupsLegend.getComponent(i);
            final Label label = (Label) component;
            if (targetsPerGroup.size() > i && labelsToUpdate > i) {
                final Long targetCount = targetsPerGroup.get(i);
                final String groupName = groupNames.get(i);

                label.setValue(getTargetsInGroupMessage(targetCount, groupName));
                label.setVisible(true);
            } else {
                label.setValue("");
                label.setVisible(false);
            }
        }

        showOrHideToBeContinueLabel(targetsPerGroup);

        if (remainingTargets > 0) {
            unassignedTargetsLabel.setValue(
                    getTargetsInGroupMessage(remainingTargets, i18n.getMessage("message.rollout.target.unassigned")));
            unassignedTargetsLabel.setVisible(true);
        } else {
            unassignedTargetsLabel.setValue("");
            unassignedTargetsLabel.setVisible(false);
        }

    }

    private String getTargetsInGroupMessage(final Long targets, final String groupName) {
        return i18n.getMessage("label.rollout.targets.in.group", targets, groupName);
    }

}
