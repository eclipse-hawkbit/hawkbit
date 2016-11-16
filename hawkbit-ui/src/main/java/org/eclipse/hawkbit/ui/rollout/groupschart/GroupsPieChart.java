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

public class GroupsPieChart extends AbstractComponent {
    public GroupsPieChart() {
        registerRpc(new GroupsPieChartServerRpc() {
            private GroupsPieChartClientRpc getClientRpc() {
                return getRpcProxy(GroupsPieChartClientRpc.class);
            }
        });
    }

    public void setChartState(final List<Long> groupTargetCounts, final Long totalTargetsCount) {
        getState().groupTargetCounts = groupTargetCounts;
        getState().totalTargetCount = totalTargetsCount;
        markAsDirty();
    }

    @Override
    protected GroupsPieChartState getState() {
        return (GroupsPieChartState) super.getState();
    }
}
