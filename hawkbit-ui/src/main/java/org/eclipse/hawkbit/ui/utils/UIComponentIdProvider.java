/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.ui.utils;

import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;

/**
 * Interface to provide the unchanged constants.
 */
public final class UIComponentIdProvider {
    /**
     * ID-Target.
     */
    public static final String TARGET_TABLE_ID = "target.tableId";
    /**
     * caption for add target window
     */
    public static final String TARGET_ADD_CAPTION = "caption.add.new.target";
    /**
     * caption for update target window
     */
    public static final String TARGET_UPDATE_CAPTION = "caption.update.target";
    /**
     * ID- Targ.Cont ID.
     */
    public static final String TARGET_ADD_CONTROLLER_ID = "target.add.ctrlId";
    /**
     * ID-Targ.Name.
     */
    public static final String TARGET_ADD_NAME = "target.add.name";
    /**
     * ID-Targ.Disc.
     */
    public static final String TARGET_ADD_DESC = "target.add.desc";
    /**
     * ID-Targ.DEL.
     */
    public static final String TARGET_DELETE_ALL = "target.delete.all";
    /**
     * ID-SW.DEL.
     */
    public static final String SW_DELETE_ALL = "swmodule.delete.all";
    /**
     * ID-Targ.Edit.icon.
     */
    public static final String TARGET_EDIT_ICON = "target.edit.icon";
    /**
     * ID-Targ.PIN.
     */
    public static final String TARGET_PIN_ICON = "target.pin.icon.";
    /**
     * Target search text id.
     */
    public static final String TARGET_TEXT_FIELD = "target.search.textfield";
    /**
     * ID for target filter search
     */
    public static final String TARGET_FILTER_SEARCH_TEXT = "target.filter.search.text.Id";
    /**
     * ID for add target filter icon
     */
    public static final String TARGET_FILTER_ADD_ICON_ID = "target.filter.add.id";

    /**
     * ID for NO TAG for targets
     */
    public static final String NO_TAG_TARGET = "no.tag.target";

    /**
     * ID-Dist. on deployment and distribution view
     */
    public static final String DIST_TABLE_ID = "dist.tableId";

    public static final String DIST_ADD_CAPTION = "caption.add.dist";

    public static final String DIST_UPDATE_CAPTION = "caption.update.dist";

    /**
     * ID-Dist.Name.
     */
    public static final String DIST_ADD_NAME = "dist.add.name";
    /**
     * ID-Dist.Version.
     */
    public static final String DIST_ADD_VERSION = "dist.add.version";
    /**
     * ID-Dist.DistSetType.
     */
    public static final String DIST_ADD_DISTSETTYPE = "dist.add.distsettype";
    /**
     * ID-Dist.desc.
     */
    public static final String DIST_ADD_DESC = "dist.add.desc";
    /**
     * /** ID-Dist.DELETE.
     */
    public static final String DIST_DELETE_ALL = "dist.delete.all";
    /**
     * ID-Dist.MigCheck.
     */
    public static final String DIST_ADD_MIGRATION_CHECK = "dist.add.required.migration";
    /**
     * ID-Dist.Search.icon.
     */
    public static final String DIST_SEARCH_ICON = "dist.search.rest.icon";
    /**
     * ID-Dist.Search.txt.
     */
    public static final String DIST_SEARCH_TEXTFIELD = "dist.search.textfield";
    /**
     * ID - Dist.Add.
     */
    public static final String DIST_ADD_ICON = "dist.add.icon";
    /**
     * ID for Distribution Tag ComboBox
     */
    public static final String DIST_TAG_COMBO = "dist.tag.combo";
    /**
     * ID-Dist.PIN.
     */
    public static final String DIST_PIN_BUTTON = "dist.pin.button";
    /**
     * ID for NO TAG for distribution sets
     */
    public static final String NO_TAG_DISTRIBUTION_SET = "no.tag.distribution.set";
    /**
     * ID for distribution set tag icon
     */
    public static final String SHOW_DIST_TAG_ICON = "show.dist.tags.icon";
    /**
     * ID - soft.module.name.
     */
    public static final String SOFT_MODULE_NAME = "soft.module.name";
    /**
     * ID - soft.module.version.
     */
    public static final String SOFT_MODULE_VERSION = "soft.module.version";
    /**
     * ID - soft.module.vendor.
     */
    public static final String SOFT_MODULE_VENDOR = "soft.module.vendor";
    /**
     * ID - Save Assign.
     */
    public static final String SAVE_ASSIGNMENT = "save.actions.popup.assign";
    /**
     * ID - Discard Assign.
     */
    public static final String DISCARD_ASSIGNMENT = "discard.actions.popup.assign";
    /**
     * ID - Delete Distribution SetType Save.
     */
    public static final String SAVE_DELETE_DIST_SET_TYPE = "save.actions.popup.delete.dist.set.type";
    /**
     * ID - Discard Distribution SetType.
     */
    public static final String DISCARD_DIST_SET_TYPE = "save.actions.popup.discard.dist.set.type";
    /**
     * ID Delete Software Module Type save.
     */
    public static final String SAVE_DELETE_SW_MODULE_TYPE = "save.actions.popup.delete.sw.module.type";
    /**
     * ID - Discard SW Module Type.
     */
    public static final String DISCARD_SW_MODULE_TYPE = "save.actions.popup.discard.sw.module.type";
    /**
     * Action history table cancel Id.
     */
    public static final String ACTION_DETAILS_SOFT_ID = "action.details.soft.group";
    /**
     * Start type of rollout manual radio button
     */
    public static final String ROLLOUT_START_MANUAL_ID = "rollout.start.manual.radio";

