/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.groupschart.client;

import org.eclipse.hawkbit.ui.rollout.groupschart.GroupsPieChart;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.communication.StateChangeEvent;
import com.vaadin.client.ui.AbstractComponentConnector;
import com.vaadin.shared.ui.Connect;

/**
 * Connector between client side GroupsPieChartWidget and service side.
 */
@Connect(GroupsPieChart.class)
public class GroupsPieChartConnector extends AbstractComponentConnector {

    private static final long serialVersionUID = -2907528194018611155L;

    @Override
    protected Widget createWidget() {
        return GWT.create(GroupsPieChartWidget.class);
    }

    @Override
    public GroupsPieChartWidget getWidget() {
        return (GroupsPieChartWidget) super.getWidget();
    }

    @Override
    public GroupsPieChartState getState() {
        return (GroupsPieChartState) super.getState();
    }

    @Override
    public void onStateChanged(StateChangeEvent stateChangeEvent) {
        super.onStateChanged(stateChangeEvent);
        getWidget().update(getState().getGroupTargetCounts(), getState().getTotalTargetCount());
    }
}
