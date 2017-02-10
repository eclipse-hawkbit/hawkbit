/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    public List<Long> getGroupTargetCounts() {
        return groupTargetCounts;
    }

    public void setGroupTargetCounts(List<Long> groupTargetCounts) {
        this.groupTargetCounts = groupTargetCounts;
    }

    public Long getTotalTargetCount() {
        return totalTargetCount;
    }

    public void setTotalTargetCount(Long totalTargetCount) {
        this.totalTargetCount = totalTargetCount;
    }
}