    /**
     * Start type of rollout auto start radio button
     */
    public static final String ROLLOUT_START_AUTO_ID = "rollout.start.auto.radio";

    /**
     * Start type of rollout scheduled start radio button
     */
    public static final String ROLLOUT_START_SCHEDULED_ID = "rollout.start.scheduled.start.radio";

    /**
     * ID - Label.
     */
    public static final String ACTION_LABEL = "save.actions.popup.actionMsg";
    /**
     * ID - Count.
     */
    public static final String COUNT_LABEL = "count.message.label";
    /**
     * UNKNOWN_STATUS_ICON ID.
     */
    public static final String UNKNOWN_STATUS_ICON = "unknown.status.icon";
    /**
     * INSYNCH_STATUS_ICON ID.
     */
    public static final String INSYNCH_STATUS_ICON = "insynch.status.icon";
    /**
     * PENDING_STATUS_ICON ID.
     */
    public static final String PENDING_STATUS_ICON = "pending.status.icon";
    /**
     * ERROR_STATUS_ICON ID.
     */
    public static final String ERROR_STATUS_ICON = "error.status.icon";
    /**
     * REGISTERED_STATUS_ICON ID.
     */
    public static final String REGISTERED_STATUS_ICON = "registered.status.icon";
    /**
     * OVERDUE_STATUS_ICON ID.
     */
    public static final String OVERDUE_STATUS_ICON = "overdue.status.icon";
    /**
     * DROP filter icon id.
     */
    public static final String TARGET_DROP_FILTER_ICON = "target.drop.filter.icon";

    /**
     * Pending action button id.
     */
    public static final String PENDING_ACTION_BUTTON = "pending.action.button";
    /**
     * Action history grid Id.
     */
    public static final String ACTION_HISTORY_GRID_ID = "action.history.gridId";
    /**
     * Action history details grid Id.
     */
    public static final String ACTION_HISTORY_DETAILS_GRID_ID = "action.history.details.gridId";
    /**
     * Action history message grid Id.
     */
    public static final String ACTION_HISTORY_MESSAGE_GRID_ID = "action.history.message.gridId";
    /**
     * Action history table cancel button Id.
     */
    public static final String ACTION_HISTORY_TABLE_CANCEL_ID = "action.history.table.action.cancel";
    /**
     * Action history table force button Id.
     */
    public static final String ACTION_HISTORY_TABLE_FORCE_ID = "action.history.table.action.force";
    /**
     * Action history table force quit button Id.
     */
    public static final String ACTION_HISTORY_TABLE_FORCE_QUIT_ID = "action.history.table.action.force.quit";
    /**
     * Action history table forced label Id.
     */
    public static final String ACTION_HISTORY_TABLE_FORCED_LABEL_ID = "action.history.table.forcedId";

