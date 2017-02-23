/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUITagButtonStyle;
import org.eclipse.hawkbit.ui.filtermanagement.TargetFilterBeanQuery;
import org.eclipse.hawkbit.ui.management.event.ManagementUIEvent;
import org.eclipse.hawkbit.ui.management.state.ManagementUIState;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.addons.lazyquerycontainer.LazyQueryDefinition;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.data.Item;
import com.vaadin.ui.Button;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Target filter query{#link {@link TargetFilterQuery} buttons layout.
 */
public class TargetFilterQueryButtons extends Table {
    private static final long serialVersionUID = 9188095103191937850L;
    protected static final String FILTER_BUTTON_COLUMN = "filterButton";

    private final ManagementUIState managementUIState;

    private transient EventBus.UIEventBus eventBus;

    private CustomTargetTagFilterButtonClick customTargetTagFilterButtonClick;

    TargetFilterQueryButtons(final ManagementUIState managementUIState, final UIEventBus eventBus) {
        this.managementUIState = managementUIState;
        this.eventBus = eventBus;
    }

    /**
     * initializing table.
     * 
     * @param filterButtonClickBehaviour
     */
    void init(final CustomTargetTagFilterButtonClick filterButtonClickBehaviour) {
        this.customTargetTagFilterButtonClick = filterButtonClickBehaviour;
        createTable();
        eventBus.subscribe(this);
    }

    private void createTable() {
        setImmediate(true);
        setId(getButtonsTableId());
        setStyleName("type-button-layout");
        setStyle();
        setContainerDataSource(createButtonsLazyQueryContainer());
        addTableProperties();
        addColumn();
        setTableVisibleColumns();
        setDragMode(TableDragMode.NONE);
        setSelectable(false);
        setSizeFull();

    }

    protected String getButtonsTableId() {
        return UIComponentIdProvider.CUSTOM_TARGET_TAG_TABLE_ID;
    }

    private void setStyle() {
        addStyleName(ValoTheme.TABLE_NO_STRIPES);
        addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        addStyleName(ValoTheme.TABLE_BORDERLESS);
        addStyleName(ValoTheme.TABLE_COMPACT);
    }

    protected LazyQueryContainer createButtonsLazyQueryContainer() {
        final BeanQueryFactory<TargetFilterBeanQuery> queryFactory = new BeanQueryFactory<>(
                TargetFilterBeanQuery.class);
        queryFactory.setQueryConfiguration(Collections.emptyMap());
        return new LazyQueryContainer(new LazyQueryDefinition(true, 20, "id"), queryFactory);
    }

    private void addTableProperties() {
        final LazyQueryContainer container = (LazyQueryContainer) getContainerDataSource();
        container.addContainerProperty(SPUILabelDefinitions.VAR_ID, Long.class, null, true, true);
        container.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, null, true, true);
    }

    protected void addColumn() {
        addGeneratedColumn(FILTER_BUTTON_COLUMN, (source, itemId, columnId) -> addGeneratedCell(itemId));
    }

    private Button addGeneratedCell(final Object itemId) {
        final Item item = getItem(itemId);
        final Long id = (Long) item.getItemProperty(SPUILabelDefinitions.VAR_ID).getValue();
        final String name = (String) item.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        final Button typeButton = createFilterButton(id, name, itemId);

        if (isClickedByDefault(id)) {
            customTargetTagFilterButtonClick.setDefaultButtonClicked(typeButton);
        }
        return typeButton;
    }

    private boolean isClickedByDefault(final Long id) {
        return managementUIState.getTargetTableFilters().getTargetFilterQuery().map(q -> q.equals(id)).orElse(false);
    }

    private Button createFilterButton(final Long id, final String name, final Object itemId) {
        final Button button = SPUIComponentProvider.getButton("", name, name, "", false, null,
                SPUITagButtonStyle.class);
        button.addStyleName("custom-filter-button");
        button.setId(name);
        if (id != null) {
            button.setCaption(name);
        }
        button.setDescription(name);
        button.setData(itemId);
        button.addClickListener(event -> customTargetTagFilterButtonClick.processButtonClick(event));
        return button;
    }

    private void setTableVisibleColumns() {
        final List<Object> columnIds = new ArrayList<>();
        columnIds.add(FILTER_BUTTON_COLUMN);
        setVisibleColumns(columnIds.toArray());
        setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final ManagementUIEvent event) {
        if (event == ManagementUIEvent.RESET_TARGET_FILTER_QUERY) {
            customTargetTagFilterButtonClick.clearAppliedTargetFilterQuery();
        }
    }
}
