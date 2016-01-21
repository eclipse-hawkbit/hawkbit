/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.artifacts.details.ArtifactDetailsLayout;
import org.eclipse.hawkbit.ui.artifacts.event.SMFilterEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleEvent.SoftwareModuleEventType;
import org.eclipse.hawkbit.ui.common.table.AbstractTable;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsUIEvent;
import org.eclipse.hawkbit.ui.distributions.event.DistributionsViewAcceptCriteria;
import org.eclipse.hawkbit.ui.distributions.event.DragEvent;
import org.eclipse.hawkbit.ui.distributions.event.SaveActionWindowEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;
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
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

/**
 * Implementation of software module table using generic abstract table styles .
 * 
 *
 *
 */
@SpringComponent
@ViewScope
public class SwModuleTable extends AbstractTable {

    private static final long serialVersionUID = 6785314784507424750L;

    @Autowired
    private I18N i18n;

    @Autowired
    private ManageDistUIState manageDistUIState;

    @Autowired
    private transient EventBus.SessionEventBus eventBus;

    @Autowired
    private transient SoftwareManagement softwareManagement;

    @Autowired
    private DistributionsViewAcceptCriteria distributionsViewAcceptCriteria;

    @Autowired
    private ArtifactDetailsLayout artifactDetailsLayout;

    /**
     * Initialize the filter layout.
     */
    @PostConstruct
    protected void init() {
        super.init();
        eventBus.subscribe(this);
        styleTableOnDistSelection();
        setNoDataAvailable();
    }

    @PreDestroy
    void destroy() {
        /*
         * It's good manners to do this, even though vaadin-spring will
         * automatically unsubscribe when this UI is garbage collected.
         */
        eventBus.unsubscribe(this);
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
        if (event.getSoftwareModuleEventType() == SoftwareModuleEventType.MINIMIZED) {
            UI.getCurrent().access(() -> applyMinTableSettings());
        } else if (event.getSoftwareModuleEventType() == SoftwareModuleEventType.MAXIMIZED) {
            UI.getCurrent().access(() -> applyMaxTableSettings());
        } else if (event.getSoftwareModuleEventType() == SoftwareModuleEventType.NEW_SOFTWARE_MODULE) {
            UI.getCurrent().access(() -> addSoftwareModule(event.getSoftwareModule()));
        }
    }