    /**
     * Action history table time-forced label Id.
     */
    public static final String ACTION_HISTORY_TABLE_TIMEFORCED_LABEL_ID = "action.history.table.timedforceId";

    /**
     * Action history table status label Id.
     */
    public static final String ACTION_HISTORY_TABLE_STATUS_LABEL_ID = "action.history.table.statusId";

    /**
     * Action history table active-state label Id.
     */
    public static final String ACTION_HISTORY_TABLE_ACTIVESTATE_LABEL_ID = "action.history.table.activeStateId";

    /**
     * Action status grid status label Id.
     */
    public static final String ACTION_STATUS_GRID_STATUS_LABEL_ID = "action.status.grid.statusId";

    /**
     * ID for option group save timeforced
     */
    public static final String ACTION_TYPE_OPTION_GROUP_SAVE_TIMEFORCED = "save.action.radio.timeforced";

    /**
     * Target filter wrapper id.
     */
    public static final String TARGET_FILTER_WRAPPER_ID = "target-drop-filter";

    /**
     * ID for HorizontalLayout containing Action Buttons (delete, action)
     */
    public static final String ACTION_BUTTON_LAYOUT = "ActionButtonLayout";

    /**
     * Delete button wrapper id.
     */
    public static final String DELETE_BUTTON_WRAPPER_ID = "delete.button";
    /**
     * tag color preview button id.
     */
    public static final String TAG_COLOR_PREVIEW_ID = "tag.color.preview";
    /**
     * Id for ColorPickerLayout
     */
    public static final String COLOR_PICKER_LAYOUT = "color.picker.layout";
    /**
     * Id for ColorPickerLayout's red slider
     */
    public static final String COLOR_PICKER_RED_SLIDER = "color.picker.red.slider";
    /**
     * Id for Color preview field with the color code
     */
    public static final String COLOR_PREVIEW_FIELD = "color-preview-field";
    /**
     * Id for OptionGroup Create/Update tag
     */
    public static final String OPTION_GROUP = "create.update.tag";
    /**
     * Confirmation dialogue OK button id.
     */
    public static final String OK_BUTTON = "ok.button";
    /**
     * Upload - type button id.
     */
    public static final String UPLOAD_TYPE_BUTTON_PREFIX = "upload.type.button.";
    /**
     * Upload - process button id.
     */
    public static final String UPLOAD_PROCESS_BUTTON = "upload.process.button";
    /**
     * Upload - discard button id.
     */
    public static final String UPLOAD_DISCARD_BUTTON = "upload.discard.button";

    /**
     * Upload - artifact detail close button.
     */
    public static final String UPLOAD_ARTIFACT_DETAILS_CLOSE = "upload.artifactdetails.close.button";
    /**
     * Upload - artifact detail table.
     */
    public static final String UPLOAD_ARTIFACT_DETAILS_TABLE = "upload.artifactdetails.table";

    /**
     * Upload - artifact upload button.
     */
    public static final String UPLOAD_BUTTON = "artifact.upload.button";
    /**
     * Upload - process button id.
     */
    public static final String UPLOAD_DISCARD_DETAILS_BUTTON = "upload.discard.details.button";
    /**
     * Upload - delete button id.
     */
    public static final String UPLOAD_DELETE_ICON = "upload.delete.button";

    /**
     * Upload- file delete button id.
     */
    public static final String UPLOAD_FILE_DELETE_ICON = "upload.file.delete.button";
    /**
     * ID-Dist.
     */
    public static final String UPLOAD_SOFTWARE_MODULE_TABLE = "upload.swModule.table";

    /**
     * Upload result popup close button.
     */
    public static final String UPLOAD_ARTIFACT_RESULT_POPUP_CLOSE = "upload.resultwindow.close.button";

    /**
     * Upload result popup close button.
     */
    public static final String UPLOAD_ARTIFACT_RESULT_CLOSE = "upload.results.close.button";
    /**
     * Upload - artifact result table.
     */
    public static final String UPLOAD_RESULT_TABLE = "upload.result.table";
    /**
     * Upload - software module search text id.
     */
    public static final String SW_MODULE_SEARCH_TEXT_FIELD = "swmodule.search.textfield";
    /**
     * Upload - software module search reset icon id.
     */
    public static final String SW_MODULE_SEARCH_RESET_ICON = "sw.search.reset.icon";

