/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.details.ArtifactDetailsLayout;
import org.eclipse.hawkbit.ui.artifacts.event.RefreshSoftwareModuleByFilterEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.ManagementEntityState;
import org.eclipse.hawkbit.ui.common.table.AbstractNamedVersionTable;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.dd.criteria.DistributionsViewClientCriterion;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.push.SoftwareModuleUpdatedEventContainer;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.eclipse.hawkbit.ui.view.filter.OnlyEventsFromDistributionsViewFilter;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.google.common.collect.Maps;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Implementation of software module table using generic abstract table styles .
 */
public class SwModuleTable extends AbstractNamedVersionTable<SoftwareModule> {

    private static final long serialVersionUID = 1L;

    private final ManageDistUIState manageDistUIState;

    private final transient SoftwareModuleManagement softwareModuleManagement;

    private final DistributionsViewClientCriterion distributionsViewClientCriterion;

    private final ArtifactDetailsLayout artifactDetailsLayout;

    private final SwMetadataPopupLayout swMetadataPopupLayout;

    SwModuleTable(final UIEventBus eventBus, final VaadinMessageSource i18n, final UINotification uiNotification,
            final ManageDistUIState manageDistUIState, final SoftwareModuleManagement softwareManagement,
            final DistributionsViewClientCriterion distributionsViewClientCriterion,
            final ArtifactManagement artifactManagement, final SwMetadataPopupLayout swMetadataPopupLayout,
            final ArtifactUploadState artifactUploadState) {
        super(eventBus, i18n, uiNotification);
        this.manageDistUIState = manageDistUIState;
        this.softwareModuleManagement = softwareManagement;
        this.distributionsViewClientCriterion = distributionsViewClientCriterion;
        this.artifactDetailsLayout = new ArtifactDetailsLayout(i18n, eventBus, artifactUploadState, uiNotification,
                artifactManagement, softwareManagement);
        this.swMetadataPopupLayout = swMetadataPopupLayout;

        addNewContainerDS();
        setColumnProperties();
        setDataAvailable(getContainerDataSource().size() != 0);
        styleTableOnDistSelection();
    }

