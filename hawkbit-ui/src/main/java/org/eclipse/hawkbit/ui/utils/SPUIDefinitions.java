/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.domain.Sort.Direction;

/**
 * Class to provide the unchanged constants.
 *
 *
 *
 *
 *
 */
public final class SPUIDefinitions {
    /**
     * Available locales.
     */
    private static final Set<String> AVAILABLE_LOCALES = Stream.of("en", "de").collect(Collectors.toSet());

    /**
     * Default locale.
     */
    public static final String DEFAULT_LOCALE = "en";
    /**
     * Locale cookie name.
     */
    public static final String COOKIE_NAME = "BOSCHSI_UII_LOCALE";
    /**
     * Lazy query container page size.
     */
    public static final int PAGE_SIZE = 50;

    /**
     * Lazy query container Distribution Set Type size.
     */
    public static final int DIST_TYPE_SIZE = 8;

    /**
     * Target and Distribution set table size.
     */
    public static final int TABLE_MIN_LENGTH = 3;

    /**
     * Action history active column.
     */
    public static final String ACTION_HIS_TBL_ACTIVE = "Active";

    /**
     * Action history action id column.
     */
    public static final String ACTION_HIS_TBL_ACTION_ID = "Action Id";

    /**
     * Action history active hidden column. This is using to generate active
     * icons under active coloumn.
     */
    public static final String ACTION_HIS_TBL_ACTIVE_HIDDEN = "Active_Hidden";

    /**
     * Action history distribution set column.
     */
    public static final String ACTION_HIS_TBL_DIST = "Distributionset";

    /**
     * Action history Action Id hidden column.
     */
    public static final String ACTION_HIS_TBL_ACTION_ID_HIDDEN = "DistributionsetId";

    /**
     * Action history date & time column.
     */
    public static final String ACTION_HIS_TBL_DATETIME = "Date and time";

    /**
     * Action history status column.
     */
    public static final String ACTION_HIS_TBL_STATUS = "Status";

    /**
     * Actions column.
     */
    public static final String ACTIONS_COLUMN = "Actions";

    /**
     * Action history messages of particular action update.
     */
    public static final String ACTION_HIS_TBL_MSGS = "Messages";

    /**
     * Action history hidden column of messages of particular action update.
     */
    public static final String ACTION_HIS_TBL_MSGS_HIDDEN = "Messages_Hidden";

    /**
     * Action history layout - rollout name column.
     */
    public static final String ACTION_HIS_TBL_ROLLOUT_NAME = "Rollout name";

    /**
     * /** Action history status hidden column. This is using to generate status
     * icons under status coloumn.
     */
    public static final String ACTION_HIS_TBL_STATUS_HIDDEN = "Status_Hidden";

    /**
     * Action history action type column.
     */
    public static final String ACTION_HIS_TBL_FORCED = "Forced";

    /**
     * Action history helping constant.
     */
    public static final String ACTIVE = "active";

    /**
     * Action history helping constant.
     */
    public static final String IN_ACTIVE = "inactive";

    /**
     * Action history helping constant.
     */
    public static final String SCHEDULED = "scheduled";

    /**
     * Action history lazy fetch page length.
     */
    public static final int PAGE_LEN_ACTION_HISTORY = 5;

    /**
     * Action history lazy fetch page length in popup.
     */
    public static final int PAGE_LEN_ACTION_HISTORY_IN_POPUP = 4;

    /**
     * Action history messages page length in the popup.
     */
    public static final int PAGE_LEN_MSGS_IN_ACTION_HISTORY = 3;

    /**
     * Target filter box style.
     */
    public static final String TARGET_FILTER_TEXTFIELD_STYLE = "target-filter-box target-filter-box-hide";

    /**
     * Distribution filter box style.
     */
    public static final String DISTRIBUTION_FILTER_TEXTFIELD_STYLE = "dist-filter-box dist-filter-box-hide";