    /**
     * Upload - artifact upload error reason.
     */
    public static final String UPLOAD_ERROR_REASON = "upload-error-reason";

    /**
     * Upload - artifact upload - Software module add button.
     */
    public static final String SW_MODULE_ADD_BUTTON = "sw.module.add.button";

    /**
     * Upload - artifact upload - Software module type combo id.
     */
    public static final String SW_MODULE_TYPE = "sw.module.select.type";

    /**
     * Upload - artifact upload - Software module description.
     */
    public static final String ADD_SW_MODULE_DESCRIPTION = "sw.module.description";

    /**
     * ID-Targ.Detail.icon.
     */
    public static final String SW_TABLE_ATRTIFACT_DETAILS_ICON = "swmodule.artifact.details.icon";

    /**
     * Artifact upload - sw module edit button id.
     */
    public static final String UPLOAD_SW_MODULE_EDIT_BUTTON = "swmodule.edit.button";

    /**
     * Ds edit button id.
     */
    public static final String DS_EDIT_BUTTON = "ds.edit.button";
    /**
     * Upload Artifact details max table Id.
     */
    public static final String UPLOAD_ARTIFACT_DETAILS_TABLE_MAX = "upload.artifactdetails.table.max";

    /**
     * Target tag close button.
     */
    public static final String HIDE_TARGET_TAGS = "hide.target.tags";

    /**
     * Show target tag layout icon.
     */
    public static final String SHOW_TARGET_TAGS = "show.target.tags.icon";

    /**
     * ID-Target tag table.
     */
    public static final String TARGET_TAG_TABLE_ID = "target.tag.tableId";

    /**
     * ID-Target tag table drop area.
     */
    public static final String TARGET_TAG_DROP_AREA_ID = "target.tag.drop.area";

    /**
     * ID-Distibution tag table.
     */
    public static final String DISTRIBUTION_TAG_TABLE_ID = "distriution.tag.tableId";
    /**
     * ID-Software module type table.
     */
    public static final String SW_MODULE_TYPE_TABLE_ID = "sw.module.type.table";

    /**
     * ID-Target tag table.
     */
    public static final String DISTRIBUTION_SET_TYPE_TABLE_ID = "dist.set.type.tableId";

    /**
     * Tab sheet id.
     */
    public static final String TARGET_DETAILS_TABSHEET = "target.details.tabsheet";

    /**
     * Combobox id.
     */
    public static final String SYSTEM_CONFIGURATION_DEFAULTDIS_COMBOBOX = "default.disset.combobox";

    /**
     * Polling system configuration.
     */
    public static final String SYSTEM_CONFIGURATION_POLLING = "system.configuration.polling";

    /**
     * Overdue system configuration.
     */
    public static final String SYSTEM_CONFIGURATION_OVERDUE = "system.configuration.ovderdue";

    /**
     * Button save id.
     */
    public static final String SYSTEM_CONFIGURATION_SAVE = "system.configuration.save";

    /**
     * ID for save button in pop-up-windows instance of commonDialogWindow
     */
    public static final String SAVE_BUTTON = "common.dialog.window.save";

    /**
     * ID for cancel button in pop-up-windows instance of commonDialogWindow
     */
    public static final String CANCEL_BUTTON = "common.dialog.window.cancel";

    /**
     * Cancel button is.
     */
    public static final String SYSTEM_CONFIGURATION_CANCEL = "system.configuration.cancel";

    /**
     * Id of maximize/minimize icon of table - Software module table.
     */
    public static final String SW_MAX_MIN_TABLE_ICON = "sw.max.min.table.icon";

    /**
     * Id of maximize/minimize icon of table - Distribution table.
     */
    public static final String DS_MAX_MIN_TABLE_ICON = "ds.max.min.table.icon";

    /**
     * Software module table in upload UI.
     */
    public static final String SM_TYPE_FILTER_BTN_ID = "sm.type.filter.btn.";

    /**
     * Target table details header caption id.
     */
    public static final String TARGET_DETAILS_HEADER_LABEL_ID = "target.details.header.caption";

