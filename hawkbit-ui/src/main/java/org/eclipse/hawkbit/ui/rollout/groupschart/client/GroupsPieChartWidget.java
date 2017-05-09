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

/**
 * Draws a pie chart using D3. The slices are based on the list of Longs and on
 * the total target count. The total target count represents 100% of the pie. If
 * the sum in the list of Longs is less than the total target count a slice for
 * unassigned targets will be displayed.
 *
 */
// Exception squid - non Java 8 compatible GWT code that runs on browser
@SuppressWarnings({ "squid:TrailingCommentCheck", "squid:S1604" })
public class GroupsPieChartWidget extends DockLayoutPanel {

    private static final String ATTR_VISIBILITY = "visibility";
    private static final String ATTR_TRANSFORM = "transform";

    private List<Long> groupTargetCounts;
    private Long totalTargetCount;
    private long unassignedTargets;

    private Selection svg;
    private Selection pieGroup;
    private Selection infoText;

    private Arc arc;

    /**
     * Initializes the pie chart
     */
    public GroupsPieChartWidget() {
        super(Style.Unit.PX);
        init();
    }

    private void init() {

        initChart();

        draw();

    }

    /**
     * Updates the pie chart with new data
     * 
     * @param groupTargetCounts
     *            list of target counts
     * @param totalTargetCount
     *            total count of targets that are represented by the pie
     */
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

    private static PieArc getPie(Long count, Long total, double startAngle) {
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
                double x = point.getNumber(0);
                double y = point.getNumber(1);
                if (index == 0) {
                    updateHoverText("Unassigned: " + unassignedTargets, x, y);
                } else {
                    updateHoverText(index + ": " + groupTargetCounts.get(index - 1), x, y);
                }

                return null;
            }
        }).on(BrowserEvents.MOUSEOUT, new DatumFunction<Void>() {
            @Override
            public Void apply(Element context, Value d, int index) {
                infoText.attr(ATTR_VISIBILITY, "hidden");
                return null;
            }
        });
        pies.exit().remove();
        pies.attr("d", arc);

    }

    private void updateHoverText(final String displayText, final double x, final double y) {
        final Selection text = infoText.select("text");
        final Selection background = infoText.select("rect");

        text.html(displayText);

        final double textWidth = getTextWidth(text.node());
        final double textHeight = getTextHeight(text.node());

        background.attr("width", textWidth * 1.1);
        background.attr("height", textHeight);

        moveSelection(background, -textWidth * 1.1 / 2.0, -textHeight*0.8);
        moveSelection(infoText, x, y);
        infoText.attr(ATTR_VISIBILITY, "visible");
    }

    private static void moveSelection(Selection sel, double x, double y) {
        sel.attr(ATTR_TRANSFORM, "translate(" + x + ", " + y + ")");
    }


    private static final native double getTextWidth(Element e)/*-{
        return e.getBBox().width;
    }-*/;

    private static final native double getTextHeight(Element e)/*-{
        return e.getBBox().height;
    }-*/;

    private void initChart() {
        arc = D3.svg().arc().innerRadius(0).outerRadius(90);
        int height = 200;
        int width = 260;

        svg = D3.select(this).append("svg").attr("width", width).attr("height", height).append("g");
        moveSelection(svg, (float) width / 2, (float) height / 2);

        pieGroup = svg.append("g");

        infoText = svg.append("g").attr(ATTR_VISIBILITY, "hidden").classed("pie-info", true);
        infoText.append("rect");
        infoText.append("text").attr("text-anchor", "middle");

    }

    private static class PieArc {
        private double startAngle;

        private double endAngle;

        public PieArc(double startAngle, double endAngle) {
            this.startAngle = startAngle;
            this.endAngle = endAngle;
        }

        public double getEndAngle() {
            return endAngle;
        }

        public Arc getArc() {
            return Arc.constantArc().startAngle(startAngle).endAngle(endAngle);
        }
    }
}
