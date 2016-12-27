/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TimeZone;

import org.eclipse.hawkbit.repository.model.AssignmentResult;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.PollStatus;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.ui.rollout.StatusFontIcon;
import org.vaadin.addons.lazyquerycontainer.AbstractBeanQuery;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.alump.distributionbar.DistributionBar;

import com.google.common.base.Strings;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;

/**
 * Common util class.
 */
public final class HawkbitCommonUtil {
    public static final String SP_STRING_PIPE = " | ";
    /**
     * Define spaced string.
     */
    public static final String SP_STRING_SPACE = " ";
    /**
     * Define empty string.
     */
    public static final String SP_STRING_EMPTY = "";
    /**
     * Html span.
     */
    public static final String SPAN_CLOSE = "</span>";
    public static final String HTML_LI_CLOSE_TAG = "</li>";
    public static final String HTML_LI_OPEN_TAG = "<li>";
    public static final String HTML_UL_CLOSE_TAG = "</ul>";
    public static final String HTML_UL_OPEN_TAG = "<ul>";

    public static final String DIV_DESCRIPTION_START = "<div id=\"desc-length\"><p id=\"desciption-p\">";
    public static final String DIV_DESCRIPTION_END = "</p></div>";

    private static final String SM_HIGHLIGHT_CREATE_SCRIPT = "smHighlight = document.createElement('style'); smHighlight.id=\"sm-table-highlight\";  document.head.appendChild(smHighlight); ";
    private static final String SM_HIGHLIGHT_REMOVE_SCRIPT = "var y = document.getElementById('sm-table-highlight'); if(y) { document.head.removeChild(y); } ";
    private static final String SM_HIGHLIGHT_RESET_SCRIPT = SM_HIGHLIGHT_REMOVE_SCRIPT + SM_HIGHLIGHT_CREATE_SCRIPT
            + "document.getElementById('sm-table-highlight').innerHTML =\"\"; ";
    private static final String SM_HIGHLIGHT_SCRIPT_CURRENT = "var smHighlightStyle = document.getElementById('sm-table-highlight').innerHTML; ";
    private static final String SM_HIGHLIGHT_SCRIPT_APPEND = "document.getElementById('sm-table-highlight').innerHTML = smHighlightStyle;";
    private static final String NEW_PREVIEW_COLOR_CREATE_SCRIPT = "newColorPreview = document.createElement('style'); newColorPreview.id=\"new-color-preview\";  document.head.appendChild(newColorPreview); ";
    private static final String NEW_PREVIEW_COLOR_REMOVE_SCRIPT = "var z = document.getElementById('new-color-preview'); if(z) { document.head.removeChild(z); } ";
    private static final String NEW_PREVIEW_COLOR_SET_STYLE_SCRIPT = "document.getElementById('new-color-preview').innerHTML = newColorPreviewStyle;";
    private static final String PREVIEW_BUTTON_COLOR_CREATE_SCRIPT = "tagColorPreview = document.createElement('style'); tagColorPreview.id=\"tag-color-preview\";  document.head.appendChild(tagColorPreview); ";
    private static final String PREVIEW_BUTTON_COLOR_REMOVE_SCRIPT = "var a = document.getElementById('tag-color-preview'); if(a) { document.head.removeChild(a); } ";
    private static final String PREVIEW_BUTTON_COLOR_SET_STYLE_SCRIPT = "document.getElementById('tag-color-preview').innerHTML = tagColorPreviewStyle;";

    private HawkbitCommonUtil() {

    }

    /**
     * Check wether the given map is not {@code null} and not empty.
     *
     * @param mapToCheck
     *            the map to validate
     * @return {@code true} if the given map is not {@code null} and not empty.
     *         Otherwise {@code false}
     */
    public static boolean isNotNullOrEmpty(final Map<?, ?> mapToCheck) {
        return mapToCheck != null && !mapToCheck.isEmpty();
    }

    /**
     * Trim the text and convert into null in case of empty string.
     *
     * @param text
     *            as text to be trimed
     * @return null if the text is null or if the text is blank, text.trim() if
     *         the text is not empty.
     */
    public static String trimAndNullIfEmpty(final String text) {
        String emptyStr = null;
        if (null != text && !text.trim().isEmpty()) {
            emptyStr = text.trim();
        }
        return emptyStr;
    }

