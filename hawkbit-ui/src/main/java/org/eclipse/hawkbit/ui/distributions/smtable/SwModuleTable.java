/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.details.ArtifactDetailsLayout;
import org.eclipse.hawkbit.ui.artifacts.event.SMFilterEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.common.ManagmentEntityState;
import org.eclipse.hawkbit.ui.common.table.AbstractNamedVersionTable;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsViewAcceptCriteria;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.window.WindowMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Implementation of software module table using generic abstract table styles .
 *
 */
@SpringComponent
@ViewScope
public class SwModuleTable extends AbstractNamedVersionTable<SoftwareModule, Long> {

    private static final long serialVersionUID = 6785314784507424750L;

    @Autowired
    private ManageDistUIState manageDistUIState;

    @Autowired
    private transient SoftwareManagement softwareManagement;

    @Autowired
    private DistributionsViewAcceptCriteria distributionsViewAcceptCriteria;

    @Autowired
    private ArtifactDetailsLayout artifactDetailsLayout;
    
    @Autowired
    private SwMetadataPopupLayout swMetadataPopupLayout;

    /**
     * Initialize the filter layout.
     */
    @Override
    protected void init() {
        super.init();
        styleTableOnDistSelection();
    }

    /* All event Listeners */

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SMFilterEvent filterEvent) {
        UI.getCurrent().access(() -> {

            if (filterEvent == SMFilterEvent.FILTER_BY_TYPE || filterEvent == SMFilterEvent.FILTER_BY_TEXT
                    || filterEvent == SMFilterEvent.REMOVER_FILTER_BY_TYPE
                    || filterEvent == SMFilterEvent.REMOVER_FILTER_BY_TEXT) {
                refreshFilter();
                styleTableOnDistSelection();
            }
        });
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final DistributionsUIEvent event) {
        UI.getCurrent().access(() -> {
            if (event == DistributionsUIEvent.ORDER_BY_DISTRIBUTION) {
                refreshFilter();
                styleTableOnDistSelection();
            }
        });
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SaveActionWindowEvent event) {
        if (event == SaveActionWindowEvent.DELETE_ALL_SOFWARE) {
            UI.getCurrent().access(() -> refreshFilter());
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SoftwareModuleEvent event) {
        onBaseEntityEvent(event);
    }

    @Override
    protected String getTableId() {
        return SPUIComponentIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE;
    }

    @Override
    protected Container createContainer() {
        final Map<String, Object> queryConfiguration = prepareQueryConfigFilters();

        final BeanQueryFactory<SwModuleBeanQuery> swQF = new BeanQueryFactory<>(SwModuleBeanQuery.class);
        swQF.setQueryConfiguration(queryConfiguration);

        return new LazyQueryContainer(new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, "swId"), swQF);
    }

    private Map<String, Object> prepareQueryConfigFilters() {
        final Map<String, Object> queryConfig = new HashMap<>();
        manageDistUIState.getSoftwareModuleFilters().getSearchText()
                .ifPresent(value -> queryConfig.put(SPUIDefinitions.FILTER_BY_TEXT, value));

        manageDistUIState.getSoftwareModuleFilters().getSoftwareModuleType()
                .ifPresent(type -> queryConfig.put(SPUIDefinitions.BY_SOFTWARE_MODULE_TYPE, type));

        manageDistUIState.getLastSelectedDistribution()
                .ifPresent(distIdName -> queryConfig.put(SPUIDefinitions.ORDER_BY_DISTRIBUTION, distIdName.getId()));

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

            private static final long serialVersionUID = -5982361782989980277L;

            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                HorizontalLayout iconLayout = new HorizontalLayout();
                
                // add artifactory details popup
                final String nameVersionStr = getNameAndVerion(itemId);
                final Button showArtifactDtlsBtn = createShowArtifactDtlsButton(nameVersionStr);
                final Button manageMetaDataBtn = createManageMetadataButton(nameVersionStr);

                showArtifactDtlsBtn.addClickListener(event -> showArtifactDetailsWindow((Long) itemId, nameVersionStr));
                manageMetaDataBtn.addClickListener(event -> showMetadataDetails((Long) itemId, nameVersionStr));

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
        return manageDistUIState.getSelectedSoftwareModules();
    }

    @Override
    protected boolean isMaximized() {
        return manageDistUIState.isSwModuleTableMaximized();
    }

    @Override
    protected void publishEntityAfterValueChange(final SoftwareModule selectedLastEntity) {
        eventBus.publish(this, new SoftwareModuleEvent(BaseEntityEventType.SELECTED_ENTITY, selectedLastEntity));
    }

    @Override
    protected void setManagementEntitiyStateValues(final Set<Long> values, final Long lastId) {
        manageDistUIState.setSelectedBaseSwModuleId(lastId);
        manageDistUIState.setSelectedSoftwareModules(values);
    }

    @Override
    protected SoftwareModule findEntityByTableValue(final Long lastSelectedId) {
        return softwareManagement.findSoftwareModuleById(lastSelectedId);
    }

    @Override
    protected ManagmentEntityState<Long> getManagmentEntityState() {
        return null;
    }

    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        final List<TableColumn> columnList = super.getTableVisibleColumns();
        if (isMaximized()) {
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_VENDOR, i18n.get("header.vendor"), 0.1F));
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
    protected DropHandler getTableDropHandler() {
        return new DropHandler() {

            private static final long serialVersionUID = -6175865860867652573L;

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return distributionsViewAcceptCriteria;
            }

            @Override
            public void drop(final DragAndDropEvent event) {
                /* sw module dont accept drops */
            }
        };
    }

    /* All Private Methods */

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

    private String getTableStyle(final Long typeId, final boolean isAssigned, final String color) {
        if (isAssigned) {
            addTypeStyle(typeId, color);
            return "distribution-upload-type-" + typeId;
        }
        return null;
    }

    private void addTypeStyle(final Long tagId, final String color) {
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
                SPUIComponentIdProvider.SW_TABLE_ATRTIFACT_DETAILS_ICON + "." + nameVersionStr, "", "", null, false,
                FontAwesome.FILE_O, SPUIButtonStyleSmallNoBorder.class);
        showArtifactDtlsBtn.addStyleName(SPUIStyleDefinitions.ARTIFACT_DTLS_ICON);
        showArtifactDtlsBtn.setDescription(i18n.get("tooltip.artifact.icon"));
        return showArtifactDtlsBtn;
    }

    private Button createManageMetadataButton(String nameVersionStr) {
        final Button manageMetadataBtn = SPUIComponentProvider.getButton(
                SPUIComponentIdProvider.SW_TABLE_MANAGE_METADATA_ID + "." + nameVersionStr, "", "", null, false,
                FontAwesome.PLUS_SQUARE_O, SPUIButtonStyleSmallNoBorder.class);
        manageMetadataBtn.addStyleName(SPUIStyleDefinitions.ARTIFACT_DTLS_ICON);
        manageMetadataBtn.setDescription(i18n.get("tooltip.metadata.icon"));
        return manageMetadataBtn;
    }

    private String getNameAndVerion(final Object itemId) {
        final Item item = getItem(itemId);
        final String name = (String) item.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        final String version = (String) item.getItemProperty(SPUILabelDefinitions.VAR_VERSION).getValue();
        return name + "." + version;
    }

    @Override
    protected Item addEntity(final SoftwareModule baseEntity) {
        final Item item = super.addEntity(baseEntity);

        if (!manageDistUIState.getSelectedSoftwareModules().isEmpty()) {
            manageDistUIState.getSelectedSoftwareModules().stream().forEach(this::unselect);
        }
        select(baseEntity.getId());
        return item;
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
        final Window atrifactDtlsWindow = new Window();
        atrifactDtlsWindow.setCaption(HawkbitCommonUtil.getArtifactoryDetailsLabelId(nameVersionStr));
        atrifactDtlsWindow.setCaptionAsHtml(true);
        atrifactDtlsWindow.setClosable(true);
        atrifactDtlsWindow.setResizable(true);
        atrifactDtlsWindow.setImmediate(true);
        atrifactDtlsWindow.setWindowMode(WindowMode.NORMAL);
        atrifactDtlsWindow.setModal(true);
        atrifactDtlsWindow.addStyleName(SPUIStyleDefinitions.CONFIRMATION_WINDOW_CAPTION);

        artifactDetailsLayout.setFullWindowMode(false);
        artifactDetailsLayout.populateArtifactDetails(itemId, nameVersionStr);
        /* Now add table to the window */
        artifactDetailsLayout.getArtifactDetailsTable().setWidth(700, Unit.PIXELS);
        artifactDetailsLayout.getArtifactDetailsTable().setHeight(500, Unit.PIXELS);
        atrifactDtlsWindow.setContent(artifactDetailsLayout.getArtifactDetailsTable());

        /* Create maximized view of the table */
        atrifactDtlsWindow.addWindowModeChangeListener(event -> {
            if (event.getWindowMode() == WindowMode.MAXIMIZED) {
                atrifactDtlsWindow.setSizeFull();
                artifactDetailsLayout.setFullWindowMode(true);
                artifactDetailsLayout.createMaxArtifactDetailsTable();
                artifactDetailsLayout.getMaxArtifactDetailsTable().setWidth(100, Unit.PERCENTAGE);
                artifactDetailsLayout.getMaxArtifactDetailsTable().setHeight(100, Unit.PERCENTAGE);
                atrifactDtlsWindow.setContent(artifactDetailsLayout.getMaxArtifactDetailsTable());
            } else {
                atrifactDtlsWindow.setSizeUndefined();
                atrifactDtlsWindow.setContent(artifactDetailsLayout.getArtifactDetailsTable());
            }
        });
        /* display the window */
        UI.getCurrent().addWindow(atrifactDtlsWindow);
    }

    @Override
    protected void setDataAvailable(final boolean available) {
        manageDistUIState.setNoDataAvilableSwModule(!available);

    }

    private void showMetadataDetails(Long itemId, String nameVersionStr) {
        SoftwareModule swmodule = softwareManagement.findSoftwareModuleWithDetails(itemId);
        UI.getCurrent().addWindow(swMetadataPopupLayout.getWindow(swmodule));
    }

}