    public static final String TARGET_TAG_BUTTON = "Target Tag";

    public static final String DISTRIBUTION_TAG_BUTTON = "Distribution Tag";

    public static final String SOFTWARE_MODULE_TYPE_BUTTON = "SoftwareModule Type";

    public static final String DISTRIBUTION_SET_TYPE_BUTTON = "DistributionSet Type";
    /**
     * New jvm name field id.
     */
    public static final String NEW_JVM_NAME = "NewJvmName";
    /**
     * New jvm version field id.
     */
    public static final String NEW_JVM_VERSION = "NewJvmVersion";
    /**
     * New JVM vendor name field id.
     */
    public static final String NEW_JVM_VENDOR_NAME = "NewVendorName";
    /**
     * save new jvm icon id.
     */
    public static final String SAVE_NEW_JVM = "SaveNewJvm";
    /**
     * dicard new jvm icon id.
     */
    public static final String DISCARD_NEW_JVM = "DiscardNewJvm";
    /**
     * New agent hub name field id.
     */
    public static final String NEW_AGENT_HUB_NAME = "NewAgentHubName";
    /**
     * New agent hub version field id.
     */
    public static final String NEW_AGENT_HUB_VERSION = "NewAgentHubVersion";
    /**
     * New agent hub vendor field id.
     */
    public static final String NEW_AGENT_HUB_VENDOR = "NewAgentHubVendorName";
    /**
     * Save agent hub icon id.
     */
    public static final String NEW_AGENT_HUB_SAVE = "SaveNewAgentHub";
    /**
     * Discard agent hub icon id.
     */
    public static final String NEW_AGENT_HUB_DISCARD = "DiscardNewAgentHub";
    /**
     * New OS name field id.
     */
    public static final String NEW_OS_NAME = "NewOSName";
    /**
     * New OS version field id.
     */
    public static final String NEW_OS_VERSION = "NewOSVersion";
    /**
     * New OS vendor field id.
     */
    public static final String NEW_OS_VENDOR = "NewOSVendorName";
    /**
     * New OS save icon id.
     */
    public static final String NEW_OS_SAVE = "SaveNewOS";
    /**
     * New OS discard icon id.
     */
    public static final String NEW_OS_DISCARD = "DiscardNewOS";
    /**
     * New distribution set name field id.
     */
    public static final String NEW_DIST_NAME = "NewDistributionName";
    /**
     * New distribution set version field id.
     */
    public static final String NEW_DIST_VERSION = "NewDistributionVersion";
    /**
     * New distribution set description field id.
     */
    public static final String NEW_DIST_DESCRIPTION = "NewDistributionDescription";
    /**
     * New distribution set JVM combo-box id.
     */
    public static final String NEW_DIST_JVM_COMBO = "NewDistributionJvmCombo";
    /**
     * New distribution set Agent hub combo-box id.
     */
    public static final String NEW_DIST_AGENT_HUB_COMBO = "NewDistributionAgentHubCombo";
    /**
     * New distribution set OS combo-box id.
     */
    public static final String NEW_DIST_OS_COMBO = "NewDistributionOSCombo";
    /**
     * New distribution set save icon id.
     */
    public static final String NEW_DIST_SAVE = "NewDistributionSave";
    /**
     * New distribution set discard icon id.
     */
    public static final String NEW_DIST_DISCARD = "NewDistributionSave";
    /**
     * New Target controller id field id.
     */
    public static final String NEW_TARGET_CONTOLLERID = "NewTargetControlerId";
    /**
     * New Target name field id.
     */
    public static final String NEW_TARGET_NAME = "NewTargetName";
    /**
     * New Target description field id.
     */
    public static final String NEW_TARGET_DESC = "NewTargetDescription";
    /**
     * New Target save icon id.
     */
    public static final String NEW_TARGET_SAVE = "target.add.save";
    /**
     * New Target discard icon id.
     */
    // public static final String NEW_TARGET_DISCARD = "target.add.discard";
    /**
     * New Target add icon id.
     */
    public static final String NEW_TARGET_ADD_ICON = "NewTargetAddIcon";
    /**
     * New distribution set add icon id.
     */
    public static final String NEW_DIST_ADD_ICON = "NewDistAddIcon";
    /**
     * New Target tag name field id.
     */
    public static final String NEW_TARGET_TAG_NAME = "target.tag.add.name";

