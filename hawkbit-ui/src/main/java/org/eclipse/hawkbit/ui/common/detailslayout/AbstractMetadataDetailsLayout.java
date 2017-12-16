/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.detailslayout;

import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorder;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.ui.Button;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract metadata tab for entities.
 *
 */
public abstract class AbstractMetadataDetailsLayout extends Table {

    protected static final String METADATA_KEY = "Key";

    protected static final int MAX_METADATA_QUERY = 500;

    private final VaadinMessageSource i18n;

    protected AbstractMetadataDetailsLayout(final VaadinMessageSource i18n) {
        this.i18n = i18n;
        createMetadataTable();

        addCustomGeneratedColumns();
    }

    private VaadinMessageSource getI18n() {
        return i18n;
    }

    private void createMetadataTable() {
        addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        addStyleName(ValoTheme.TABLE_NO_STRIPES);
        setSelectable(false);
        setImmediate(true);
        setContainerDataSource(getContainer());
        setColumnHeaderMode(ColumnHeaderMode.EXPLICIT);
        addTableHeader();
        setSizeFull();
        // same as height of other tabs in details tabsheet
        setHeight(116, Unit.PIXELS);
    }

    private IndexedContainer getContainer() {
        final IndexedContainer container = new IndexedContainer();
        container.addContainerProperty(METADATA_KEY, String.class, "");
        setColumnExpandRatio(METADATA_KEY, 0.7F);
        setColumnAlignment(METADATA_KEY, Align.LEFT);

        return container;
    }

    private void addTableHeader() {
        setColumnHeader(METADATA_KEY, getI18n().getMessage("header.key"));
    }

    protected void setMetadataProperties(final MetaData dsMetadata) {
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
        viewIcon.addClickListener(event -> showMetadataDetails(metadataKey));
        return viewIcon;
    }

    protected abstract String getDetailLinkId(final String name);

    protected abstract void showMetadataDetails(final String metadataKey);

}