    /**
     * Concatenate the given text all the string arguments with the given
     * delimiter.
     *
     * @param delimiter
     *            the delimiter text to be used while concatenation.
     * @param texts
     *            all these string values will be concatenated with the given
     *            delimiter.
     * @return null in case no text arguments to be compared. just concatenation
     *         of all texts arguments if "delimiter" is null or empty.
     *         concatenation of all texts arguments with "delimiter" if it not
     *         null.
     */
    public static String concatStrings(final String delimiter, final String... texts) {
        final String delim = delimiter == null ? SP_STRING_EMPTY : delimiter;
        final StringBuilder conCatStrBldr = new StringBuilder();
        if (null != texts) {
            for (final String text : texts) {
                conCatStrBldr.append(delim);
                conCatStrBldr.append(text);
            }
        }
        final String conCatedStr = conCatStrBldr.toString();
        return delim.length() > 0 && conCatedStr.startsWith(delim) ? conCatedStr.substring(1) : conCatedStr;
    }

    /**
     * Returns the input text within html bold tag <b>..</b>.
     *
     * @param text
     *            is the text to be converted in to Bold
     * @return null if the input text param is null returns text with <b>...</b>
     *         tags.
     */
    public static String getBoldHTMLText(final String text) {
        String boldStr = null;
        if (text != null) {
            final StringBuilder updatedMsg = new StringBuilder("<b>");
            updatedMsg.append(text);
            updatedMsg.append("</b>");
            boldStr = updatedMsg.toString();
        }
        return boldStr;
    }

    /**
     * Get Label for Artifact Details.
     *
     * @param name
     * @return
     */
    public static String getArtifactoryDetailsLabelId(final String name) {
        return new StringBuilder()
                .append(DIV_DESCRIPTION_START + "Artifact Details of " + getBoldHTMLText(getFormattedName(name)))
                .append(DIV_DESCRIPTION_END).toString();
    }

    /**
     * Get Label for Artifact Details.
     *
     * @param caption
     *            as caption of the details
     * @param name
     *            as name
     * @return
     */
    public static String getSoftwareModuleName(final String caption, final String name) {
        return new StringBuilder()
                .append(DIV_DESCRIPTION_START + caption + " : " + getBoldHTMLText(getFormattedName(name)))
                .append(DIV_DESCRIPTION_END).toString();
    }

    /**
     * Get Label for Action History Details.
     *
     * @param name
     * @return
     */
    public static String getActionHistoryLabelId(final String name) {
        return new StringBuilder()
                .append(DIV_DESCRIPTION_START + "Action History For " + getBoldHTMLText(getFormattedName(name)))
                .append(DIV_DESCRIPTION_END).toString();
    }

    /**
     * Get tool tip for Poll status.
     *
     * @param pollStatus
     * @param i18N
     * @return
     */
    public static String getPollStatusToolTip(final PollStatus pollStatus, final I18N i18N) {
        if (pollStatus != null && pollStatus.getLastPollDate() != null && pollStatus.isOverdue()) {
            final TimeZone tz = SPDateTimeUtil.getBrowserTimeZone();
            return "Overdue for " + SPDateTimeUtil.getDurationFormattedString(
                    pollStatus.getOverdueDate().atZone(SPDateTimeUtil.getTimeZoneId(tz)).toInstant().toEpochMilli(),
                    pollStatus.getCurrentDate().atZone(SPDateTimeUtil.getTimeZoneId(tz)).toInstant().toEpochMilli(),
                    i18N);
        }
        return null;
    }

    /**
     * Null check for text.
     *
     * @param orgText
     *            text to be formatted
     * @return String formatted text
     */
    public static String getFormattedName(final String orgText) {
        return trimAndNullIfEmpty(orgText) == null ? SPUIDefinitions.SPACE : orgText;
    }

    /**
     * Find required extra width of software module.
     *
     * @param newBrowserWidth
     *            new browser width
     * @return float width of software module table
     */
    public static float findRequiredSwModuleExtraWidth(final float newBrowserWidth) {
        return newBrowserWidth > SPUIDefinitions.REQ_MIN_UPLOAD_BROWSER_WIDTH
                ? (newBrowserWidth - SPUIDefinitions.REQ_MIN_UPLOAD_BROWSER_WIDTH) : 0;
    }