    /**
     * New Software Module name field id.
     */
    public static final String NEW_SOFTWARE_TYPE_NAME = "software.type.add.name";

    /**
     * New Distribution Type name field id.
     */
    public static final String NEW_DISTRIBUTION_TYPE_NAME = "distribution.set.type.add.name";

    /**
     * New Distribution Type key field id.
     */
    public static final String NEW_DISTRIBUTION_TYPE_KEY = "distribution.set.type.add.key";

    /**
     * New Create Update option group id.
     */
    public static final String CREATE_OPTION_GROUP_DISTRIBUTION_SET_TYPE_ID = "create.option.group.dist.set.type.id";
    /**
     * SW Module Source Table ID.
     */
    public static final String SW_MODULE_TYPE_SOURCE_TABLE_ID = "sw.module.type.source.tab.id";

    /**
     * SW Module target Table ID.
     */
    public static final String SW_MODULE_TYPE_TARGET_TABLE_ID = "sw.module.type.target.tab.id";

    /**
     * New Software Module desc field id.
     */
    public static final String NEW_SOFTWARE_TYPE_DESC = "software.type.add.desc";
    /**
     * Hide filter by dist type layout button.
     */
    public static final String HIDE_FILTER_DIST_TYPE = "hide.filter.dist.type.layout";
    /**
     * New Distribution Type distribution field id.
     */
    public static final String NEW_DISTRIBUTION_TYPE_DESC = "distribution.set.type.add.desc";

    /**
     * New Distribution Type distribution field id.
     */
    public static final String NEW_DISTRIBUTION_SET_TYPE_NAME_COMBO = "distribution.set.type.name.combo";

    /**
     * New Software Module key field id.
     */
    public static final String NEW_SOFTWARE_TYPE_KEY = "software.type.add.key";
    /**
     * New Target tag desc field id.
     */
    public static final String NEW_TARGET_TAG_DESC = "target.tag.add.desc";
    /**
     * New Target tag color lable id.
     */
    public static final String NEW_TARGET_TAG_COLOR = "target.tag.add.color";
    /**
     * New Target tag save icon id.
     */
    // public static final String NEW_TARGET_TAG_SAVE = "target.tag.add.save";
    /**
     * New Target tag discard icon id.
     */
    // public static final String NEW_TARGET_TAG_DISRACD =
    // "target.tag.add.discard";
    /**
     * New Target tag add icon id.
     */
    public static final String ADD_TARGET_TAG = "target.tag.add";
    /**
     * New distribution set tag add icon id.
     */
    public static final String ADD_DISTRIBUTION_TAG = "distribution.tag.add";
    /**
     * New distribution Type set tag add icon id.
     */
    public static final String ADD_DISTRIBUTION_TYPE_TAG = "distribution.type.tag.add";
    /**
     * New software module set type add icon id.
     */
    public static final String ADD_SOFTWARE_MODULE_TYPE = "softwaremodule.type.add";

    /**
     * New distribution set tag name field id.
     */
    public static final String NEW_DISTRIBUTION_TAG_NAME = "distribution.add.tag.name";
    /**
     * New distribution set tag color lable id.
     */
    public static final String NEW_DISTRIBUTION_TAG_COLOR = "NewDistributionTagColor";
    /**
     * New distribution set tag save icon id.
     */
    public static final String NEW_DISTRIBUTION_TAG_SAVE = "distribution.tag.save";
    /**
     * New distribution set tag discard icon id.
     */
    public static final String NEW_DISTRIBUTION_TAG_DISRACD = "NewDistributionTagDiscard";
    /**
     * No data.
     */
    public static final String NO_DATA = "No Data";

