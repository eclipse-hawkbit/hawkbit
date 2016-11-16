/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.groupschart.client;

import com.github.gwtd3.api.D3;
import com.github.gwtd3.api.arrays.Array;
import com.github.gwtd3.api.core.Selection;
import com.github.gwtd3.api.core.UpdateSelection;
import com.github.gwtd3.api.core.Value;
import com.github.gwtd3.api.functions.DatumFunction;
import com.github.gwtd3.api.svg.Arc;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.DockLayoutPanel;

import java.util.List;

/**
 * Draws a pie chart using D3. The slices are based on the list of Longs and on
 * the total target count. The total target count represents 100% of the pie. If
 * the sum in the list of Longs is less than the total target count a slice for
 * unassigned targets will be displayed.
 *
 */
public class GroupsPieChartWidget extends DockLayoutPanel {

    private List<Long> groupTargetCounts;
    private Long totalTargetCount;
    private long unassignedTargets = 0;

    private Selection svg;
    private Selection pieGroup;
    private Selection infoText;

    private Arc arc;

    public GroupsPieChartWidget() {
        super(Style.Unit.PX);
        init();
    }

    private void init() {

        initChart();

        draw();

    }

    public void update(final List<Long> groupTargetCounts, final Long totalTargetCount) {
        this.groupTargetCounts = groupTargetCounts;
        this.totalTargetCount = totalTargetCount;

        if (groupTargetCounts != null) {
            long sum = 0;
            for (Long targetCount : groupTargetCounts) {
                sum += targetCount;
            }
            unassignedTargets = totalTargetCount - sum;
        }

        draw();

    }

    private PieArc getPie(Long count, Long total, double startAngle) {
        final Double percentage = count.doubleValue() / total.doubleValue();
        return new PieArc(startAngle, startAngle + percentage * 2 * Math.PI);
    }

    private void draw() {
        if (svg == null || groupTargetCounts == null || totalTargetCount == null) {
            return;
        }

        final Array<Arc> dataArray = Array.create();

        PieArc pie = getPie(unassignedTargets, totalTargetCount, 0);
        dataArray.push(pie.getArc());

        double lastAngle = pie.getEndAngle();
        for (int i = 0; i < groupTargetCounts.size(); i++) {
            final PieArc arcEntry = getPie(groupTargetCounts.get(i), totalTargetCount, lastAngle);
            dataArray.push(arcEntry.getArc());
            lastAngle = arcEntry.getEndAngle();
        }

        UpdateSelection pies = pieGroup.selectAll(".pie").data(dataArray);
        pies.enter().append("path").classed("pie", true).on(BrowserEvents.MOUSEOVER, new DatumFunction<Void>() {
            @Override
            public Void apply(Element context, Value d, int index) {
                Array<Double> point = arc.centroid(d.as(Arc.class), index);
                if (index == 0) {
                    infoText.html("Unassigned: " + unassignedTargets);
                } else {
                    infoText.html(index + ".: " + groupTargetCounts.get(index - 1));
                }

                infoText.attr("visibility", "visible");
                infoText.attr("x", point.getNumber(0));
                infoText.attr("y", point.getNumber(1));
                return null;
            }
        }).on(BrowserEvents.MOUSEOUT, new DatumFunction<Void>() {
            @Override
            public Void apply(Element context, Value d, int index) {
                infoText.attr("visibility", "hidden");
                return null;
            }
        });
        pies.exit().remove();
        pies.attr("d", arc);

    }

    private void initChart() {
        arc = D3.svg().arc().innerRadius(0).outerRadius(90);
        int height = 200;
        int width = 200;

        svg = D3.select(this).append("svg").attr("width", width).attr("height", height).append("g").attr("transform",
                "translate(" + ((float) width / 2) + "," + ((float) height / 2) + ")");

        pieGroup = svg.append("g");
        infoText = svg.append("text").attr("visibility", "hidden").classed("pie-info", true).attr("text-anchor",
                "middle");

    }

    private class PieArc {
        private double startAngle;

        private double endAngle;

        public PieArc(double startAngle, double endAngle) {
            this.startAngle = startAngle;
            this.endAngle = endAngle;
        }

        public double getStartAngle() {
            return startAngle;
        }

        public double getEndAngle() {
            return endAngle;
        }

        public Arc getArc() {
            return Arc.constantArc().startAngle(startAngle).endAngle(endAngle);
        }
    }
}