    @EventBusListenerMethod(scope = EventScope.UI, filter = OnlyEventsFromDistributionsViewFilter.class)
    void onEvent(final RefreshSoftwareModuleByFilterEvent filterEvent) {
        UI.getCurrent().access(() -> {
            refreshFilter();
            styleTableOnDistSelection();
        });
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final DistributionsUIEvent event) {
        UI.getCurrent().access(() -> {
            if (event == DistributionsUIEvent.ORDER_BY_DISTRIBUTION) {
                refreshFilter();
                styleTableOnDistSelection();
            }
        });
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SaveActionWindowEvent event) {
        if (event == SaveActionWindowEvent.DELETE_ALL_SOFWARE) {
            UI.getCurrent().access(this::refreshFilter);
        }
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SoftwareModuleEvent event) {
        onBaseEntityEvent(event);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onSoftwareModuleUpdateEvents(final SoftwareModuleUpdatedEventContainer eventContainer) {
        @SuppressWarnings("unchecked")
        final List<Long> visibleItemIds = (List<Long>) getVisibleItemIds();
        handleSelectedAndUpdatedSoftwareModules(eventContainer.getEvents());
        eventContainer.getEvents().stream().filter(event -> visibleItemIds.contains(event.getEntityId()))
                .filter(Objects::nonNull)
                .forEach(event -> updateSoftwareModuleInTable(event.getEntity()));
    }

    private void handleSelectedAndUpdatedSoftwareModules(final List<SoftwareModuleUpdatedEvent> events) {
        manageDistUIState.getLastSelectedSoftwareModule()
                .ifPresent(lastSelectedModuleId -> events.stream()
                        .filter(event -> lastSelectedModuleId.equals(event.getEntityId()))
                        .filter(Objects::nonNull).findAny()
                        .ifPresent(lastEvent -> eventBus.publish(this,
                                new SoftwareModuleEvent(BaseEntityEventType.SELECTED_ENTITY, lastEvent.getEntity()))));
    }

    private void updateSoftwareModuleInTable(final SoftwareModule editedSm) {
        final Item item = getContainerDataSource().getItem(editedSm.getId());
        updateEntity(editedSm, item);
    }

    @Override
    protected String getTableId() {
        return UIComponentIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE;
    }

    @Override
    protected Container createContainer() {
        final Map<String, Object> queryConfiguration = prepareQueryConfigFilters();

        final BeanQueryFactory<SwModuleBeanQuery> swQF = new BeanQueryFactory<>(SwModuleBeanQuery.class);
        swQF.setQueryConfiguration(queryConfiguration);

        return new LazyQueryContainer(new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, "swId"), swQF);
    }

    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = Maps.newHashMapWithExpectedSize(3);
        manageDistUIState.getSoftwareModuleFilters().getSearchText()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.FILTER_BY_TEXT, value));
        manageDistUIState.getSoftwareModuleFilters().getSoftwareModuleType()
                .ifPresent(type -> queryConfig.put(SPUIDefinitions.BY_SOFTWARE_MODULE_TYPE, type));
        manageDistUIState.getLastSelectedDistribution()
                .ifPresent(id -> queryConfig.put(SPUIDefinitions.ORDER_BY_DISTRIBUTION, id));
        return queryConfig;

    }

    @Override
    protected void addContainerProperties(final Container container) {
        final LazyQueryContainer lazyContainer = (LazyQueryContainer) container;
        lazyContainer.addContainerProperty("nameAndVersion", String.class, null, false, false);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_ID, Long.class, null, false, false);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, "", false, true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_VERSION, String.class, null, false, false);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, null, false, true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_VENDOR, String.class, null, false, true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_BY, String.class, null, false, true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, String.class, null, false, true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_CREATED_DATE, String.class, null, false, true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE, String.class, null, false,
                true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_COLOR, String.class, null, false, true);
        lazyContainer.addContainerProperty(SPUILabelDefinitions.VAR_SOFT_TYPE_ID, Long.class, null, false, true);
    }

    @Override
    protected void addCustomGeneratedColumns() {
        addGeneratedColumn(SPUILabelDefinitions.ARTIFACT_ICON, new ColumnGenerator() {

            private static final long serialVersionUID = 1L;

            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                final HorizontalLayout iconLayout = new HorizontalLayout();
                // add artifactory details popup
                final String nameVersionStr = getNameAndVerion(itemId);
                final Button showArtifactDtlsBtn = createShowArtifactDtlsButton(nameVersionStr);
                final Button manageMetaDataBtn = createManageMetadataButton(nameVersionStr);
                showArtifactDtlsBtn.addClickListener(event -> showArtifactDetailsWindow((Long) itemId, nameVersionStr));
                manageMetaDataBtn.addClickListener(event -> showMetadataDetails((Long) itemId));
                iconLayout.addComponent(showArtifactDtlsBtn);
                iconLayout.addComponent(manageMetaDataBtn);
                return iconLayout;
            }
        });

    }

    @Override
    protected boolean isFirstRowSelectedOnLoad() {
        return manageDistUIState.getSelectedSoftwareModules().isEmpty();
    }

    @Override
    protected Object getItemIdToSelect() {
        return manageDistUIState.getSelectedSoftwareModules().isEmpty() ? null
                : manageDistUIState.getSelectedSoftwareModules();
    }

    @Override
    protected boolean isMaximized() {
        return manageDistUIState.isSwModuleTableMaximized();
    }

    @Override
    protected void publishSelectedEntityEvent(final SoftwareModule selectedLastEntity) {
        eventBus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.SELECTED_ENTITY, selectedLastEntity));
    }

    @Override
    protected void setLastSelectedEntityId(final Long selectedLastEntityId) {
        manageDistUIState.setLastSelectedSoftwareModule(selectedLastEntityId);
    }

    @Override
    protected void setManagementEntityStateValues(final Set<Long> values, final Long lastId) {
        manageDistUIState.setLastSelectedSoftwareModule(lastId);
        manageDistUIState.setSelectedSoftwareModules(values);
    }

    @Override
    protected Optional<SoftwareModule> findEntityByTableValue(final Long lastSelectedId) {
        return softwareModuleManagement.get(lastSelectedId);
    }

    @Override
    protected ManagementEntityState getManagementEntityState() {
        return null;
    }

    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        final List<TableColumn> columnList = super.getTableVisibleColumns();
        if (isMaximized()) {
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_VENDOR, i18n.getMessage("header.vendor"), 0.1F));
        } else {
            columnList.add(new TableColumn(SPUILabelDefinitions.ARTIFACT_ICON, "", 0.1F));
        }
        return columnList;
    }

    @Override
    protected float getColumnNameMinimizedSize() {
        return 0.7F;
    }

    @Override
    protected AcceptCriterion getDropAcceptCriterion() {
        return distributionsViewClientCriterion;
    }

    @Override
    protected boolean isDropValid(final DragAndDropEvent dragEvent) {
        return false;
    }

    private void styleTableOnDistSelection() {
        Page.getCurrent().getJavaScript().execute(HawkbitCommonUtil.getScriptSMHighlightReset());
        setCellStyleGenerator(new Table.CellStyleGenerator() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getStyle(final Table source, final Object itemId, final Object propertyId) {
                return createTableStyle(itemId, propertyId);
            }
        });
    }

    private static String getTableStyle(final Long typeId, final boolean isAssigned, final String color) {
        if (isAssigned) {
            addTypeStyle(typeId, color);
            return "distribution-upload-type-" + typeId;
        }
        return null;
    }

    private static void addTypeStyle(final Long tagId, final String color) {
        final JavaScript javaScript = UI.getCurrent().getPage().getJavaScript();
        UI.getCurrent()
                .access(() -> javaScript.execute(
                        HawkbitCommonUtil.getScriptSMHighlightWithColor(".v-table-row-distribution-upload-type-" + tagId
                                + "{background-color:" + color + " !important;background-image:none !important }")));
    }

    private String createTableStyle(final Object itemId, final Object propertyId) {
        if (null == propertyId) {
            final Item item = getItem(itemId);
            final boolean isAssigned = (boolean) item.getItemProperty(SPUILabelDefinitions.VAR_ASSIGNED).getValue();
            final Long typeId = (Long) item.getItemProperty(SPUILabelDefinitions.VAR_SOFT_TYPE_ID).getValue();
            String color = (String) item.getItemProperty(SPUILabelDefinitions.VAR_COLOR).getValue();
            if (color == null) {
                // In case the color is null apply default green color. eg:- for
                // types ECL OS, ECL
                // Application etc.,
                color = SPUIDefinitions.DEFAULT_COLOR;
            }
            return getTableStyle(typeId, isAssigned, color);
        }
        return null;
    }

    private Button createShowArtifactDtlsButton(final String nameVersionStr) {
        final Button showArtifactDtlsBtn = SPUIComponentProvider.getButton(
                UIComponentIdProvider.SW_TABLE_ATRTIFACT_DETAILS_ICON + "." + nameVersionStr, "", "", null, false,
                FontAwesome.FILE_O, SPUIButtonStyleSmallNoBorder.class);
        showArtifactDtlsBtn.addStyleName(SPUIStyleDefinitions.ARTIFACT_DTLS_ICON);
        showArtifactDtlsBtn.setDescription(i18n.getMessage("tooltip.artifact.icon"));
        return showArtifactDtlsBtn;
    }

    private Button createManageMetadataButton(final String nameVersionStr) {
        final Button manageMetadataBtn = SPUIComponentProvider.getButton(
                UIComponentIdProvider.SW_TABLE_MANAGE_METADATA_ID + "." + nameVersionStr, "", "", null, false,
                FontAwesome.LIST_ALT, SPUIButtonStyleSmallNoBorder.class);
        manageMetadataBtn.addStyleName(SPUIStyleDefinitions.ARTIFACT_DTLS_ICON);
        manageMetadataBtn.setDescription(i18n.getMessage("tooltip.metadata.icon"));
        return manageMetadataBtn;
    }

    private String getNameAndVerion(final Object itemId) {
        final Item item = getItem(itemId);
        final String name = (String) item.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        final String version = (String) item.getItemProperty(SPUILabelDefinitions.VAR_VERSION).getValue();
        return name + "." + version;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void updateEntity(final SoftwareModule baseEntity, final Item item) {
        final String swNameVersion = HawkbitCommonUtil.concatStrings(":", baseEntity.getName(),
                baseEntity.getVersion());
        item.getItemProperty(SPUILabelDefinitions.NAME_VERSION).setValue(swNameVersion);
        item.getItemProperty("swId").setValue(baseEntity.getId());
        item.getItemProperty(SPUILabelDefinitions.VAR_VENDOR).setValue(baseEntity.getVendor());
        item.getItemProperty(SPUILabelDefinitions.VAR_COLOR).setValue(baseEntity.getType().getColour());
        super.updateEntity(baseEntity, item);
    }

    private void showArtifactDetailsWindow(final Long itemId, final String nameVersionStr) {
        final Window artifactDtlsWindow = new Window();
        artifactDtlsWindow.setCaption(HawkbitCommonUtil.getArtifactoryDetailsLabelId(nameVersionStr));
        artifactDtlsWindow.setCaptionAsHtml(true);
        artifactDtlsWindow.setClosable(true);
        artifactDtlsWindow.setResizable(true);
        artifactDtlsWindow.setImmediate(true);
        artifactDtlsWindow.setWindowMode(WindowMode.NORMAL);
        artifactDtlsWindow.setModal(true);
        artifactDtlsWindow.addStyleName(SPUIStyleDefinitions.CONFIRMATION_WINDOW_CAPTION);

        artifactDetailsLayout.setFullWindowMode(false);
        artifactDetailsLayout.populateArtifactDetails(itemId, nameVersionStr);
        /* Now add table to the window */
        artifactDetailsLayout.getArtifactDetailsTable().setWidth(700, Unit.PIXELS);
        artifactDetailsLayout.getArtifactDetailsTable().setHeight(500, Unit.PIXELS);
        artifactDtlsWindow.setContent(artifactDetailsLayout.getArtifactDetailsTable());

        /* Create maximized view of the table */
        artifactDtlsWindow.addWindowModeChangeListener(

                event -> {
                    if (event.getWindowMode() == WindowMode.MAXIMIZED) {
                        artifactDtlsWindow.setSizeFull();
                        artifactDetailsLayout.setFullWindowMode(true);
                        artifactDetailsLayout.createMaxArtifactDetailsTable();
                        artifactDetailsLayout.getMaxArtifactDetailsTable().setWidth(100, Unit.PERCENTAGE);
                        artifactDetailsLayout.getMaxArtifactDetailsTable().setHeight(100, Unit.PERCENTAGE);
                        artifactDtlsWindow.setContent(artifactDetailsLayout.getMaxArtifactDetailsTable());
                    } else {
                        artifactDtlsWindow.setSizeUndefined();
                        artifactDtlsWindow.setContent(artifactDetailsLayout.getArtifactDetailsTable());
                    }
                });
        /* display the window */
        UI.getCurrent().addWindow(artifactDtlsWindow);

    }

    @Override
    protected void setDataAvailable(final boolean available) {
        manageDistUIState.setNoDataAvilableSwModule(!available);

    }

    private void showMetadataDetails(final Long itemId) {
        softwareModuleManagement.get(itemId)
                .ifPresent(swmodule -> UI.getCurrent().addWindow(swMetadataPopupLayout.getWindow(swmodule, null)));
    }

}