    /**
     * No Available.
     */
    public static final String DATA_AVAILABLE = "Data available";

    /**
     * Filter by status key.
     */
    public static final String FILTER_BY_STATUS = "FilterByStatus";
    /**
     * Filter by tag key.
     */
    public static final String FILTER_BY_TAG = "FilterByTag";
    /**
     * Filter by no tag button.
     */
    public static final String FILTER_BY_NO_TAG = "FilterByNoTag";
    /**
     * Filter by target filter query.
     */
    public static final String FILTER_BY_TARGET_FILTER_QUERY = "FilterByTargetFilterQuery";

    /**
     * Filter by distribution key.
     */
    public static final String FILTER_BY_DISTRIBUTION = "FilterByDistribution";
    /**
     * Filter by distributionSet Type.
     */
    public static final String FILTER_BY_DISTRIBUTION_SET_TYPE = "FilterByDistributionSetType";
    /**
     * Order by pinnedTarget.
     */
    public static final String ORDER_BY_PINNED_TARGET = "OrderByPinnedTarget";
    /**
     * Filter by text key.
     */
    public static final String FILTER_BY_TEXT = "FilterByText";
    /**
     * Visible column name of target table.
     */
    public static final String TARGET_TABLE_VISIBILE_COLUMN_NAME = "nameDescLabel";
    /**
     * Visible column name of distribution table.
     */
    public static final String DISTRIBUTION_TABLE_VISIBILE_COLUMN_NAME = "nameDescLabel";

    /**
     * Text area style.
     */
    public static final String TEXT_AREA_STYLE = "text-style text-area-style";

    /**
     * Combo box style.
     */
    public static final String COMBO_BOX_STYLE = "combo-box-style text-field-spacing";

    /**
     * Text field style.
     */
    public static final String TEXT_STYLE = "text-style";

    /**
     * Message to be displayed when no data available for chosen filter.
     */
    public static final String NO_DATA_MSG = "No data available for chosen filter";

    /**
     * Style to highlight row in orange color.
     */
    public static final String HIGHTLIGHT_ORANGE = "highlight-orange";

    /**
     * Style to highlight row in green color.
     */
    public static final String HIGHTLIGHT_GREEN = "highlight-green";

    /**
     * Action window show style.
     */
    public static final String ACTION_WINDOW_SHOW_STYLE = "action-window" + " " + "action-window-show";

    /**
     * Action window hide style.
     */
    public static final String ACTION_WINDOW_HIDE_STYLE = "action-window" + " " + "action-window-hide";

    /**
     * The Combo box specific style for Distribution set.
     */
    public static final String COMBO_BOX_SPECIFIC_STYLE = "combo-specific-style";

    /**
     * Length of Assignment details table in save popup window.
     */
    public static final int ACCORDION_TAB_DETAILS_PAGE_LENGTH = 7;

    /**
     * Target and distribution column width in save popup window.
     */
    public static final float TARGET_DISTRIBUTION_COLUMN_WIDTH = 2.8f;
    /**
     * Discard column width in save window popup.
     */
    public static final float DISCARD_COLUMN_WIDTH = 1.2f;
    /**
     * message hint text layout.
     */
    public static final String MESSAGE_HINT_TEXT_LAYOUT = "message-hint-text-layout";

    /**
     * Message hint bottom layout.
     */
    public static final String MESSAGE_HINT_BOTTOM_LAYOUT = "message-hint-bottom-layout";

