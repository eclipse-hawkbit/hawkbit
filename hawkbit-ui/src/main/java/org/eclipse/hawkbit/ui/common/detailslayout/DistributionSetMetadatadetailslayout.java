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

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.distributions.dstable.DsMetadataPopupLayout;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * DistributionSet Metadata details layout.
 *
 */
public class DistributionSetMetadatadetailslayout extends Table {

    private static final long serialVersionUID = 2913758299611837718L;

    private static final String METADATA_KEY = "Key";

    private static final String VIEW = "view";

    private final transient DistributionSetManagement distributionSetManagement;

    private final DsMetadataPopupLayout dsMetadataPopupLayout;

    private final SpPermissionChecker permissionChecker;

    private final transient EntityFactory entityFactory;

    private final I18N i18n;

    private Long selectedDistSetId;

    public DistributionSetMetadatadetailslayout(final I18N i18n, final SpPermissionChecker permissionChecker,
            final DistributionSetManagement distributionSetManagement,
            final DsMetadataPopupLayout dsMetadataPopupLayout, final EntityFactory entityFactory) {
        this.i18n = i18n;
        this.permissionChecker = permissionChecker;
        this.distributionSetManagement = distributionSetManagement;
        this.dsMetadataPopupLayout = dsMetadataPopupLayout;
        this.entityFactory = entityFactory;
        createDSMetadataTable();
        addCustomGeneratedColumns();
    }

    /**
     * Populate software module metadata.
     *
     * @param distributionSet
     */
    public void populateDSMetadata(final DistributionSet distributionSet) {
        removeAllItems();
        if (null == distributionSet) {
            return;
        }
        selectedDistSetId = distributionSet.getId();
        final List<DistributionSetMetadata> dsMetadataList = distributionSetManagement
                .findDistributionSetMetadataByDistributionSetId(selectedDistSetId);
        if (null != dsMetadataList && !dsMetadataList.isEmpty()) {
            dsMetadataList.forEach(dsMetadata -> setDSMetadataProperties(dsMetadata));
        }

    }

    private void createDSMetadataTable() {
        addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        addStyleName(ValoTheme.TABLE_NO_STRIPES);
        addStyleName(SPUIStyleDefinitions.SW_MODULE_TABLE);
        addStyleName("details-layout");
        setSelectable(false);
        setImmediate(true);
        setContainerDataSource(getDistSetContainer());
        setColumnHeaderMode(ColumnHeaderMode.EXPLICIT);
        addDSMetadataTableHeader();
        setSizeFull();
        // same as height of other tabs in details tabsheet
        setHeight(116, Unit.PIXELS);
    }

    private IndexedContainer getDistSetContainer() {
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

    private void addDSMetadataTableHeader() {
        setColumnHeader(METADATA_KEY, i18n.get("header.key"));
    }

    private void setDSMetadataProperties(final DistributionSetMetadata dsMetadata) {
        final Item item = getContainerDataSource().addItem(dsMetadata.getKey());
        item.getItemProperty(METADATA_KEY).setValue(dsMetadata.getKey());

    }

    private void addCustomGeneratedColumns() {
        addGeneratedColumn(METADATA_KEY, (source, itemId, columnId) -> customMetadataDetailButton((String) itemId));
    }

    private Button customMetadataDetailButton(final String metadataKey) {
        final Button viewIcon = SPUIComponentProvider.getButton(getDetailLinkId(metadataKey), metadataKey,
                "View " + metadataKey + "  Metadata details", null, false, null, SPUIButtonStyleSmallNoBorder.class);
        viewIcon.setData(metadataKey);
        viewIcon.addStyleName(ValoTheme.BUTTON_TINY + " " + ValoTheme.BUTTON_LINK + " " + "on-focus-no-border link"
                + " " + "text-style");
        viewIcon.addClickListener(event -> showMetadataDetails(selectedDistSetId, metadataKey));
        return viewIcon;
    }

    private static String getDetailLinkId(final String name) {
        return new StringBuilder(UIComponentIdProvider.DS_METADATA_DETAIL_LINK).append('.').append(name).toString();
    }

    private void showMetadataDetails(final Long selectedDistSetId, final String metadataKey) {
        final DistributionSet distSet = distributionSetManagement.findDistributionSetById(selectedDistSetId);

        /* display the window */
        UI.getCurrent()
                .addWindow(dsMetadataPopupLayout.getWindow(distSet, entityFactory.generateMetadata(metadataKey, "")));
    }

}