    /**
     * Software module table details header caption id.
     */
    public static final String DISTRIBUTION_DETAILS_HEADER_LABEL_ID = "distribution.details.header.caption";

    /**
     * Software Module table details header caption id.
     */
    public static final String SOFTWARE_MODULE_DETAILS_HEADER_LABEL_ID = "software.module.details.header.caption";

    /**
     * Software module table details vendor label id.
     */
    public static final String DETAILS_VENDOR_LABEL_ID = "details.vendor";

    /**
     * Software module table details description label id.
     */
    public static final String DETAILS_DESCRIPTION_LABEL_ID = "details.description";

    /**
     * Software module table details type label id.
     */
    public static final String DETAILS_TYPE_LABEL_ID = "details.type";

    /**
     * Id of show filter button in software module table.
     */
    public static final String SM_SHOW_FILTER_BUTTON_ID = "show.filter.layout";

    /**
     * Software module table in upload UI.
     */
    public static final String DS_TYPE_FILTER_BTN_ID = "ds.type.filter.btn.";

    /**
     * Id of target table search reset Icon.
     */
    public static final String TARGET_TBL_SEARCH_RESET_ID = "target.search.rest.icon";

    /**
     * Id of the target table add Icon.
     */
    public static final String TARGET_TBL_ADD_ICON_ID = "target.add";

    /**
     * Id of IP address label in target details.
     */
    public static final String TARGET_IP_ADDRESS = "target.ip.address";

    /**
     * Id of Last query date label in target details.
     */
    public static final String TARGET_LAST_QUERY_DT = "target.last.query.date";

    /**
     * Id of Controller Id label in target details.
     */
    public static final String TARGET_CONTROLLER_ID = "target.controller.id";

    /**
     * Id of security token label in target details.
     */
    public static final String TARGET_SECURITY_TOKEN = "target.security.token";

    /**
     * Id of maximize/minimize icon of table - Software module table.
     */
    public static final String TARGET_MAX_MIN_TABLE_ICON = "target.max.min.table.icon";

    /**
     * Software module table in upload UI.
     */
    public static final String SWM_DTLS_MAX_ASSIGN = "max.assign";

    /**
     * Documentation Link in Login view and menu.
     */
    public static final String LINK_DOCUMENTATION = "link.documentation";

    /**
     * Demo Link in Login view and menu.
     */
    public static final String LINK_DEMO = "link.demo";

    /**
     * Request account Link in Login view and menu.
     */
    public static final String LINK_REQUESTACCOUNT = "link.requestaccount";

    /**
     * Support Link in Login view and menu.
     */
    public static final String LINK_SUPPORT = "link.support";

    /**
     * User management Link in Login view and menu.
     */
    public static final String LINK_USERMANAGEMENT = "link.usermanagement";

    /**
     * New Target tag add icon id.
     */
    public static final String ADD_TARGET_TAG = "target.tag.add";

    /**
     * New distribution set tag add icon id.
     */
    public static final String ADD_DISTRIBUTION_TAG = "distribution.tag.add";
    /**
     * Bulk target upload - distribution set combo.
     */
    public static final String BULK_UPLOAD_DS_COMBO = "bulkupload.ds.combo";

    /**
     * Bulk target upload - description.
     */
    public static final String BULK_UPLOAD_DESC = "bulkupload.description";
    /**
     * Bulk target upload - tag field.
     */
    public static final String BULK_UPLOAD_TAG = "bulkupload.tag";
    /**
     * Bulk target upload - count label.
     */
    public static final String BULK_UPLOAD_COUNT = "bulkupload.upload.count";

    /**
     * Id of the target table bulk upload Icon.
     */
    public static final String TARGET_TBL_BULK_UPLOAD_ICON_ID = "target.bulk.upload";

    /**
     * Id of target filter table search reset Icon.
     */
    public static final String TARGET_FILTER_TBL_SEARCH_RESET_ID = "target.filter.search.rest.icon";

    /**
     * ID - simple filter- Accordion-Tab
     */
    public static final String SIMPLE_FILTER_ACCORDION_TAB = "simple.filter.accordion.tab";

    /**
     * ID - custom filter- Accordion-Tab
     */
    public static final String CUSTOM_FILTER_ACCORDION_TAB = "custom.filter.accordion.tab";

