/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.rollout.groupschart;

import java.util.List;

import org.eclipse.hawkbit.ui.rollout.groupschart.client.GroupsPieChartState;

import com.vaadin.ui.AbstractComponent;

/**
 * Draws a pie charts for the provided groups.
 */
public class GroupsPieChart extends AbstractComponent {

    private static final long serialVersionUID = 1311542227339430098L;

    /**
     * Updates the state of the chart
     * 
     * @param totalTargetsCount
     *            total count of targets that are represented by the pie
     * @param groupTargetCounts
     *            list of target counts
     */
    public void setChartState(final Long totalTargetsCount, final List<Long> groupTargetCounts) {
        getState().setGroupTargetCounts(groupTargetCounts);
        getState().setTotalTargetCount(totalTargetsCount);
        markAsDirty();
    }

    @Override
    protected GroupsPieChartState getState() {
        return (GroupsPieChartState) super.getState();
    }
}