    /**
     * The Target Tag edit icon id.
     */
    public static final String EDIT_TARGET_TAG = "target.tag.edit";
    /**
     * The Dist Tag edit icon id.
     */
    public static final String EDIT_DISTRIBUTION_TAG = "dist.tag.edit";
    /**
     * Target Tag color picker style.
     */
    public static final String TARGET_COLOR_PICKER_STYLE = "target-color-picker-style";
    /**
     * Target tag name.
     */
    public static final String TAG_NAME = "target-tag-name";
    /**
     * Target tag desc.
     */
    public static final String TAG_DESC = "target-tag-desc";
    /**
     * Software type name.
     */
    public static final String TYPE_NAME = "type-name";
    /**
     * DistributionSet type name.
     */
    public static final String DIST_SET_TYPE_NAME = "dist-set-type-name";

    /**
     * DiscriptionSet type desc.
     */
    public static final String DIST_SET_TYPE_DESC = "dist-set-type-desc";

    /**
     * DistributionSet type key.
     */
    public static final String DIST_SET_TYPE_KEY = "dist-set-type-key";
    /**
     * Software type desc.
     */
    public static final String TYPE_DESC = "type-desc";

    /**
     * Software type key.
     */
    public static final String TYPE_KEY = "type-key";

    /**
     * Type combo style.
     */
    public static final String TYPE_COMBO_STYLE = "type-combo-specific-style";

    /**
     * Tag combo style.
     */
    public static final String FILTER_TYPE_COMBO_STYLE = "filter-combo-specific-style";
    /**
     * Color label style.
     */
    public static final String COLOR_LABEL_STYLE = "color-label-style";

    /**
     * Text area of tag description style.
     */
    public static final String TARGET_TAG_TEXT_AREA_STYLE = "target-tag-desc";

    /**
     * Distribution table.
     */
    public static final String DISTRIBUTION_TABLE = "dist-table";

    /**
     * Target tag.
     */
    public static final String TARGET_TAG = "target-tag";
    /**
     * Distribution tag.
     */
    public static final String DIST_TAG = "dist-tag";

    /**
     * Minimum height for the target tags layout. In case browser height is less
     * than this height, the vertical scroll bars get displayed. Used for
     * Responsive UI.
     */
    public static final int MIN_TARGET_TAGS_HEIGHT = 39;

    /**
     * Minimum height for the distribution type tags layout. In case browser
     * height is less than this height, the vertical scroll bars get displayed.
     * Used for Responsive UI.
     */
    public static final int MIN_DISTRIBUTION_TYPE_TAGS_HEIGHT = 39;

    /**
     * Minimum height for the distribution tags layout. In case browser height
     * is less than this height, the vertical scroll bars get displayed. Used
     * for Responsive UI.
     */
    public static final int MIN_DIST_TAGS_HEIGHT = 109;

    /**
     * Minimum height required for target or distribution table with minimum
     * records. This is calculated as ( no. of min rows * height of each row ).
     * Used for Responsive UI.
     */
    public static final int MIN_TARGET_DIST_TABLE_HEIGHT = TABLE_MIN_LENGTH * 37 - 42;

    /**
     * Minimum width required to show tags for both target and distribution
     * table without horizontal scroll. In case browser width is less than this
     * width, the tags will get hidden automatically and also horizontal scroll
     * bars get displayed. Used for Responsive UI.
     */
    public static final int REQ_MIN_BROWSER_WIDTH = 1200;

    public static final int REQ_MIN_BROWSER_WIDTH_UPLOAD = 1200;

    public static final int REQ_MIN_BROWSER_WIDTH_DIST = 1300;

    /**
     * Target table maximum width.
     */
    public static final int MAX_TARGET_TABLE_WIDTH = 700;

    /**
     * Distribution table maximum width.
     */
    public static final int MAX_DIST_TABLE_WIDTH = 425;

    /**
     * Details layout- initial margin left value.
     */
    public static final int DETAILS_MARGIN_LEFT = 275;

