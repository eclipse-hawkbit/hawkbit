/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.List;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.smtable.SwMetadataPopupLayout;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.CollectionUtils;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * SoftwareModule Metadata details layout.
 *
 */
@SpringComponent
@UIScope
public class SoftwareModuleMetadatadetailslayout extends Table {

    private static final long serialVersionUID = 2913758299611838818L;

    private static final String METADATA_KEY = "Key";

    private static final int MAX_METADATA_QUERY = 500;

    private SpPermissionChecker permissionChecker;

    private transient SoftwareModuleManagement softwareModuleManagement;

    private SwMetadataPopupLayout swMetadataPopupLayout;

    private VaadinMessageSource i18n;

    private Long selectedSWModuleId;

    private transient EntityFactory entityFactory;

    /**
     * Initialize the layout.
     * 
     * @param i18n
     *            the i18n service
     * @param permissionChecker
     *            the permission checker service
     * @param softwareManagement
     *            the software management service
     * @param swMetadataPopupLayout
     *            the software module metadata popup layout
     * @param entityFactory
     *            the entity factory service
     */
    public void init(final VaadinMessageSource i18n, final SpPermissionChecker permissionChecker,
            final SoftwareModuleManagement softwareManagement, final SwMetadataPopupLayout swMetadataPopupLayout,
            final EntityFactory entityFactory) {
        this.i18n = i18n;
        this.permissionChecker = permissionChecker;
        this.softwareModuleManagement = softwareManagement;
        this.swMetadataPopupLayout = swMetadataPopupLayout;
        this.entityFactory = entityFactory;
        createSWMMetadataTable();
        addCustomGeneratedColumns();
    }

    /**
     * Populate software module metadata table.
     * 
     * @param swModule
     */
    public void populateSMMetadata(final SoftwareModule swModule) {
        removeAllItems();
        if (null == swModule) {
            return;
        }
        selectedSWModuleId = swModule.getId();
        final List<SoftwareModuleMetadata> swMetadataList = softwareModuleManagement
                .findMetaDataBySoftwareModuleId(new PageRequest(0, MAX_METADATA_QUERY),
                        selectedSWModuleId)
                .getContent();
        if (!CollectionUtils.isEmpty(swMetadataList)) {
            swMetadataList.forEach(this::setSWMetadataProperties);
        }
    }

    /**
     * Create metadata.
     * 
     * @param metadataKeyName
     */
    public void createMetadata(final String metadataKeyName) {
        final IndexedContainer metadataContainer = (IndexedContainer) getContainerDataSource();
        final Item item = metadataContainer.addItem(metadataKeyName);
        item.getItemProperty(METADATA_KEY).setValue(metadataKeyName);

    }

    /**
     * Delete metadata.
     * 
     * @param metadataKeyName
     */
    public void deleteMetadata(final String metadataKeyName) {
        final IndexedContainer metadataContainer = (IndexedContainer) getContainerDataSource();
        metadataContainer.removeItem(metadataKeyName);
    }

    private void createSWMMetadataTable() {
        addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        addStyleName(ValoTheme.TABLE_NO_STRIPES);
        addStyleName(SPUIStyleDefinitions.SW_MODULE_TABLE);
        setSelectable(false);
        setImmediate(true);
        setContainerDataSource(getSwModuleMetadataContainer());
        setColumnHeaderMode(ColumnHeaderMode.EXPLICIT);
        addSMMetadataTableHeader();
        setSizeFull();
        // same as height of other tabs in details tabsheet
        setHeight(116, Unit.PIXELS);
    }

    private IndexedContainer getSwModuleMetadataContainer() {
        final IndexedContainer container = new IndexedContainer();
        container.addContainerProperty(METADATA_KEY, String.class, "");
        setColumnAlignment(METADATA_KEY, Align.LEFT);
        return container;
    }

    private void addSMMetadataTableHeader() {
        setColumnHeader(METADATA_KEY, i18n.getMessage("header.key"));
    }

    private void setSWMetadataProperties(final SoftwareModuleMetadata swMetadata) {
        final Item item = getContainerDataSource().addItem(swMetadata.getKey());
        item.getItemProperty(METADATA_KEY).setValue(swMetadata.getKey());
    }

    private void addCustomGeneratedColumns() {
        addGeneratedColumn(METADATA_KEY, (source, itemId, columnId) -> customMetadataDetailButton((String) itemId));
    }

    private Button customMetadataDetailButton(final String metadataKey) {
        final Button viewLink = SPUIComponentProvider.getButton(getDetailLinkId(metadataKey), metadataKey,
                "View" + metadataKey + " Metadata details", null, false, null, SPUIButtonStyleSmallNoBorder.class);
        viewLink.setData(metadataKey);
        if (permissionChecker.hasUpdateDistributionPermission()) {
            viewLink.addStyleName(ValoTheme.BUTTON_TINY + " " + ValoTheme.BUTTON_LINK + " " + "on-focus-no-border link"
                    + " " + "text-style");
            viewLink.addClickListener(event -> showMetadataDetails(selectedSWModuleId, metadataKey));
        }
        return viewLink;
    }

    private static String getDetailLinkId(final String name) {
        return new StringBuilder(UIComponentIdProvider.SW_METADATA_DETAIL_LINK).append('.').append(name).toString();
    }

    private void showMetadataDetails(final Long selectedSWModuleId, final String metadataKey) {
        softwareModuleManagement.get(selectedSWModuleId).ifPresent(swmodule -> UI.getCurrent()
                .addWindow(swMetadataPopupLayout.getWindow(swmodule, entityFactory.generateMetadata(metadataKey, ""))));
    }

}
