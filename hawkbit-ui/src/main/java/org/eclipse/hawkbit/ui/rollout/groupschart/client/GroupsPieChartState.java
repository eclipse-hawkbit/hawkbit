/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.rollout.groupschart.client;

import java.util.List;

import com.vaadin.shared.AbstractComponentState;

/**
 * State to transfer for the groups pie chart between server and client.
 */
public class GroupsPieChartState extends AbstractComponentState {

    private static final long serialVersionUID = 7344220498082627571L;

    private transient List<Long> groupTargetCounts;

    private Long totalTargetCount;

    /**
     * @return Total count of group target
     */
    public List<Long> getGroupTargetCounts() {
        return groupTargetCounts;
    }

    /**
     * Sets the group target count
     *
     * @param groupTargetCounts
     *          list of target counts
     */
    public void setGroupTargetCounts(List<Long> groupTargetCounts) {
        this.groupTargetCounts = groupTargetCounts;
    }

    /**
     * @return total count of targets that are represented by the pie
     */
    public Long getTotalTargetCount() {
        return totalTargetCount;
    }

    /**
     * Sets the target count
     *
     * @param totalTargetCount
     *          Total targets
     */
    public void setTotalTargetCount(Long totalTargetCount) {
        this.totalTargetCount = totalTargetCount;
    }
}
