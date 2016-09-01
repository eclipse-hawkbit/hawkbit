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

import com.vaadin.data.Container;
import com.vaadin.data.Item;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

import com.vaadin.data.util.IndexedContainer;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;

/**
 *
 * DistributionSet TargetFilterQuery table
 *
 */

@SpringComponent
@VaadinSessionScope
public class TargetFilterQueryDetailsTable extends Table {

    private static final long serialVersionUID = 2913758299611837718L;

    private static final String TFQ_NAME = "name";
    private static final String TFQ_QUERY = "query";

    private I18N i18n;

    /**
     * 
     * @param i18n
     */
    public void init(final I18N i18n) {
        this.i18n = i18n;
        createTable();
    }

    /**
     * Populate software module metadata.
     *
     * @param distributionSet the selected distribution set
     */
    public void populateTableByDistributionSet(final DistributionSet distributionSet) {
        removeAllItems();
        if (null == distributionSet) {
            return;
        }

        Container dataSource = getContainerDataSource();
        List<TargetFilterQuery> filters = distributionSet.getAutoAssignFilters();
        filters.forEach(query -> {
            Object itemId = dataSource.addItem();
            Item item = dataSource.getItem(itemId);
            item.getItemProperty(TFQ_NAME).setValue(query.getName());
            item.getItemProperty(TFQ_QUERY).setValue(query.getQuery());
        });

    }

    private void createTable() {
        addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        addStyleName(ValoTheme.TABLE_NO_STRIPES);
        addStyleName(SPUIStyleDefinitions.SW_MODULE_TABLE);
        addStyleName("details-layout");
        setSelectable(false);
        setImmediate(true);
        setContainerDataSource(getDistSetContainer());
        setColumnHeaderMode(ColumnHeaderMode.EXPLICIT);
        addTableHeader();
        setSizeFull();
        // same as height of other tabs in details tabsheet
        setHeight(116, Unit.PIXELS);
    }

    private IndexedContainer getDistSetContainer() {
        final IndexedContainer container = new IndexedContainer();
        container.addContainerProperty(TFQ_NAME, String.class, "");
        container.addContainerProperty(TFQ_QUERY, String.class, "");
        setColumnExpandRatio(TFQ_NAME, 0.4F);
        setColumnAlignment(TFQ_NAME, Align.LEFT);
        setColumnExpandRatio(TFQ_QUERY, 0.6F);
        setColumnAlignment(TFQ_QUERY, Align.LEFT);

        return container;
    }

    private void addTableHeader() {
        setColumnHeader(TFQ_NAME, i18n.get("header.target.filter.name"));
        setColumnHeader(TFQ_QUERY, i18n.get("header.target.filter.query"));
    }


}