    /**
     * Get artifact upload pop up width.
     *
     * @param newBrowserWidth
     *            new browser width
     * @param minPopupWidth
     *            minimum popup width
     * @return float new pop up width
     */
    public static float getArtifactUploadPopupWidth(final float newBrowserWidth, final int minPopupWidth) {
        final float extraWidth = findRequiredSwModuleExtraWidth(newBrowserWidth);
        if (extraWidth + minPopupWidth > SPUIDefinitions.MAX_UPLOAD_CONFIRMATION_POPUP_WIDTH) {
            return SPUIDefinitions.MAX_UPLOAD_CONFIRMATION_POPUP_WIDTH;
        }
        return extraWidth + minPopupWidth;
    }

    /**
     * @param newBrowserHeight
     *            new browser height
     * @param minPopupHeight
     *            minimum pop up height
     * @return float new pop up height
     */
    public static float getArtifactUploadPopupHeight(final float newBrowserHeight, final int minPopupHeight) {
        final float extraWidth = findRequiredSwModuleExtraWidth(newBrowserHeight);
        if (extraWidth + minPopupHeight > SPUIDefinitions.MAX_UPLOAD_CONFIRMATION_POPUP_HEIGHT) {
            return SPUIDefinitions.MAX_UPLOAD_CONFIRMATION_POPUP_HEIGHT;
        }
        return extraWidth + minPopupHeight;
    }

    /**
     * Remove the prefix from text.
     *
     * @param text
     *            name
     * @param prefix
     *            text to be removed
     * @return String name
     */
    public static String removePrefix(final String text, final String prefix) {
        if (text != null) {
            return text.replaceFirst(prefix, "");
        }
        return null;
    }

    /**
     * Get formatted label.Appends ellipses if content does not fit the label.
     *
     * @param labelContent
     *            content
     * @return Label
     */
    public static Label getFormatedLabel(final String labelContent) {
        final Label labelValue = new Label(labelContent, ContentMode.HTML);
        labelValue.setSizeFull();
        labelValue.addStyleName(SPUIDefinitions.TEXT_STYLE);
        labelValue.addStyleName("label-style");
        return labelValue;
    }

    /**
     * Get concatenated string of software module name and version.
     *
     * @param name
     * @param version
     * @return String concatenated string
     */
    public static String getFormattedNameVersion(final String name, final String version) {
        return name + ":" + version;
    }

    /**
     * @param distName
     * @param distVersion
     * @return
     */
    public static String getDistributionNameAndVersion(final String distName, final String distVersion) {
        return new StringBuilder(distName).append(':').append(distVersion).toString();
    }

    /**
     * Display Target Tag action message.
     *
     * @param tagName
     *            as tag name
     * @param result
     *            as TargetTagAssigmentResult
     * @param i18n
     *            I18N
     * @return message
     */
    public static String createAssignmentMessage(final String tagName,
            final AssignmentResult<? extends NamedEntity> result, final I18N i18n) {
        final StringBuilder formMsg = new StringBuilder();
        final int assignedCount = result.getAssigned();
        final int alreadyAssignedCount = result.getAlreadyAssigned();
        final int unassignedCount = result.getUnassigned();
        if (assignedCount == 1) {
            formMsg.append(i18n.get("message.target.assigned.one",
                    new Object[] { result.getAssignedEntity().get(0).getName(), tagName })).append("<br>");
        } else if (assignedCount > 1) {
            formMsg.append(i18n.get("message.target.assigned.many", new Object[] { assignedCount, tagName }))
                    .append("<br>");

            if (alreadyAssignedCount > 0) {
                final String alreadyAssigned = i18n.get("message.target.alreadyAssigned",
                        new Object[] { alreadyAssignedCount });
                formMsg.append(alreadyAssigned).append("<br>");
            }
        }
        if (unassignedCount == 1) {
            formMsg.append(i18n.get("message.target.unassigned.one",
                    new Object[] { result.getUnassignedEntity().get(0).getName(), tagName })).append("<br>");
        } else if (unassignedCount > 1) {
            formMsg.append(i18n.get("message.target.unassigned.many", new Object[] { unassignedCount, tagName }))
                    .append("<br>");
        }
        return formMsg.toString();
    }

