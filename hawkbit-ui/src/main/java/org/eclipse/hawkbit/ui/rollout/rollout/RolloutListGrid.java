/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.rollout.rollout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.hawkbit.eventbus.event.RolloutChangeEvent;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rollout.RolloutStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.ui.common.grid.AbstractGrid;
import org.eclipse.hawkbit.ui.customrenderers.client.renderers.CustomObject;
import org.eclipse.hawkbit.ui.customrenderers.renderers.CustomObjectRenderer;
import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlButtonRenderer;
import org.eclipse.hawkbit.ui.customrenderers.renderers.HtmlLabelRenderer;
import org.eclipse.hawkbit.ui.rollout.DistributionBarHelper;
import org.eclipse.hawkbit.ui.rollout.StatusFontIcon;
import org.eclipse.hawkbit.ui.rollout.event.RolloutEvent;
import org.eclipse.hawkbit.ui.rollout.state.RolloutUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.peter.contextmenu.ContextMenu;
import org.vaadin.peter.contextmenu.ContextMenu.ContextMenuItem;
import org.vaadin.peter.contextmenu.ContextMenu.ContextMenuItemClickEvent;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.data.util.PropertyValueGenerator;
import com.vaadin.data.util.converter.Converter;
import com.vaadin.server.AbstractClientConnector;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;
import com.vaadin.ui.renderers.HtmlRenderer;

/**
 * 
 * Rollout list grid component.
 *
 */
@SpringComponent
@ViewScope
public class RolloutListGrid extends AbstractGrid {

	private static final long serialVersionUID = 4060904914954370524L;

	private static final String UPDATE_OPTION = "Update";

	private static final String RESUME_OPTION = "Resume";

	private static final String PAUSE_OPTION = "Pause";

	private static final String START_OPTION = "Start";

	private static final String customObject = "customObject";

	@Autowired
	private transient RolloutManagement rolloutManagement;

	@Autowired
	private AddUpdateRolloutWindowLayout addUpdateRolloutWindow;

	@Autowired
	private UINotification uiNotification;

	@Autowired
	private transient RolloutUIState rolloutUIState;

	@Autowired
	private transient SpPermissionChecker permissionChecker;

	private transient Map<RolloutStatus, StatusFontIcon> statusIconMap = new EnumMap<>(RolloutStatus.class);

	@EventBusListenerMethod(scope = EventScope.SESSION)
	void onEvent(final RolloutEvent event) {
		switch (event) {
		case FILTER_BY_TEXT:
		case CREATE_ROLLOUT:
		case UPDATE_ROLLOUT:
		case SHOW_ROLLOUTS:
			refreshGrid();
			break;
		default:
			return;
		}
	}

