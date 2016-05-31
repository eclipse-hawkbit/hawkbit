/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIComponetIdProvider;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;

public class SoftwareModuleMetadatadetailslayout extends Table {

    private static final long serialVersionUID = 2913758299611838818L;

    private static final Logger LOG = LoggerFactory.getLogger(SoftwareModuleMetadatadetailslayout.class);

    private static final String METADATA_KEY = "Key";

    private static final String VIEW = "view";

    private SpPermissionChecker permissionChecker;

    private I18N i18n;

    /**
     * Initialize software module table- to be displayed in details layout.
     * 
     * @param i18n
     *            I18N
     * @param isUnassignSoftModAllowed
     *            boolean flag to check for unassign functionality allowed for
     *            the view.
     * @param distributionSetManagement
     *            DistributionSetManagement
     * @param permissionChecker
     *            SpPermissionChecker
     * @param eventBus
     *            SessionEventBus
     * @param manageDistUIState
     *            ManageDistUIState
     */
    public void init(final I18N i18n, final SpPermissionChecker permissionChecker) {
        this.i18n = i18n;
        this.permissionChecker = permissionChecker;
        createSWMMetadataTable();
        addCustomGeneratedColumns();
    }

    private void createSWMMetadataTable() {
        addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        addStyleName(ValoTheme.TABLE_NO_STRIPES);
        setSelectable(false);
        setImmediate(true);
        setContainerDataSource(getSwModuleMetadataContainer());
        setColumnHeaderMode(ColumnHeaderMode.EXPLICIT);
        addSMMetadataTableHeader();
        setSizeFull(); // check if this style is required
        addStyleName(SPUIStyleDefinitions.SW_MODULE_TABLE);
    }

    private IndexedContainer getSwModuleMetadataContainer() {
        final IndexedContainer container = new IndexedContainer();
        container.addContainerProperty(METADATA_KEY, String.class, "");
        setColumnExpandRatio(METADATA_KEY, 0.7f);
        setColumnAlignment(METADATA_KEY, Align.LEFT);

        if (permissionChecker.hasUpdateDistributionPermission()) {
            container.addContainerProperty(VIEW, Label.class, "");
            setColumnExpandRatio(VIEW, 0.2F);
            setColumnAlignment(VIEW, Align.RIGHT);
        }
        return container;
    }

    private void addSMMetadataTableHeader() {
        setColumnHeader(METADATA_KEY, i18n.get("label.dist.details.key"));
    }

    /**
     * Populate software module table.
     * 
     * @param distributionSet
     */
    public void populateSMMetadata(final SoftwareModule swModule) {
        removeAllItems();
        final List<SoftwareModuleMetadata> swMetadtaList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            final SoftwareModuleMetadata swMetadata = new SoftwareModuleMetadata();
            swMetadata.setKey("ReleaseNote BBB" + i);
            swMetadata.setValue("ReleaseNote BBB sample data" + i);
            swMetadtaList.add(swMetadata);
        }
        if (null != swModule) {
            /*
             * final List<SoftwareModuleMetadata> swMetadataList =
             * swModule.getMetadata();
             */
            final List<SoftwareModuleMetadata> swMetadataList = swMetadtaList;
            if (null != swMetadataList && !swMetadataList.isEmpty()) {
                swMetadataList.forEach(swMetadata -> setSWMetadataProperties(swMetadata));
            }
        }

    }

    private void setSWMetadataProperties(final SoftwareModuleMetadata swMetadata) {
        final Item item = getContainerDataSource().addItem(swMetadata.getKey());
        item.getItemProperty(METADATA_KEY).setValue(swMetadata.getKey());
        if (permissionChecker.hasUpdateDistributionPermission()) {
            item.getItemProperty(VIEW).setValue(HawkbitCommonUtil.getFormatedLabel("View"));
        }

    }

    protected void addCustomGeneratedColumns() {
        addGeneratedColumn(VIEW, (source, itemId, columnId) -> customMetadataDetailButton((String) itemId));
    }

    private Button customMetadataDetailButton(final String itemId) {
        final Item row1 = getItem(itemId);
        final String metadataKey = (String) row1.getItemProperty(METADATA_KEY).getValue();

        final Button viewIcon = SPUIComponentProvider.getButton(getDetailLinkId(metadataKey), VIEW,
                "View Software Module Metadata details", null, false, null, SPUIButtonStyleSmallNoBorder.class);
        viewIcon.setData(metadataKey);
        viewIcon.addStyleName(ValoTheme.LINK_SMALL + " " + "on-focus-no-border link");
        // viewIcon.addClickListener(event -> onClickOfDetailButton(event));
        return viewIcon;
    }

    private static String getDetailLinkId(final String name) {
        return new StringBuilder(SPUIComponetIdProvider.SW_METADATA_DETAIL_LINK).append('.').append(name).toString();
    }
}