    /**
     * Create a lazy query container for the given query bean factory with empty
     * configurations.
     *
     * @param queryFactory
     *            is reference of {@link BeanQueryFactory<? extends
     *            AbstractBeanQuery>} on which lazy container should create.
     * @return instance of {@link LazyQueryContainer}.
     */
    public static LazyQueryContainer createLazyQueryContainer(
            final BeanQueryFactory<? extends AbstractBeanQuery<?>> queryFactory) {
        queryFactory.setQueryConfiguration(Collections.emptyMap());
        return new LazyQueryContainer(new LazyQueryDefinition(true, 20, SPUILabelDefinitions.VAR_NAME), queryFactory);
    }

    /**
     * Create lazy query container for DS type.
     *
     * @param queryFactory
     * @return LazyQueryContainer
     */
    public static LazyQueryContainer createDSLazyQueryContainer(
            final BeanQueryFactory<? extends AbstractBeanQuery<?>> queryFactory) {
        queryFactory.setQueryConfiguration(Collections.emptyMap());
        return new LazyQueryContainer(new LazyQueryDefinition(true, 20, "tagIdName"), queryFactory);
    }

    /**
     * Set distribution table column properties.
     *
     * @param container
     *            table container
     */
    public static void getDsTableColumnProperties(final Container container) {
        final LazyQueryContainer lqc = (LazyQueryContainer) container;
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_ID, Long.class, null, false, false);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, "", false, true);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_VERSION, String.class, null, false, false);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, null, false, true);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_BY, String.class, null, false, true);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, String.class, null, false, true);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, String.class, null, false, true);
        lqc.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE, String.class, null, false, true);
    }

    /**
     * Reset the software module table rows highlight css.
     *
     * @return javascript to rest software module table rows highlight css.
     */
    public static String getScriptSMHighlightReset() {
        return SM_HIGHLIGHT_RESET_SCRIPT;
    }

    /**
     * Highlight software module rows with the color of sw-type.
     *
     * @param colorCSS
     *            color to generate the css script.
     * @return javascript to append software module table rows with highlighted
     *         color.
     */
    public static String getScriptSMHighlightWithColor(final String colorCSS) {
        return new StringBuilder().append(SM_HIGHLIGHT_SCRIPT_CURRENT)
                .append("smHighlightStyle = smHighlightStyle + \"").append(colorCSS).append("\";")
                .append(SM_HIGHLIGHT_SCRIPT_APPEND).toString();
    }

    /**
     * Get javascript to reflect new color selection in color picker preview for
     * name and description fields .
     *
     * @param colorPickedPreview
     *            changed color
     * @return javascript for the selected color.
     */
    public static String changeToNewSelectedPreviewColor(final String colorPickedPreview) {
        return new StringBuilder().append(NEW_PREVIEW_COLOR_REMOVE_SCRIPT).append(NEW_PREVIEW_COLOR_CREATE_SCRIPT)
                .append("var newColorPreviewStyle = \".v-app .new-tag-name{ border: solid 3px ")
                .append(colorPickedPreview)
                .append(" !important; width:138px; margin-left:2px !important; box-shadow:none !important; } \"; ")
                .append("newColorPreviewStyle = newColorPreviewStyle + \".v-app .new-tag-desc{ border: solid 3px ")
                .append(colorPickedPreview)
                .append(" !important; width:138px; height:75px !important; margin-top:4px !important; margin-left:2px !important;;box-shadow:none !important;} \"; ")
                .append(NEW_PREVIEW_COLOR_SET_STYLE_SCRIPT).toString();
    }

    /**
     * Get javascript to reflect new color selection for preview button.
     *
     * @param color
     *            changed color
     * @return javascript for the selected color.
     */
    public static String getPreviewButtonColorScript(final String color) {
        return new StringBuilder().append(PREVIEW_BUTTON_COLOR_REMOVE_SCRIPT).append(PREVIEW_BUTTON_COLOR_CREATE_SCRIPT)
                .append("var tagColorPreviewStyle = \".v-app .tag-color-preview{ height: 15px !important; padding: 0 10px !important; border: 0px !important; margin-left:12px !important;  margin-top: 4px !important; border-width: 0 !important; background: ")
                .append(color)
                .append(" } .v-app .tag-color-preview:after{ border-color: none !important; box-shadow:none !important;} \"; ")
                .append(PREVIEW_BUTTON_COLOR_SET_STYLE_SCRIPT).toString();
    }

    /**
     * Apply style for status label in target table.
     *
     * @param targetTable
     *            target table
     * @param pinBtn
     *            pin button used for status display and pin on mouse over
     * @param itemId
     *            id of the tabel row
     */
    public static void applyStatusLblStyle(final Table targetTable, final Button pinBtn, final Object itemId) {
        final Item item = targetTable.getItem(itemId);
        if (item != null) {
            final TargetUpdateStatus updateStatus = (TargetUpdateStatus) item
                    .getItemProperty(SPUILabelDefinitions.VAR_TARGET_STATUS).getValue();
            pinBtn.removeStyleName("statusIconRed statusIconBlue statusIconGreen statusIconYellow statusIconLightBlue");
            if (updateStatus == TargetUpdateStatus.ERROR) {
                pinBtn.addStyleName("statusIconRed");
            } else if (updateStatus == TargetUpdateStatus.UNKNOWN) {
                pinBtn.addStyleName("statusIconBlue");
            } else if (updateStatus == TargetUpdateStatus.IN_SYNC) {
                pinBtn.addStyleName("statusIconGreen");
            } else if (updateStatus == TargetUpdateStatus.PENDING) {
                pinBtn.addStyleName("statusIconYellow");
            } else if (updateStatus == TargetUpdateStatus.REGISTERED) {
                pinBtn.addStyleName("statusIconLightBlue");
            }
        }
    }

    /**
     * Set status progress bar value.
     *
     * @param bar
     *            DistributionBar
     * @param statusName
     *            status name
     * @param count
     *            target counts in a status
     * @param index
     *            bar part index
     */
    public static void setBarPartSize(final DistributionBar bar, final String statusName, final int count,
            final int index) {
        bar.setPartSize(index, count);
        bar.setPartTooltip(index, statusName);
        bar.setPartStyleName(index, "status-bar-part-" + statusName);
    }

    /**
     * Initialize status progress bar with values and number of parts on load.
     *
     * @param bar
     *            DistributionBar
     * @param item
     *            row of a table
     */
    public static void initialiseProgressBar(final DistributionBar bar, final Item item) {
        final Long notStartedTargetsCount = getStatusCount(SPUILabelDefinitions.VAR_COUNT_TARGETS_NOT_STARTED, item);
        final Long runningTargetsCount = getStatusCount(SPUILabelDefinitions.VAR_COUNT_TARGETS_RUNNING, item);
        final Long scheduledTargetsCount = getStatusCount(SPUILabelDefinitions.VAR_COUNT_TARGETS_SCHEDULED, item);
        final Long errorTargetsCount = getStatusCount(SPUILabelDefinitions.VAR_COUNT_TARGETS_ERROR, item);
        final Long finishedTargetsCount = getStatusCount(SPUILabelDefinitions.VAR_COUNT_TARGETS_FINISHED, item);
        final Long cancelledTargetsCount = getStatusCount(SPUILabelDefinitions.VAR_COUNT_TARGETS_CANCELLED, item);
        if (isNoTargets(errorTargetsCount, notStartedTargetsCount, runningTargetsCount, scheduledTargetsCount,
                finishedTargetsCount, cancelledTargetsCount)) {
            HawkbitCommonUtil.setBarPartSize(bar, TotalTargetCountStatus.Status.SCHEDULED.toString().toLowerCase(), 0,
                    0);
            HawkbitCommonUtil.setBarPartSize(bar, TotalTargetCountStatus.Status.FINISHED.toString().toLowerCase(), 0,
                    1);
        } else {
            bar.setNumberOfParts(6);
            setProgressBarDetails(bar, item);
        }
    }

    /**
     * Formats the finished percentage of a rollout group into a string with one
     * digit after comma.
     *
     * @param rolloutGroup
     *            the rollout group
     * @param finishedPercentage
     *            the calculated finished percentage of the rolloutgroup
     * @return formatted String value
     */
    public static String formattingFinishedPercentage(final RolloutGroup rolloutGroup, final float finishedPercentage) {
        float tmpFinishedPercentage = 0;
        switch (rolloutGroup.getStatus()) {
        case READY:
        case SCHEDULED:
        case ERROR:
            tmpFinishedPercentage = 0.0F;
            break;
        case FINISHED:
            tmpFinishedPercentage = 100.0F;
            break;
        case RUNNING:
            tmpFinishedPercentage = finishedPercentage;
            break;
        default:
            break;
        }
        return String.format("%.1f", tmpFinishedPercentage);
    }

    /**
     * Reset the values of status progress bar on change of values.
     *
     * @param bar
     *            DistributionBar
     * @param item
     *            row of the table
     */
    private static void setProgressBarDetails(final DistributionBar bar, final Item item) {
        bar.setNumberOfParts(6);
        final Long notStartedTargetsCount = getStatusCount(SPUILabelDefinitions.VAR_COUNT_TARGETS_NOT_STARTED, item);
        HawkbitCommonUtil.setBarPartSize(bar, TotalTargetCountStatus.Status.NOTSTARTED.toString().toLowerCase(),
                notStartedTargetsCount.intValue(), 0);
        HawkbitCommonUtil.setBarPartSize(bar, TotalTargetCountStatus.Status.SCHEDULED.toString().toLowerCase(),
                getStatusCount(SPUILabelDefinitions.VAR_COUNT_TARGETS_SCHEDULED, item).intValue(), 1);
        HawkbitCommonUtil.setBarPartSize(bar, TotalTargetCountStatus.Status.RUNNING.toString().toLowerCase(),
                getStatusCount(SPUILabelDefinitions.VAR_COUNT_TARGETS_RUNNING, item).intValue(), 2);
        HawkbitCommonUtil.setBarPartSize(bar, TotalTargetCountStatus.Status.ERROR.toString().toLowerCase(),
                getStatusCount(SPUILabelDefinitions.VAR_COUNT_TARGETS_ERROR, item).intValue(), 3);
        HawkbitCommonUtil.setBarPartSize(bar, TotalTargetCountStatus.Status.FINISHED.toString().toLowerCase(),
                getStatusCount(SPUILabelDefinitions.VAR_COUNT_TARGETS_FINISHED, item).intValue(), 4);
        HawkbitCommonUtil.setBarPartSize(bar, TotalTargetCountStatus.Status.CANCELLED.toString().toLowerCase(),
                getStatusCount(SPUILabelDefinitions.VAR_COUNT_TARGETS_CANCELLED, item).intValue(), 5);
    }

    private static boolean isNoTargets(final Long... statusCount) {
        return (Arrays.asList(statusCount).stream().filter(value -> value > 0).toArray().length) == 0;
    }

    private static Long getStatusCount(final String propertName, final Item item) {
        return (Long) item.getItemProperty(propertName).getValue();
    }

    /**
     * Returns a formatted string as needed by label custom render .This string
     * holds the properties of a status label.
     *
     * @param value
     *            label value
     * @param style
     *            label style
     * @param id
     *            label id
     * @return
     */
    public static String getStatusLabelDetailsInString(final String value, final String style, final String id) {
        final StringBuilder val = new StringBuilder();
        if (!Strings.isNullOrEmpty(value)) {
            val.append("value:").append(value).append(",");
        }
        if (!Strings.isNullOrEmpty(style)) {
            val.append("style:").append(style).append(",");
        }
        return val.append("id:").append(id).toString();
    }

    /**
     * Receive the code point of a given StatusFontIcon.
     *
     * @param statusFontIcon
     *            the status font icon
     * @return the code point of the StatusFontIcon
     */
    public static String getCodePoint(final StatusFontIcon statusFontIcon) {
        if (statusFontIcon == null) {
            return null;
        }
        return statusFontIcon.getFontIcon() != null ? Integer.toString(statusFontIcon.getFontIcon().getCodepoint())
                : null;
    }
}