	/**
	 * Handles the RolloutChangeEvent to refresh the item in the grid.
	 * 
	 * @param rolloutChangeEvent
	 *            the event which contains the rollout which has been changed
	 */
	@SuppressWarnings("unchecked")
	@EventBusListenerMethod(scope = EventScope.SESSION)
	public void onEvent(final RolloutChangeEvent rolloutChangeEvent) {
		if (!rolloutUIState.isShowRollOuts()) {
			return;
		}
		final Rollout rollout = rolloutManagement.findRolloutWithDetailedStatus(rolloutChangeEvent.getRolloutId());
		final TotalTargetCountStatus totalTargetCountStatus = rollout.getTotalTargetCountStatus();
		final LazyQueryContainer rolloutContainer = (LazyQueryContainer) getContainerDataSource();
		final Item item = rolloutContainer.getItem(rolloutChangeEvent.getRolloutId());
		if (item == null) {
			return;
		}
		item.getItemProperty(SPUILabelDefinitions.VAR_STATUS).setValue(rollout.getStatus());
		item.getItemProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS).setValue(totalTargetCountStatus);
		final Long groupCount = (Long) item.getItemProperty(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).getValue();
		final int groupsCreated = rollout.getRolloutGroupsCreated();
		if (groupsCreated != 0) {
			item.getItemProperty(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).setValue(Long.valueOf(groupsCreated));
		} else if (rollout.getRolloutGroups() != null && groupCount != rollout.getRolloutGroups().size()) {
			item.getItemProperty(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS)
					.setValue(Long.valueOf(rollout.getRolloutGroups().size()));
		}
		item.getItemProperty(customObject)
				.setValue(new CustomObject(rollout.getName(), rollout.getStatus().toString()));

	}

	@Override
	protected Container createContainer() {
		final BeanQueryFactory<RolloutBeanQuery> rolloutQf = new BeanQueryFactory<>(RolloutBeanQuery.class);
		return new LazyQueryContainer(
				new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, SPUILabelDefinitions.VAR_ID), rolloutQf);
	}

	@Override
	protected void addContainerProperties() {
		final LazyQueryContainer rolloutGridContainer = (LazyQueryContainer) getContainerDataSource();
		rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, "", false, false);
		rolloutGridContainer.addContainerProperty(customObject, CustomObject.class, null, false, false);
		rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, null, false, false);
		rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_STATUS, RolloutStatus.class, null, false,
				false);
		rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_DIST_NAME_VERSION, String.class, null, false,
				false);
		rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, String.class, null, false,
				false);

		rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_DATE, String.class, null, false,
				false);
		rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_USER, String.class, null, false,
				false);
		rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_MODIFIED_BY, String.class, null, false,
				false);
		rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS, Long.class, 0, false,
				false);
		rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS, String.class, "0", false,
				false);
		rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS,
				TotalTargetCountStatus.class, null, false, false);

		rolloutGridContainer.addContainerProperty(SPUILabelDefinitions.ACTION, String.class,
				FontAwesome.CIRCLE_O.getHtml(), false, false);

	}

	@Override
	protected void setColumnExpandRatio() {

		getColumn(customObject).setMinimumWidth(40);
		getColumn(customObject).setMaximumWidth(150);

		getColumn(SPUILabelDefinitions.VAR_DIST_NAME_VERSION).setMinimumWidth(40);
		getColumn(SPUILabelDefinitions.VAR_DIST_NAME_VERSION).setMaximumWidth(150);

		getColumn(SPUILabelDefinitions.VAR_STATUS).setMinimumWidth(75);
		getColumn(SPUILabelDefinitions.VAR_STATUS).setMaximumWidth(75);

		getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS).setMinimumWidth(40);
		getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS).setMaximumWidth(100);

		getColumn(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).setMinimumWidth(40);
		getColumn(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).setMaximumWidth(100);

		getColumn(SPUILabelDefinitions.ACTION).setMinimumWidth(75);
		getColumn(SPUILabelDefinitions.ACTION).setMaximumWidth(75);

		getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS).setMinimumWidth(280);

		setFrozenColumnCount(getColumns().size());
	}

	@Override
	protected void setColumnHeaderNames() {
		getColumn(customObject).setHeaderCaption(i18n.get("header.name"));
		getColumn(SPUILabelDefinitions.VAR_DIST_NAME_VERSION).setHeaderCaption(i18n.get("header.distributionset"));
		getColumn(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).setHeaderCaption(i18n.get("header.numberofgroups"));
		getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS).setHeaderCaption(i18n.get("header.total.targets"));
		getColumn(SPUILabelDefinitions.VAR_CREATED_DATE).setHeaderCaption(i18n.get("header.createdDate"));
		getColumn(SPUILabelDefinitions.VAR_CREATED_USER).setHeaderCaption(i18n.get("header.createdBy"));
		getColumn(SPUILabelDefinitions.VAR_MODIFIED_DATE).setHeaderCaption(i18n.get("header.modifiedDate"));
		getColumn(SPUILabelDefinitions.VAR_MODIFIED_BY).setHeaderCaption(i18n.get("header.modifiedBy"));
		getColumn(SPUILabelDefinitions.VAR_DESC).setHeaderCaption(i18n.get("header.description"));
		getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS)
				.setHeaderCaption(i18n.get("header.detail.status"));
		getColumn(SPUILabelDefinitions.VAR_STATUS).setHeaderCaption(i18n.get("header.status"));
		getColumn(SPUILabelDefinitions.ACTION).setHeaderCaption(i18n.get("upload.action"));
	}

	@Override
	protected String getGridId() {
		return SPUIComponetIdProvider.ROLLOUT_LIST_GRID_ID;
	}

	@Override
	protected void setColumnProperties() {
		final List<Object> columnList = new ArrayList<>();
		columnList.add(customObject);
		columnList.add(SPUILabelDefinitions.VAR_DIST_NAME_VERSION);
		columnList.add(SPUILabelDefinitions.VAR_STATUS);
		columnList.add(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS);
		columnList.add(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS);
		columnList.add(SPUILabelDefinitions.VAR_TOTAL_TARGETS);
		columnList.add(SPUILabelDefinitions.ACTION);

		columnList.add(SPUILabelDefinitions.VAR_CREATED_DATE);
		columnList.add(SPUILabelDefinitions.VAR_CREATED_USER);
		columnList.add(SPUILabelDefinitions.VAR_MODIFIED_DATE);
		columnList.add(SPUILabelDefinitions.VAR_MODIFIED_BY);
		columnList.add(SPUILabelDefinitions.VAR_DESC);
		setColumnOrder(columnList.toArray());
		alignColumns();
	}

	@Override
	protected void setHiddenColumns() {
		final List<Object> columnsToBeHidden = new ArrayList<>();
		columnsToBeHidden.add(SPUILabelDefinitions.VAR_NAME);
		columnsToBeHidden.add(SPUILabelDefinitions.VAR_CREATED_DATE);
		columnsToBeHidden.add(SPUILabelDefinitions.VAR_CREATED_USER);
		columnsToBeHidden.add(SPUILabelDefinitions.VAR_MODIFIED_DATE);
		columnsToBeHidden.add(SPUILabelDefinitions.VAR_MODIFIED_BY);
		columnsToBeHidden.add(SPUILabelDefinitions.VAR_DESC);
		for (final Object propertyId : columnsToBeHidden) {
			getColumn(propertyId).setHidden(true);
		}

	}

	@Override
	protected CellDescriptionGenerator getDescriptionGenerator() {
		return cell -> getDescription(cell);
	}

	@Override
	protected void addColumnRenderes() {
		getColumn(SPUILabelDefinitions.VAR_NUMBER_OF_GROUPS).setRenderer(new HtmlRenderer(),
				new TotalTargetGroupsConverter());
		getColumn(SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS).setRenderer(new HtmlRenderer(),
				new TotalTargetCountStatusConverter());

		createRolloutStatusToFontMap();
		getColumn(SPUILabelDefinitions.VAR_STATUS).setRenderer(new HtmlLabelRenderer(), new RolloutStatusConverter());

		getColumn(SPUILabelDefinitions.ACTION).setRenderer(new HtmlButtonRenderer(event -> onClickOfActionBtn(event)));

		CustomObjectRenderer customObjectRenderer = new CustomObjectRenderer(CustomObject.class);
		customObjectRenderer.addClickListener(event -> onClickOfRolloutName(event));
		getColumn(customObject).setRenderer(customObjectRenderer);

	}

	private void createRolloutStatusToFontMap() {
		statusIconMap.put(RolloutStatus.FINISHED,
				new StatusFontIcon(FontAwesome.CHECK_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_GREEN));
		statusIconMap.put(RolloutStatus.PAUSED,
				new StatusFontIcon(FontAwesome.PAUSE, SPUIStyleDefinitions.STATUS_ICON_BLUE));
		statusIconMap.put(RolloutStatus.RUNNING, new StatusFontIcon(null, SPUIStyleDefinitions.STATUS_SPINNER_YELLOW));
		statusIconMap.put(RolloutStatus.READY,
				new StatusFontIcon(FontAwesome.DOT_CIRCLE_O, SPUIStyleDefinitions.STATUS_ICON_LIGHT_BLUE));
		statusIconMap.put(RolloutStatus.STOPPED,
				new StatusFontIcon(FontAwesome.STOP, SPUIStyleDefinitions.STATUS_ICON_RED));
		statusIconMap.put(RolloutStatus.CREATING, new StatusFontIcon(null, SPUIStyleDefinitions.STATUS_SPINNER_GREY));
		statusIconMap.put(RolloutStatus.STARTING, new StatusFontIcon(null, SPUIStyleDefinitions.STATUS_SPINNER_BLUE));
		statusIconMap.put(RolloutStatus.ERROR_CREATING,
				new StatusFontIcon(FontAwesome.EXCLAMATION_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_RED));
		statusIconMap.put(RolloutStatus.ERROR_STARTING,
				new StatusFontIcon(FontAwesome.EXCLAMATION_CIRCLE, SPUIStyleDefinitions.STATUS_ICON_RED));
	}

	private void alignColumns() {
		setCellStyleGenerator(new CellStyleGenerator() {
			private static final long serialVersionUID = 5573570647129792429L;

			@Override
			public String getStyle(final CellReference cellReference) {
				final String[] coulmnNames = { SPUILabelDefinitions.VAR_STATUS, SPUILabelDefinitions.ACTION };
				if (Arrays.asList(coulmnNames).contains(cellReference.getPropertyId())) {
					return "centeralign";
				}
				return null;
			}
		});
	}

	private void onClickOfRolloutName(final RendererClickEvent event) {
		rolloutUIState.setRolloutId((long) event.getItemId());
		final String rolloutName = (String) getContainerDataSource().getItem(event.getItemId())
				.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
		rolloutUIState.setRolloutName(rolloutName);
		final String ds = (String) getContainerDataSource().getItem(event.getItemId())
				.getItemProperty(SPUILabelDefinitions.VAR_DIST_NAME_VERSION).getValue();
		rolloutUIState.setRolloutDistributionSet(ds);
		eventBus.publish(this, RolloutEvent.SHOW_ROLLOUT_GROUPS);
	}

	private void onClickOfActionBtn(final RendererClickEvent event) {
		final ContextMenu contextMenu = createContextMenu((Long) event.getItemId());
		contextMenu.setAsContextMenuOf((AbstractClientConnector) event.getComponent());
		contextMenu.open(event.getClientX(), event.getClientY());
	}

	private ContextMenu createContextMenu(final Long rolloutId) {
		final ContextMenu context = new ContextMenu();
		context.addItemClickListener(event -> menuItemClicked(event));
		final Item row = getContainerDataSource().getItem(rolloutId);
		final RolloutStatus rolloutStatus = (RolloutStatus) row.getItemProperty(SPUILabelDefinitions.VAR_STATUS)
				.getValue();

		switch (rolloutStatus) {
		case READY:
			final ContextMenuItem startItem = context.addItem(START_OPTION);
			startItem.setData(new ContextMenuData(rolloutId, ACTION.START));
			break;
		case RUNNING:
			final ContextMenuItem pauseItem = context.addItem(PAUSE_OPTION);
			pauseItem.setData(new ContextMenuData(rolloutId, ACTION.PAUSE));
			break;
		case PAUSED:
			final ContextMenuItem resumeItem = context.addItem(RESUME_OPTION);
			resumeItem.setData(new ContextMenuData(rolloutId, ACTION.RESUME));
			break;
		case STARTING:
		case CREATING:
		case ERROR_CREATING:
		case ERROR_STARTING:
			// do not provide any action on these statuses
			return context;
		default:
			break;
		}
		getUpdateMenuItem(context, rolloutId);
		return context;
	}

	private void getUpdateMenuItem(final ContextMenu context, final Long rolloutId) {
		// Add 'Update' option only if user has update permission
		if (!permissionChecker.hasRolloutUpdatePermission()) {
			return;
		}
		final ContextMenuItem cancelItem = context.addItem(UPDATE_OPTION);
		cancelItem.setData(new ContextMenuData(rolloutId, ACTION.UPDATE));
	}

	private void menuItemClicked(final ContextMenuItemClickEvent event) {
		final ContextMenuItem item = (ContextMenuItem) event.getSource();
		final ContextMenuData contextMenuData = (ContextMenuData) item.getData();
		final Item row = getContainerDataSource().getItem(contextMenuData.getRolloutId());
		final String rolloutName = (String) row.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
		switch (contextMenuData.getAction()) {
		case PAUSE:
			rolloutManagement.pauseRollout(rolloutManagement.findRolloutById(contextMenuData.getRolloutId()));
			uiNotification.displaySuccess(i18n.get("message.rollout.paused", rolloutName));
			break;
		case RESUME:
			rolloutManagement.resumeRollout(rolloutManagement.findRolloutById(contextMenuData.getRolloutId()));
			uiNotification.displaySuccess(i18n.get("message.rollout.resumed", rolloutName));
			break;
		case START:
			rolloutManagement.startRolloutAsync(rolloutManagement.findRolloutByName(rolloutName));
			uiNotification.displaySuccess(i18n.get("message.rollout.started", rolloutName));
			break;
		case UPDATE:
			onUpdate(contextMenuData);
			break;
		default:
			break;
		}
	}

	private void onUpdate(final ContextMenuData contextMenuData) {
		addUpdateRolloutWindow.populateData(contextMenuData.getRolloutId());
		final Window addTargetWindow = addUpdateRolloutWindow.getWindow();
		addTargetWindow.setCaption(i18n.get("caption.update.rollout"));
		UI.getCurrent().addWindow(addTargetWindow);
		addTargetWindow.setVisible(Boolean.TRUE);
	}

	private void refreshGrid() {
		((LazyQueryContainer) getContainerDataSource()).refresh();
	}

	public final class FontIconGenerator extends PropertyValueGenerator<String> {

		private static final long serialVersionUID = 2544026030795375748L;
		private final FontAwesome fontIcon;

		public FontIconGenerator(final FontAwesome icon) {
			this.fontIcon = icon;
		}

		@Override
		public String getValue(final Item item, final Object itemId, final Object propertyId) {
			return fontIcon.getHtml();
		}

		@Override
		public Class<String> getType() {
			return String.class;
		}
	}

	private String getDescription(final CellReference cell) {
		if (SPUILabelDefinitions.VAR_STATUS.equals(cell.getPropertyId())) {
			return cell.getProperty().getValue().toString().toLowerCase();
		} else if (SPUILabelDefinitions.ACTION.equals(cell.getPropertyId())) {
			return SPUILabelDefinitions.ACTION.toLowerCase();
		} else if (customObject.equals(cell.getPropertyId())) {
			return ((CustomObject) cell.getProperty().getValue()).getName();
		} else if (SPUILabelDefinitions.VAR_TOTAL_TARGETS_COUNT_STATUS.equals(cell.getPropertyId())) {
			return DistributionBarHelper
					.getTooltip(((TotalTargetCountStatus) cell.getValue()).getStatusTotalCountMap());
		}
		return null;
	}

	enum ACTION {
		PAUSE, RESUME, START, UPDATE
	}

	/**
	 * Represents data of context menu item.
	 *
	 */
	public static class ContextMenuData {

		private Long rolloutId;

		private ACTION action;

		/**
		 * Set rollout if and action.
		 * 
		 * @param rolloutId
		 *            id of rollout
		 * @param action
		 *            user action {@link ACTION}
		 */
		public ContextMenuData(final Long rolloutId, final ACTION action) {
			this.action = action;
			this.rolloutId = rolloutId;
		}

		/**
		 * @return the rolloutId
		 */
		public Long getRolloutId() {
			return rolloutId;
		}

		/**
		 * @param rolloutId
		 *            the rolloutId to set
		 */
		public void setRolloutId(final Long rolloutId) {
			this.rolloutId = rolloutId;
		}

		/**
		 * @return the action
		 */
		public ACTION getAction() {
			return action;
		}

		/**
		 * @param action
		 *            the action to set
		 */
		public void setAction(final ACTION action) {
			this.action = action;
		}
	}

	/**
	 * 
	 * Converter to convert {@link RolloutStatus} to string.
	 *
	 */
	class RolloutStatusConverter implements Converter<String, RolloutStatus> {

		private static final long serialVersionUID = -1217685750825632678L;

		@Override
		public RolloutStatus convertToModel(final String value, final Class<? extends RolloutStatus> targetType,
				final Locale locale) {
			return null;
		}

		@Override
		public String convertToPresentation(final RolloutStatus value, final Class<? extends String> targetType,
				final Locale locale) {
			return convertRolloutStatusToString(value);
		}

		@Override
		public Class<RolloutStatus> getModelType() {
			return RolloutStatus.class;
		}

		@Override
		public Class<String> getPresentationType() {
			return String.class;
		}

		private String convertRolloutStatusToString(final RolloutStatus value) {
			final StatusFontIcon statusFontIcon = statusIconMap.get(value);
			final String codePoint = HawkbitCommonUtil.getCodePoint(statusFontIcon);
			return HawkbitCommonUtil.getStatusLabelDetailsInString(codePoint, statusFontIcon.getStyle(),
					SPUIComponetIdProvider.ROLLOUT_STATUS_LABEL_ID);
		}
	}

	/**
	 * Converter to convert {@link TotalTargetCountStatus} to formatted string
	 * with status and count details.
	 *
	 */
	class TotalTargetCountStatusConverter implements Converter<String, TotalTargetCountStatus> {

		private static final long serialVersionUID = -5794528427855153924L;

		@Override
		public TotalTargetCountStatus convertToModel(final String value,
				final Class<? extends TotalTargetCountStatus> targetType, final Locale locale)
				throws com.vaadin.data.util.converter.Converter.ConversionException {
			return null;
		}

		@Override
		public String convertToPresentation(final TotalTargetCountStatus value,
				final Class<? extends String> targetType, final Locale locale)
				throws com.vaadin.data.util.converter.Converter.ConversionException {
			return DistributionBarHelper.getDistributionBarAsHTMLString(value.getStatusTotalCountMap());
		}

		@Override
		public Class<TotalTargetCountStatus> getModelType() {
			return TotalTargetCountStatus.class;
		}

		@Override
		public Class<String> getPresentationType() {
			return String.class;
		}
	}

	/**
	 * 
	 * Converter to convert 0 to empty , if total target groups is zero.
	 *
	 */

	class TotalTargetGroupsConverter implements Converter<String, Long> {

		private static final long serialVersionUID = 6589305227035220369L;

		@Override
		public Long convertToModel(String value, Class<? extends Long> targetType, Locale locale)
				throws com.vaadin.data.util.converter.Converter.ConversionException {
			return null;
		}

		@Override
		public String convertToPresentation(Long value, Class<? extends String> targetType, Locale locale)
				throws com.vaadin.data.util.converter.Converter.ConversionException {
			if (value == 0) {
				return "";
			}
			return value.toString();
		}

		@Override
		public Class<Long> getModelType() {
			return Long.class;
		}

		@Override
		public Class<String> getPresentationType() {
			return String.class;
		}

	}

}