    /**
     * ID- Customfilter.Name.
     */
    public static final String CUSTOM_FILTER_ADD_NAME = "custom.filter.add.name";

    /**
     * custom filter - delete button id.
     */
    public static final String CUSTOM_FILTER_DELETE_ICON = "custom.filter.delete.button";

    /**
     * custom filter - update button id.
     */
    public static final String CUSTOM_FILTER_DETAIL_LINK = "custom.filter.detail.link";

    /**
     * custom filter - save button id.
     */
    public static final String CUSTOM_FILTER_SAVE_ICON = "custom.filter.save.button.Id";

    /**
     * ID-Custom target tag table.
     */
    public static final String CUSTOM_TARGET_TAG_TABLE_ID = "custom.target.tag.tableId";

    /**
     * ID for closing custom filter
     */
    public static final String CUSTOM_FILTER_CLOSE = "create.custom.filter.close.Id";

    /**
     * ID for custom filter query text
     */
    public static final String CUSTOM_FILTER_QUERY = "custom.query.text.Id";

    /**
     * Target filter table id.
     */
    public static final String TARGET_FILTER_TABLE_ID = "target.query.filter.table.Id";

    /**
     * create or update target filter query - name label id.
     */
    public static final String TARGET_FILTER_QUERY_NAME_LABEL_ID = "target.filter.name.label.id";

    /**
     * create or update target filter query - name layout id.
     */
    public static final String TARGET_FILTER_QUERY_NAME_LAYOUT_ID = "target.filter.name.layout.id";
    /**
     * * create or update target filter query - target table id.
     */
    public static final String CUSTOM_FILTER_TARGET_TABLE_ID = "custom.filter.target.table.id";

    /**
     * Bulk upload notification button id.
     */
    public static final String BULK_UPLOAD_STATUS_BUTTON = "bulk.upload.notification.id";

    /**
     * Target bulk upload minimize button id.
     */
    public static final String BULK_UPLOAD_MINIMIZE_BUTTON_ID = "bulk.upload.minimize.button.id";

    /**
     * Target bulk upload minimize button id.
     */

    public static final String BULK_UPLOAD_CLOSE_BUTTON_ID = "bulk.upload.close.button.id";

    /**
     * Rollout list view - search box id.
     */
    public static final String ROLLOUT_LIST_SEARCH_BOX_ID = "rollout.list.search.id";

    /**
     * Rollout list view - search reset icon id.
     */
    public static final String ROLLOUT_LIST_SEARCH_RESET_ICON_ID = "rollout.list.search.reset.icon.id";

    /**
     * Rollout list view - add icon id.
     */
    public static final String ROLLOUT_ADD_ICON_ID = "rollout.add.button.id";

    /**
     * Rollout list grid id.
     */
    public static final String ROLLOUT_LIST_GRID_ID = "rollout.grid.id";

    /**
     * Rollout group list grid id.
     */
    public static final String ROLLOUT_GROUP_LIST_GRID_ID = "rollout.group.grid.id";

    /**
     * Rollout group list grid id.
     */
    public static final String ROLLOUT_GROUP_TARGETS_LIST_GRID_ID = "rollout.group.targets.grid.id";

    /**
     * Rollout text field name id.
     */
    public static final String ROLLOUT_NAME_FIELD_ID = "rollout.name.field.id";
    /**
     * Rollout number of groups id.
     */
    public static final String ROLLOUT_NO_OF_GROUPS_ID = "rollout.no.ofgroups.id";

    /**
     * Rollout trigger threshold field if.
     */
    public static final String ROLLOUT_TRIGGER_THRESOLD_ID = "rollout.trigger.thresold.id";

    /**
     * Rollout error thresold field id.
     */
    public static final String ROLLOUT_ERROR_THRESOLD_ID = "rollout.error.thresold.id";

    /**
     * Rollout group target percentage id.
     */
    public static final String ROLLOUT_GROUP_TARGET_PERC_ID = "rollout.group.target.percentage.id";

    /**
     * Rollout group add button id
     */
    public static final String ROLLOUT_GROUP_ADD_ID = "rollout.group.add.id";