    /**
     * Distributions table details layout- initial margin left value.
     */
    public static final int DIST_TABALE_DETAILS_MARGIN_LEFT = 288;
    /**
     * Minimum height required to show the application without need of vertical
     * scroll. In case browser height is more than this height, the vertical
     * scroll bars get displayed. Used for Responsive UI.
     */
    public static final int REQ_MIN_BROWSER_HEIGHT = 350;

    public static final int MIN_TABLE_WIDTH = 300;

    /**
     * Minimum width required to display action history.
     */
    public static final int ACTION_HISTORY_MIN_REQ_WIDTH = 500;

    /**
     * Minimum height required to display action history (5 rows minimum).
     */
    public static final int ACTION_HISTORY_MIN_REQ_HEIGHT = 130;

    /**
     * Position of the action history from top.
     */
    public static final int ACTION_HISTORY_POSITION_Y = 160;

    /**
     * The total amount of space ( top & bottom ) the action history to
     * maintain.
     */
    public static final int ACTION_HISTORY_LEFTOVER_HEIGHT = 420;

    /**
     * Target tag button id prefix.
     */
    public static final String TARGET_TAG_ID_PREFIXS = "target.tag.";
    /**
     * Distribution tag button id prefix.
     */
    public static final String DISTRIBUTION_TAG_ID_PREFIXS = "dist.tag.";

    /**
     * Distribution tag button id prefix.
     */
    public static final String SOFTWARE_MODULE_TAG_ID_PREFIXS = "swmodule.type.";
    /**
     * Distribution tag button id prefix.
     */
    public static final String DISTRIBUTION_TYPE_ID_PREFIXS = "dist-type-";

    /**
     * DistributionSet Type tag button id prefix.
     */
    public static final String DISTRIBUTION_SET_TYPE_ID_PREFIXS = "dist.set.type.";
    /**
     * Target/distribution description length.
     */
    public static final int NAME_DESCRIPTION_LENGTH = 26;
    /**
     * Target last query date format .
     */
    public static final String LAST_QUERY_DATE_FORMAT = "EEE MMM d HH:mm:ss z yyyy";

    /**
     * Space.
     */
    public static final String SPACE = "&nbsp;";
    /**
     * Item Id used in drop comparisons.
     */
    public static final String ITEMID = "itemId";
    /**
     * Expand action history.
     */
    public static final String EXPAND_ACTION_HISTORY = "expand.action.history";
    /**
     * Close action history.
     */
    public static final String CLOSE_ACTION_HISTORY = "close.action.history";
    /**
     * Filter by distribution key.
     */
    public static final String ORDER_BY_DISTRIBUTION = "OrderByDistribution";
    /**
     * Dist window width.
     */
    public static final int DIST_WINDOW_WIDTH = 235;
    /**
     * Dist window height.
     */
    public static final int DIST_WINDOW_HEIGHT = 380;

    /** Artifact upload related entries - start **/
    /**
     * Artifact details by Base software module id.
     */
    public static final String BY_BASE_SOFTWARE_MODULE = "ByBaseSoftwareModule";

    /**
     * Software module type.
     */
    public static final String BY_SOFTWARE_MODULE_TYPE = "softwareModuleType";

    /**
     * Minimum height required to show the application without need of vertical
     * scroll. In case browser height is more than this height, the vertical
     * scroll bars get displayed. Used for Responsive UI.
     */
    public static final int REQ_MIN_UPLOAD_BROWSER_HEIGHT = 380;

    /**
     * Minimum width required to display ui without horizontal scroll. In case
     * browser width is less than this width, the type layout will get hidden
     * automatically and also horizontal scroll bars get displayed. Used for
     * Responsive UI.
     */
    public static final int REQ_MIN_UPLOAD_BROWSER_WIDTH = 1250;

    /**
     * Minimum width of software module table.
     */
    public static final int MIN_SOFTWARE_MODULE_TABLE_WIDTH = 300;