    /* All Override methods */

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.SPTable#getTableId()
     */
    @Override
    protected String getTableId() {
        return SPUIComponetIdProvider.UPLOAD_SOFTWARE_MODULE_TABLE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.SPTable#createContainer()
     */
    @Override
    protected Container createContainer() {
        final Map<String, Object> queryConfiguration = new HashMap<String, Object>();
        manageDistUIState.getSoftwareModuleFilters().getSearchText()
                .ifPresent(value -> queryConfiguration.put(SPUIDefinitions.FILTER_BY_TEXT, value));

        manageDistUIState.getSoftwareModuleFilters().getSoftwareModuleType()
                .ifPresent(type -> queryConfiguration.put(SPUIDefinitions.BY_SOFTWARE_MODULE_TYPE, type));

        manageDistUIState.getLastSelectedDistribution().ifPresent(
                distIdName -> queryConfiguration.put(SPUIDefinitions.ORDER_BY_DISTRIBUTION, distIdName.getId()));

        final BeanQueryFactory<SwModuleBeanQuery> swQF = new BeanQueryFactory<SwModuleBeanQuery>(
                SwModuleBeanQuery.class);
        swQF.setQueryConfiguration(queryConfiguration);

        final LazyQueryContainer container = new LazyQueryContainer(
                new LazyQueryDefinition(true, SPUIDefinitions.PAGE_SIZE, "swId"), swQF);
        return container;
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.SPTable#addContainerProperties(com.
     * vaadin.data.Container)
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.SPTable#addCustomGeneratedColumns()
     */
    @Override
    protected void addCustomGeneratedColumns() {

        addGeneratedColumn(SPUILabelDefinitions.ARTIFACT_ICON, new ColumnGenerator() {

            private static final long serialVersionUID = -5982361782989980277L;

            @Override
            public Object generateCell(final Table source, final Object itemId, final Object columnId) {
                // add artifactory details popup
                final String nameVersionStr = getNameAndVerion(itemId);
                final Button showArtifactDtlsBtn = createShowArtifactDtlsButton(nameVersionStr);
                showArtifactDtlsBtn.addClickListener(event -> showArtifactDetailsWindow((Long) itemId, nameVersionStr));
                return showArtifactDtlsBtn;
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see hawkbit.server.ui.common.table.SPTable#isFirstRowSelectedOnLoad()
     */
    @Override
    protected boolean isFirstRowSelectedOnLoad() {
        return manageDistUIState.getSelectedSoftwareModules().isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.SPTable#getItemIdToSelect()
     */
    @Override
    protected Object getItemIdToSelect() {
        return manageDistUIState.getSelectedSoftwareModules();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.common.table.SPTable#isMaximized()
     */
    @Override
    protected boolean isMaximized() {
        return manageDistUIState.isSwModuleTableMaximized();
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void onValueChange() {
        eventBus.publish(this, DragEvent.HIDE_DROP_HINT);
        @SuppressWarnings("unchecked")
        final Set<Long> values = (Set) getValue();
        if (values != null && !values.isEmpty()) {
            final Iterator<Long> iterator = values.iterator();
            Long value = null;
            while (iterator.hasNext()) {
                value = iterator.next();
            }
            if (null != value) {
                manageDistUIState.setSelectedBaseSwModuleId(value);
                final SoftwareModule baseSoftwareModule = softwareManagement.findSoftwareModuleById(value);
                manageDistUIState.setSelectedSoftwareModules(values);
                eventBus.publish(this,
                        new SoftwareModuleEvent(SoftwareModuleEventType.SELECTED_SOFTWARE_MODULE, baseSoftwareModule));
            }
        } else {
            manageDistUIState.setSelectedBaseSwModuleId(null);
            manageDistUIState.setSelectedSoftwareModules(Collections.emptySet());
            eventBus.publish(this, new SoftwareModuleEvent(SoftwareModuleEventType.SELECTED_SOFTWARE_MODULE, null));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.hawkbit.server.ui.common.table.SPTable#getTableVisibleColumns
     * ()
     */
    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        final List<TableColumn> columnList = new ArrayList<TableColumn>();
        if (isMaximized()) {
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_NAME, i18n.get("header.name"), 0.2f));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_VERSION, i18n.get("header.version"), 0.1f));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_VENDOR, i18n.get("header.vendor"), 0.1f));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_BY, i18n.get("header.createdBy"), 0.1f));
            columnList
                    .add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_DATE, i18n.get("header.createdDate"), 0.1f));
            columnList.add(
                    new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, i18n.get("header.modifiedBy"), 0.1f));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE, i18n.get("header.modifiedDate"),
                    0.1f));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_DESC, i18n.get("header.description"), 0.2f));
        } else {
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_NAME, i18n.get("header.name"), 0.7f));
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_VERSION, i18n.get("header.version"), 0.2f));
            columnList.add(new TableColumn(SPUILabelDefinitions.ARTIFACT_ICON, "", 0.1f));

        }
        return columnList;
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

    /**
     * @param color
     * @param isAssigned
     * @return
     */
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

    /**
     * @param itemId
     * @param propertyId
     * @return
     */
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
                SPUIComponetIdProvider.SW_TABLE_ATRTIFACT_DETAILS_ICON + "." + nameVersionStr, "", "", null, false,
                FontAwesome.LIST_ALT, SPUIButtonStyleSmallNoBorder.class);
        showArtifactDtlsBtn.addStyleName(SPUIStyleDefinitions.ARTIFACT_DTLS_ICON);
        showArtifactDtlsBtn.setDescription(i18n.get("tooltip.artifact.icon"));
        return showArtifactDtlsBtn;
    }

    private String getNameAndVerion(final Object itemId) {
        final Item item = getItem(itemId);
        final String name = (String) item.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        final String version = (String) item.getItemProperty(SPUILabelDefinitions.VAR_VERSION).getValue();
        return name + "." + version;
    }

    /**
     * Add new software module to table.
     *
     * @param swModule
     *            new software module
     */
    @SuppressWarnings("unchecked")
    private void addSoftwareModule(final SoftwareModule swModule) {
        final Object addItem = addItem();
        final Item item = getItem(addItem);
        final String swNameVersion = HawkbitCommonUtil.concatStrings(":", swModule.getName(), swModule.getVersion());
        item.getItemProperty(SPUILabelDefinitions.NAME_VERSION).setValue(swNameVersion);
        item.getItemProperty("swId").setValue(swModule.getId());
        item.getItemProperty(SPUILabelDefinitions.VAR_ID).setValue(swModule.getId());
        item.getItemProperty(SPUILabelDefinitions.VAR_DESC).setValue(swModule.getDescription());
        item.getItemProperty(SPUILabelDefinitions.VAR_VERSION).setValue(swModule.getVersion());
        item.getItemProperty(SPUILabelDefinitions.VAR_NAME).setValue(swModule.getName());
        item.getItemProperty(SPUILabelDefinitions.VAR_VENDOR).setValue(swModule.getVendor());
        item.getItemProperty(SPUILabelDefinitions.VAR_CREATED_BY).setValue(swModule.getCreatedBy());
        item.getItemProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY).setValue(swModule.getLastModifiedBy());
        item.getItemProperty(SPUILabelDefinitions.VAR_CREATED_DATE)
                .setValue(SPDateTimeUtil.getFormattedDate(swModule.getCreatedAt()));
        item.getItemProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE)
                .setValue(SPDateTimeUtil.getFormattedDate(swModule.getLastModifiedAt()));

        item.getItemProperty(SPUILabelDefinitions.VAR_COLOR).setValue(swModule.getType().getColour());
    }

    /**
     * @param itemId
     * @param nameVersionStr
     * @return
     */
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

    private void setNoDataAvailable() {
        final int conatinerSize = getContainerDataSource().size();
        if (conatinerSize == 0) {
            manageDistUIState.setNoDataAvilableSwModule(true);
        } else {
            manageDistUIState.setNoDataAvilableSwModule(false);
        }
    }

}