    /**
     * Rollout group remove button id
     */
    public static final String ROLLOUT_GROUP_REMOVE_ID = "rollout.group.remove.id";

    /**
     * Rollout distribution set combo id.
     */
    public static final String ROLLOUT_DS_ID = "rollout.ds.id";
    /**
     * Rollout description field id.
     */
    public static final String ROLLOUT_DESCRIPTION_ID = "rollout.description.id";
    /**
     * Rollout target filter query combo id.
     */
    public static final String ROLLOUT_TARGET_FILTER_COMBO_ID = "rollout.target.filter.combo.id";
    /**
     * Rollout groups id
     */
    public static final String ROLLOUT_GROUPS = "rollout.groups";
    /**
     * Rollout simple tab id
     */
    public static final String ROLLOUT_SIMPLE_TAB = "rollout.create.tabs.simple";
    /**
     * Rollout advanced tab id
     */
    public static final String ROLLOUT_ADVANCED_TAB = "rollout.create.tabs.advanced";
    /**
     * Rollout action button id.
     */
    public static final String ROLLOUT_ACTION_ID = "rollout.action.button.id";

    /**
     * Rollout start button id.
     */
    public static final String ROLLOUT_RUN_BUTTON_ID = ROLLOUT_ACTION_ID + ".6";

    /**
     * Rollout pause button id.
     */
    public static final String ROLLOUT_PAUSE_BUTTON_ID = ROLLOUT_ACTION_ID + ".7";

    /**
     * Rollout update button id.
     */
    public static final String ROLLOUT_UPDATE_BUTTON_ID = ROLLOUT_ACTION_ID + ".8";

    /**
     * Rollout copy button id.
     */
    public static final String ROLLOUT_COPY_BUTTON_ID = ROLLOUT_ACTION_ID + ".9";

    /**
     * Rollout delete button id.
     */
    public static final String ROLLOUT_DELETE_BUTTON_ID = ROLLOUT_ACTION_ID + ".10";

    /**
     * Rollout status label id.
     */
    public static final String ROLLOUT_STATUS_LABEL_ID = "rollout.status.id";

    /**
     * Rollout group status label id.
     */
    public static final String ROLLOUT_GROUP_STATUS_LABEL_ID = "rollout.group.status.id";

    /**
     * Rollout % or count option group id.
     */
    public static final String ROLLOUT_ERROR_THRESOLD_OPTION_ID = "rollout.error.thresold.option.id";

    /**
     * Rollout target filter query value text area id.
     */
    public static final String ROLLOUT_TARGET_FILTER_QUERY_FIELD = "rollout.target.filter.query.field.id";

    /**
     * Rollout target view- close button id.
     */
    public static final String ROLLOUT_TARGET_VIEW_CLOSE_BUTTON_ID = "rollout.group.target.close.id";
    /**
     * Rollout group header caption.
     */
    public static final String ROLLOUT_GROUP_HEADER_CAPTION = "rollout.group.header.caption";
    /**
     * Rollout group close id.
     */
    public static final String ROLLOUT_GROUP_CLOSE = "rollout.group.close.id";
    /**
     * Rollout group targets count message label.
     */
    public static final String ROLLOUT_GROUP_TARGET_LABEL = "rollout.group.target.label";

    /**
     * ID for rollout progress bar
     */
    public static final String ROLLOUT_PROGRESS_BAR = "rollout.status.progress.bar.id";

    /**
     * Action confirmation popup id.
     */
    public static final String CONFIRMATION_POPUP_ID = "action.confirmation.popup.id";

    /**
     * Validation status icon .
     */
    public static final String VALIDATION_STATUS_ICON_ID = "validation.status.icon";

    /**
     * Artifact upload status popup - minimize button id.
     */
    public static final String UPLOAD_STATUS_POPUP_MINIMIZE_BUTTON_ID = "artifact.upload.minimize.button.id";

    /**
     * Artifact upload status popup - close button id.
     */
    public static final String UPLOAD_STATUS_POPUP_CLOSE_BUTTON_ID = "artifact.upload.close.button.id";

    /**
     * Artifact upload status popup - resize button id.
     */
    public static final String UPLOAD_STATUS_POPUP_RESIZE_BUTTON_ID = "artifact.upload.resize.button.id";