    /**
     * Maximum allowed width of software module table.
     */
    public static final int MAX_UPLOAD_SW_MODULE_TABLE_WIDTH = 450;

    /**
     * Minimum artifact table height.
     */
    public static final int MIN_UPLOAD_ARTIFACT_TABLE_HEIGHT = 168;

    /**
     * Maximum artifact table height.
     */
    public static final int MAX_UPLOAD_ARTIFACT_TABLE_HEIGHT = 450;

    /**
     * Maximum drop layout width.
     */
    public static final int MAX_UPLOAD_DROP_LAYOUT_WIDTH = 650;

    /**
     * Minimum drop layout width.
     */
    public static final int MIN_UPLOAD_DROP_LAYOUT_WIDTH = 450;

    public static final int MIN_UPLOAD_CONFIRMATION_POPUP_WIDTH = 1000;

    public static final int MAX_UPLOAD_CONFIRMATION_POPUP_WIDTH = 1050;

    public static final int MIN_UPLOAD_CONFIRMATION_POPUP_HEIGHT = 310;

    public static final int MAX_UPLOAD_CONFIRMATION_POPUP_HEIGHT = 360;

    public static final String SOFTWARE_MODULE_TABLE = "software-module-table";

    /** Artifact upload related entries - end. **/

    public static final int MIN_DASHBOARD_HEIGHT = 600;

    public static final int MIN_DASHBOARD_WIDTH = 1100;

    public static final int TAG_LAYOUT_WIDTH = 160;

    public static final int MIN_SWMODULE_TABLE_HEIGHT = 160;

    public static final int MIN_SWMODULE_TABLE_WIDTH = 150;

    /* Target Header Filter Box */
    public static final String FILTER_BOX_SHOW = "filter-box-show";
    public static final String FILTER_BOX_HIDE = "filter-box-hide";
    public static final String FILTER_RESET_ICON = "filter-reset-icon";

    /* Action History */
    public static final String CAPTION_ACTION_HISTORY = "caption.action.history";
    public static final String DISABLE_DISTRIBUTION = "incomplete-distribution";

    public static final int MIN_SWTABLE_WIDTH = 448;

    /**
     * New Target tag save icon id.
     */
    public static final String NEW_SW_TYPE_SAVE = "swmodule.type.add.save";

    /**
     * New DistributionSet type save icon id.
     */
    public static final String NEW_DIST_SET_TYPE_SAVE = "dist.set.type.add.save";

    /**
     * New distribution set type close icon id.
     */
    public static final String NEW_DIST_SET_TYPE_COLSE = "dist.set.type.add.close";

    /**
     * Filter by type layout width.
     */
    public static final float FILTER_BY_TYPE_WIDTH = 150.0f;

    /**
     * Confirmation jukebox type.
     */
    public static final String CONFIRMATION_WINDOW = "confirmation-window";

    /**
     * Create/Update window type.
     */
    public static final String CREATE_UPDATE_WINDOW = "create-update-window";

    /**
     * Defines the maximum entries in the target table before the table
     * truncates it. This protects to endless scroll to very high page numbers
     * which is very in performant.
     */
    public static final int MAX_TARGET_TABLE_ENTRIES = 5000;

    /**
     * New software module set type add icon id.
     */
    public static final String CONFIG_FILTER_BUTTON = "config.filter.button";

    /**
     * label style name for assigned sw modules.
     */

    public static final String ASSIGN_SW_MODUE_STYLE = "assignlabelstyle";

    /**
     * Software module add/update window height.
     */
    public static final int SW_ADD_UPDATE_WINDOW_HEIGHT = 350;

    /**
     * Distribution Type Twin Table selected table Id.
     */
    public static final String TWIN_TABLE_SELECTED_ID = "dist.type.selected.id";

    /**
     * Distribution Type Twin Table source table Id.
     */
    public static final String TWIN_TABLE_SOURCE_ID = "dist.type.source.id";

