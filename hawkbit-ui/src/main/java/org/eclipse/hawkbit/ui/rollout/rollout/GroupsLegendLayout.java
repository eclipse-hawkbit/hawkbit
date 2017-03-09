/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rollout;

import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.builder.RolloutGroupCreate;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroupsValidation;
import org.eclipse.hawkbit.ui.common.builder.LabelBuilder;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Displays a legend for the Groups of a Rollout with the count of targets in
 * each group. On top of the group list, the total targets are displayed. If
 * there are unassigned targets, they get display on top of the groups list.
 */
public class GroupsLegendLayout extends VerticalLayout {

    private static final long serialVersionUID = 5483206203739308677L;

    private final VaadinMessageSource i18n;

    private Label totalTargetsLabel;

    private Label loadingLabel;

    private Label unassignedTargetsLabel;

    private VerticalLayout groupsLegend;

    /**
     * Initializes a new GroupsLegendLayout
     */
    GroupsLegendLayout(final VaadinMessageSource i18n) {
        this.i18n = i18n;

        init();
    }

    private void init() {

        totalTargetsLabel = createTotalTargetsLabel();
        unassignedTargetsLabel = createUnassignedTargetsLabel();
        loadingLabel = createLoadingLabel();
        loadingLabel.setVisible(false);

        groupsLegend = new VerticalLayout();
        groupsLegend.setStyleName("groups-legend");

        addComponent(totalTargetsLabel);
        addComponent(loadingLabel);
        addComponent(unassignedTargetsLabel);
        addComponent(groupsLegend);
        for (int i = 0; i < 8; i++) {
            groupsLegend.addComponent(createGroupTargetsLabel());
        }

    }

    /**
     * Resets the display of the legend and total targets.
     */
    public void reset() {
        totalTargetsLabel.setVisible(false);
        populateGroupsLegendByTargetCounts(Collections.emptyList());
    }

    private static Label createTotalTargetsLabel() {
        final Label label = new LabelBuilder().visible(false).name("").buildLabel();
        label.addStyleName("rollout-target-count-title");
        label.setImmediate(true);
        label.setSizeUndefined();
        return label;
    }

    private Label createLoadingLabel() {
        final Label label = new LabelBuilder().visible(false).name("").buildLabel();
        label.addStyleName("rollout-target-count-loading");
        label.setImmediate(true);
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

    private String getTotalTargetMessage(final long totalTargetsCount) {
        return i18n.getMessage("label.target.filter.count") + totalTargetsCount;
    }

    /**
     * Display an indication that the legend is being calculated. When the
     * loading process is done one of the populate methods should be called.
     */
    public void displayLoading() {
        populateGroupsLegendByTargetCounts(Collections.emptyList());
        loadingLabel.setVisible(true);
    }

    /**
     * Displays the total targets or hides the label when null is supplied.
     * 
     * @param totalTargets
     *            null to hide the label or a count to be displayed as total
     *            targets message
     */
    public void populateTotalTargets(Long totalTargets) {
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
     * @param targetsPerGroup
     *            list of target counts
     */
    public void populateGroupsLegendByTargetCounts(final List<Long> targetsPerGroup) {
        loadingLabel.setVisible(false);

        for (int i = 0; i < groupsLegend.getComponentCount(); i++) {
            final Component component = groupsLegend.getComponent(i);
            final Label label = (Label) component;
            if (targetsPerGroup.size() > i) {
                final Long targetCount = targetsPerGroup.get(i);
                label.setValue(
                        getTargetsInGroupMessage(targetCount, i18n.getMessage("textfield.rollout.group.default.name", i + 1)));
                label.setVisible(true);
            } else {
                label.setValue("");
                label.setVisible(false);
            }
        }

        unassignedTargetsLabel.setValue("");
        unassignedTargetsLabel.setVisible(false);

    }

    /**
     * Populates the legend based on a groups validation and a list of groups
     * that is used for resolving their names. Positions of the groups in the
     * groups list and the validation need to be in correct order. Can have
     * unassigned targets that are displayed on top of the groups list which
     * results in one group less to be displayed
     * 
     * @param validation
     *            A rollout validation object that contains a list of target
     *            counts and the total targets
     * @param groups
     *            List of groups with their name
     */
    public void populateGroupsLegendByValidation(final RolloutGroupsValidation validation,
            final List<RolloutGroupCreate> groups) {
        loadingLabel.setVisible(false);
        if (validation == null) {
            return;
        }
        List<Long> targetsPerGroup = validation.getTargetsPerGroup();
        final long unassigned = validation.getTotalTargets() - validation.getTargetsInGroups();
        final int labelsToUpdate = (unassigned > 0) ? (groupsLegend.getComponentCount() - 1)
                : groupsLegend.getComponentCount();
        for (int i = 0; i < groupsLegend.getComponentCount(); i++) {
            final Component component = groupsLegend.getComponent(i);
            final Label label = (Label) component;
            if (targetsPerGroup.size() > i && groups.size() > i && labelsToUpdate > i) {
                final Long targetCount = targetsPerGroup.get(i);
                final String groupName = groups.get(i).build().getName();

                label.setValue(getTargetsInGroupMessage(targetCount, groupName));
                label.setVisible(true);
            } else {
                label.setValue("");
                label.setVisible(false);
            }
        }

        if (unassigned > 0) {
            unassignedTargetsLabel.setValue(getTargetsInGroupMessage(unassigned, "Unassigned"));
            unassignedTargetsLabel.setVisible(true);
        } else {
            unassignedTargetsLabel.setValue("");
            unassignedTargetsLabel.setVisible(false);
        }

    }

    /**
     * Populates the legend based on a list of groups.
     *
     * @param groups
     *            List of groups with their name
     */
    public void populateGroupsLegendByGroups(final List<RolloutGroup> groups) {
        loadingLabel.setVisible(false);

        for (int i = 0; i < groupsLegend.getComponentCount(); i++) {
            final Component component = groupsLegend.getComponent(i);
            final Label label = (Label) component;
            if (groups.size() > i) {
                final int targetCount = groups.get(i).getTotalTargets();
                final String groupName = groups.get(i).getName();

                label.setValue(getTargetsInGroupMessage((long) targetCount, groupName));
                label.setVisible(true);
            } else {
                label.setValue("");
                label.setVisible(false);
            }
        }

    }

    private String getTargetsInGroupMessage(final Long targets, final String groupName) {
        return i18n.getMessage("label.rollout.targets.in.group", targets, groupName);
    }

}