    /**
     * Artifact upload view - upload status button id.
     */
    public static final String UPLOAD_STATUS_BUTTON = "artficat.upload.status.button.id";

    /**
     * Artifact uplaod view - uplod status popup id.
     */
    public static final String UPLOAD_STATUS_POPUP_ID = "artifact.upload.status.popup.id";

    /**
     * Software module table - Manage metadata id.
     */
    public static final String SW_TABLE_MANAGE_METADATA_ID = "swtable.manage.metadata.id";

    /**
     * Metadata key id.
     */
    public static final String METADATA_KEY_FIELD_ID = "metadata.key.id";

    /**
     * Metadata value id.
     */
    public static final String METADATA_VALUE_ID = "metadata.value.id";

    /**
     * Metadata target visible checkbox id.
     */
    public static final String METADATA_TARGET_VISIBLE_ID = "metadata.targetvisible.id";

    /**
     * Metadata add icon id.
     */
    public static final String METADTA_ADD_ICON_ID = "metadata.add.icon.id";
    /**
     * Metadata table id.
     */
    public static final String METDATA_TABLE_ID = "metadata.table.id";

    /**
     * Distribution set table - Manage metadata id.
     */
    public static final String DS_TABLE_MANAGE_METADATA_ID = "dstable.manage.metadata.id";

    /**
     * DistributionSet - Metadata button id.
     */
    public static final String DS_METADATA_DETAIL_LINK = "distributionset.metadata.detail.link";

    /**
     * Metadata popup id.
     */
    public static final String METADATA_POPUP_ID = "metadata.popup.id";

    /**
     * DistributionSet table details tab id in Distributions .
     */
    public static final String DISTRIBUTIONSET_DETAILS_TABSHEET_ID = "distributionset.details.tabsheet";

    /**
     * Software module table details tab id in Distributions .
     */
    public static final String DIST_SW_MODULE_DETAILS_TABSHEET_ID = "dist.sw.module.details.tabsheet";

    /**
     * Software Module - Metadata button id.
     */
    public static final String SW_METADATA_DETAIL_LINK = "softwaremodule.metadata.detail.link";

    /**
     * Table multiselect for selecting DistType
     */
    public static final String SELECT_DIST_TYPE = "select-dist-type";
    /**
     * ID for download anonymous checkbox
     */
    public static final String DOWNLOAD_ANONYMOUS_CHECKBOX = "downloadanonymouscheckbox";
    /**
     * Id of custom filter query search Icon.
     */
    public static final String FILTER_SEARCH_ICON_ID = "filter.search.icon";

    /**
     * Distribution set select table id
     */
    public static final String DIST_SET_SELECT_TABLE_ID = "distribution.set.select.table";

    /**
     * Distribution set select window id
     */
    public static final String DIST_SET_SELECT_WINDOW_ID = "distribution.set.select.window";

    /**
     * Distribution set select consequences window id
     */
    public static final String DIST_SET_SELECT_CONS_WINDOW_ID = "distribution.set.select.consequences.window";

    public static final String DIST_SET_SELECT_ENABLE_ID = "distribution.set.select.enable";
    /**
     * Id of the unread notification button
     */
    public static final String NOTIFICATION_UNREAD_ID = "notification.unread";

    /**
     * Id of the unread notification popup
     */
    public static final String NOTIFICATION_UNREAD_POPUP_ID = "notification.unread.popup";

    /**
     * Id of the unread notification icon in the menu
     */
    public static final String NOTIFICATION_MENU_ID = "notification.menu.";

    /**
     * Id of the rollout deletion confirmation window
     */
    public static final String ROLLOUT_DELETE_CONFIRMATION_DIALOG = "rollout.delete.confirmation.window";

    /**
     * Details header caption id of the Artifacts Table
     */
    public static final String ARTIFACT_DETAILS_HEADER_LABEL_ID = "artifact.details.header.caption";

    /**
     * Configuration checkbox for
     * {@link TenantConfigurationKey#REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED}.
     */
    public static final String REPOSITORY_ACTIONS_AUTOCLOSE_CHECKBOX = "repositoryactionsautoclosecheckbox";

    /**
     * /* Private Constructor.
     */
    private UIComponentIdProvider() {

    }
}