    /**
     * Target table status/pin icon column Id.
     */
    public static final String TARGET_STATUS_PIN_TOGGLE_ICON = "targetStatusPinToggle";

    /**
     * Target table poll time icon column Id.
     */
    public static final String TARGET_STATUS_POLL_TIME = "targetPollTime";

    /**
     * Default green color for filters.
     */
    public static final String DEFAULT_COLOR = "rgb(44,151,32)";

    /**
     * Id f "NO TAG" button.
     */
    public static final String NO_TAG_BUTTON_ID = "no_tag_button";

    /**
     * CUSTOM_FILTER_DELETE.
     */
    public static final String CUSTOM_FILTER_DELETE = "Delete";

    /**
     * CUSTOM_FILTER_INSTALLED_DS.
     */
    public static final String CUSTOM_FILTER_INSTALLED_DS = "Installed DS";

    /**
     * CUSTOM_FILTER_ASSIGNED_DS.
     */
    public static final String CUSTOM_FILTER_ASSIGNED_DS = "Assigned DS";

    /**
     * TARGET_FILTER_MANAGEMENT - header caption .
     */
    public static final String TARGET_FILTER_LIST_HEADER_CAPTION = "Custom Filters";

    /**
     * CUSTOM_FILTER_STATUS.
     */
    public static final String TARGET_FILTER_STATUS = "Status";

    /**
     * CUSTOM_FILTER_INSTALLED_DS.
     */
    public static final String TARGET_FILTER_TAGS = "TAGS";

    /**
     * CUSTOM_FILTER_ID_HIDDEN.
     */
    public static final String CUSTOM_FILTER_ID_HIDDEN = "id";

    /**
     * Bulk upload DS combo style.
     */
    public static final String BULK_UPLOD_DS_COMBO_STYLE = "bulk-upload-ds-combo";

    /**
     * Bulk Targets upload window.
     */
    public static final String BULK_UPLOAD_WINDOW = "bulk-update-window";

    /**
     * Filter by target filter query.
     */
    public static final String FILTER_BY_QUERY = "FilterByTargetFilterQueryText";

    /**
     * Filter by invalid target filter query.
     */
    public static final String FILTER_BY_INVALID_QUERY = "FilterByInvalidFilterQueryText";

    /**
     * Sort order of column - created at in target table.
     */
    public static final Direction TARGET_TABLE_CREATE_AT_SORT_ORDER = Direction.ASC;

    /**
     * Rollout list view - header caption .
     */
    public static final String ROLLOUT_LIST_HEADER_CAPTION = "Rollouts";

    /**
     * Rollout status.
     */
    public static final String ROLLOUT_STATUS = "rollout-status";

    /**
     * Rollout group list view - header caption .
     */
    public static final String ROLLOUT_GROUP_LIST_HEADER_CAPTION = "Rollout Groups";

    /**
     * Rollout delete - column property name.
     */
    public static final String DELETE = "delete";

    /**
     * Rollout detail status - column property status.
     */
    public static final String DETAIL_STATUS = "detail-status";

    /**
     * Rollout name column property.
     */
    public static final String ROLLOUT_NAME = "rollout-name";
    /**
     * Rollout group name column property.
     */
    public static final String ROLLOUT_GROUP_NAME = "Name";

    /**
     * Rollout group started date column property.
     */
    public static final String ROLLOUT_GROUP_STARTED_DATE = "Started date";

    /**
     * Rollout group status column property.
     */
    public static final String ROLLOUT_GROUP_STATUS = "Status";

    /**
     * Rollout action column property.
     */
    public static final String ROLLOUT_ACTION = "rollout-action";

    /**
     * /** Constructor.
     */
    private SPUIDefinitions() {

    }

    /**
     * Get the locales
     * 
     * @return the availableLocales
     */
    public static Set<String> getAvailableLocales() {
        return AVAILABLE_LOCALES;
    }
}
