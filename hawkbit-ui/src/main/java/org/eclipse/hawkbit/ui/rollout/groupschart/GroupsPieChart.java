/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.groupschart;

import org.eclipse.hawkbit.ui.rollout.groupschart.client.GroupsPieChartClientRpc;
import org.eclipse.hawkbit.ui.rollout.groupschart.client.GroupsPieChartServerRpc;
import org.eclipse.hawkbit.ui.rollout.groupschart.client.GroupsPieChartState;
import com.vaadin.ui.AbstractComponent;

import java.util.List;

/**
 * Draws a pie charts for the provided groups.
 */
public class GroupsPieChart extends AbstractComponent {

    private static final long serialVersionUID = 1311542227339430098L;

    /**
     * Instantiates a new pie chart and registers the RPC channel
     */
    public GroupsPieChart() {
        registerRpc(new GroupsPieChartServerRpc() {
            @SuppressWarnings("squid:UnusedPrivateMethod")
            private GroupsPieChartClientRpc getClientRpc() {
                return getRpcProxy(GroupsPieChartClientRpc.class);
            }
        });
    }

    /**
     * Updates the state of the chart
     * 
     * @param groupTargetCounts
     *            list of target counts
     * @param totalTargetsCount
     *            total count of targets that are represented by the pie
     */
    public void setChartState(final List<Long> groupTargetCounts, final Long totalTargetsCount) {
        getState().setGroupTargetCounts(groupTargetCounts);
        getState().setTotalTargetCount(totalTargetsCount);
        markAsDirty();
    }

    @Override
    protected GroupsPieChartState getState() {
        return (GroupsPieChartState) super.getState();
    }
}
