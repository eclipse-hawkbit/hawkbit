/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout;

import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus.Status;
import org.vaadin.alump.distributionbar.gwt.client.GwtDistributionBar;

/**
 * Distribution bar helper to render distribution bar in grid.
 *
 */
public final class DistributionBarHelper {
    private static final String HTML_DIV_END = "</div>";
    private static final int PARENT_SIZE_IN_PCT = 100;
    private static final double MINIMUM_PART_SIZE = 10;
    private static final String DISTRIBUTION_BAR_PART_MAIN_STYLE = GwtDistributionBar.CLASSNAME + "-part";
    private static final String DISTRIBUTION_BAR_PART_CLASSNAME_PREFIX = GwtDistributionBar.CLASSNAME + "-part-";
    private static final String DISTRIBUTION_BAR_PART_VALUE_CLASSNAME = GwtDistributionBar.CLASSNAME + "-value";
    private static final String UNINITIALIZED_VALUE_CLASSNAME = GwtDistributionBar.CLASSNAME + "-uninitizalized";

    private DistributionBarHelper() {
    }

    /**
     * Returns a string with details of status and count .
     *
     * @param statusTotalCountMap
     *            map with status and count
     *
     * @return string of format "status1:count,status2:count"
     */
    public static String getDistributionBarAsHTMLString(final Map<Status, Long> statusTotalCountMap) {
        final StringBuilder htmlString = new StringBuilder();
        final Map<Status, Long> statusMapWithNonZeroValues = getStatusMapWithNonZeroValues(statusTotalCountMap);
        final Long totalValue = getTotalSizes(statusTotalCountMap);
        if (statusMapWithNonZeroValues.size() <= 0) {
            return getUnintialisedBar();
        }
        int partIndex = 1;
        htmlString.append(getParentDivStart());
        for (final Map.Entry<Status, Long> entry : statusMapWithNonZeroValues.entrySet()) {
            if (entry.getValue() > 0) {
                htmlString.append(getPart(partIndex, entry.getKey(), entry.getValue(), totalValue,
                        statusMapWithNonZeroValues.size()));
                partIndex++;
            }
        }
        htmlString.append(HTML_DIV_END);
        return htmlString.toString();
    }

    /**
     * Returns the map with status having non zero values.
     *
     * @param statusTotalCountMap
     *            map with status and count
     * @return map with non zero values
     */
    public static Map<Status, Long> getStatusMapWithNonZeroValues(final Map<Status, Long> statusTotalCountMap) {
        return statusTotalCountMap.entrySet().stream().filter(p -> p.getValue() > 0)
                .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));
    }

    /**
     * Returns tool tip for progress bar.
     *
     * @param statusCountMap
     *            map with status and count details
     * @return tool tip
     */
    public static String getTooltip(final Map<Status, Long> statusCountMap) {
        final Map<Status, Long> nonZeroStatusCountMap = DistributionBarHelper
                .getStatusMapWithNonZeroValues(statusCountMap);
        final StringBuilder tooltip = new StringBuilder();
        for (final Entry<Status, Long> entry : nonZeroStatusCountMap.entrySet()) {
            tooltip.append(entry.getKey().toString().toLowerCase()).append(" : ").append(entry.getValue())
                    .append("<br>");
        }
        return tooltip.toString();
    }

    private static String getPartStyle(final int partIndex, final int noOfParts, final String customStyle) {
        final StringBuilder mainStyle = new StringBuilder();
        final StringBuilder styleName = new StringBuilder(GwtDistributionBar.CLASSNAME);
        if (noOfParts == 1) {
            styleName.append("-only");
        } else if (partIndex == 1) {
            styleName.append("-left");
        } else if (partIndex == noOfParts) {
            styleName.append("-right");
        } else {
            styleName.append("-middle");
        }
        mainStyle.append(styleName).append(" ");
        mainStyle.append(DISTRIBUTION_BAR_PART_MAIN_STYLE).append(" ");
        mainStyle.append(DISTRIBUTION_BAR_PART_CLASSNAME_PREFIX + partIndex);
        if (customStyle != null) {
            mainStyle.append(" ").append("status-bar-part-" + customStyle);
        }
        return mainStyle.toString();
    }

    private static String getPartWidth(final Long value, final Long totalValue, final int noOfParts) {
        final double minTotalSize = MINIMUM_PART_SIZE * noOfParts;
        final double availableSize = PARENT_SIZE_IN_PCT - minTotalSize;
        final double val = MINIMUM_PART_SIZE + (double) value / totalValue * availableSize;
        // necessary due the format must contain a dot and other locals might
        // use a comma
        return String.format(Locale.ENGLISH, "%.3f", val) + "%";
    }

    private static String getPart(final int partIndex, final Status status, final Long value, final Long totalValue,
            final int noOfParts) {
        final String partValue = status.toString().toLowerCase();
        return "<div class=\"" + getPartStyle(partIndex, noOfParts, partValue) + "\" style=\"width: "
                + getPartWidth(value, totalValue, noOfParts) + ";\"><span class=\""
                + DISTRIBUTION_BAR_PART_VALUE_CLASSNAME + "\">" + value + "</span></div>";
    }

    private static String getUnintialisedBar() {
        return "<div class=\"" + UNINITIALIZED_VALUE_CLASSNAME + "\" style=\"width: 100%;\"><span class=\""
                + DISTRIBUTION_BAR_PART_VALUE_CLASSNAME + "\">uninitialized</span></div>";
    }

    private static Long getTotalSizes(final Map<Status, Long> statusTotalCountMap) {
        Long total = 0L;
        for (final Long value : statusTotalCountMap.values()) {
            total = total + value;
        }
        return total;
    }

    private static String getParentDivStart() {
        return "<div class=\"" + GwtDistributionBar.CLASSNAME
                + "\" style=\"width: 100%; height: 100%;\" id=\"rollout.status.progress.bar.id\">";
    }
}
