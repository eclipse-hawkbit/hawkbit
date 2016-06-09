/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.filterlayout;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;

import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUITagButtonStyle;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus;

import com.vaadin.data.Item;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.DragAndDropWrapper.DragStartMode;
import com.vaadin.ui.Table;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Parent class for filter button layout.
 * 
 *
 *
 * 
 */
public abstract class AbstractFilterButtons extends Table {

    private static final long serialVersionUID = 7783305719009746375L;

    private static final String DEFAULT_GREEN = "rgb(44,151,32)";

    protected static final String FILTER_BUTTON_COLUMN = "filterButton";

    @Autowired
    protected transient EventBus.SessionEventBus eventBus;

    private AbstractFilterButtonClickBehaviour filterButtonClickBehaviour;

    /**
     * Initialize layout of filter buttons.
     * 
     * @param filterButtonClickBehaviour
     *            click behaviour of filter buttons.
     */
    public void init(final AbstractFilterButtonClickBehaviour filterButtonClickBehaviour) {
        this.filterButtonClickBehaviour = filterButtonClickBehaviour;
        createTable();
        eventBus.subscribe(this);
    }

    @PreDestroy
    void destroy() {
        eventBus.unsubscribe(this);
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

    private void setStyle() {
        addStyleName(ValoTheme.TABLE_NO_STRIPES);
        addStyleName(ValoTheme.TABLE_NO_HORIZONTAL_LINES);
        addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        addStyleName(ValoTheme.TABLE_BORDERLESS);
        addStyleName(ValoTheme.TABLE_COMPACT);
    }

    private void addTableProperties() {
        final LazyQueryContainer container = (LazyQueryContainer) getContainerDataSource();
        container.addContainerProperty(SPUILabelDefinitions.VAR_ID, Long.class, null, true, true);
        container.addContainerProperty(SPUILabelDefinitions.VAR_COLOR, String.class, "", true, true);
        container.addContainerProperty(SPUILabelDefinitions.VAR_DESC, String.class, "", true, true);
        container.addContainerProperty(SPUILabelDefinitions.VAR_NAME, String.class, null, true, true);
    }

    @SuppressWarnings("serial")
    protected void addColumn() {
        addGeneratedColumn(FILTER_BUTTON_COLUMN, (source, itemId, columnId) -> addGeneratedCell(itemId));
    }

    /**
     * @param itemId
     * @return
     */
    private DragAndDropWrapper addGeneratedCell(final Object itemId) {
        final Item item = getItem(itemId);
        final Long id = (Long) item.getItemProperty(SPUILabelDefinitions.VAR_ID).getValue();
        final String name = (String) item.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        final String desc = HawkbitCommonUtil
                .trimAndNullIfEmpty((String) item.getItemProperty(SPUILabelDefinitions.VAR_DESC).getValue()) != null
                        ? item.getItemProperty(SPUILabelDefinitions.VAR_DESC).getValue().toString() : null;
        final String color = HawkbitCommonUtil
                .trimAndNullIfEmpty((String) item.getItemProperty(SPUILabelDefinitions.VAR_COLOR).getValue()) != null
                        ? item.getItemProperty(SPUILabelDefinitions.VAR_COLOR).getValue().toString() : DEFAULT_GREEN;
        final Button typeButton = createFilterButton(id, name, desc, color, itemId);
        typeButton.addClickListener(event -> filterButtonClickBehaviour.processFilterButtonClick(event));
        if (typeButton.getData().equals(SPUIDefinitions.NO_TAG_BUTTON_ID) && isNoTagSateSelected()) {
            filterButtonClickBehaviour.setDefaultClickedButton(typeButton);
        } else if (id != null && isClickedByDefault(name)) {
            filterButtonClickBehaviour.setDefaultClickedButton(typeButton);
        }
        return createDragAndDropWrapper(typeButton, name, id);
    }

    protected boolean isNoTagSateSelected() {
        return Boolean.FALSE;
    }

    private DragAndDropWrapper createDragAndDropWrapper(final Button tagButton, final String name, final Long id) {
        final DragAndDropWrapper bsmBtnWrapper = new DragAndDropWrapper(tagButton);
        bsmBtnWrapper.addStyleName(ValoTheme.DRAG_AND_DROP_WRAPPER_NO_VERTICAL_DRAG_HINTS);
        bsmBtnWrapper.addStyleName(ValoTheme.DRAG_AND_DROP_WRAPPER_NO_HORIZONTAL_DRAG_HINTS);
        bsmBtnWrapper.addStyleName(SPUIStyleDefinitions.FILTER_BUTTON_WRAPPER);
        if (getButtonWrapperData() != null) {
            if (id != null) {
                bsmBtnWrapper.setData(getButtonWrapperData().concat(id.toString()));
            } else {
                bsmBtnWrapper.setData(getButtonWrapperData());
            }
        }
        bsmBtnWrapper.setId(getButttonWrapperIdPrefix().concat(name));
        bsmBtnWrapper.setDragStartMode(DragStartMode.WRAPPER);
        bsmBtnWrapper.setDropHandler(getFilterButtonDropHandler());
        return bsmBtnWrapper;
    }

    private void setTableVisibleColumns() {
        final List<Object> columnIds = new ArrayList<>();
        columnIds.add(FILTER_BUTTON_COLUMN);
        setVisibleColumns(columnIds.toArray());
        setColumnHeaderMode(ColumnHeaderMode.HIDDEN);
        setColumnWidth(FILTER_BUTTON_COLUMN, 137);
    }

    private Button createFilterButton(final Long id, final String name, final String description, final String color,
            final Object itemId) {
        /**
         * No icon displayed for "NO TAG" button.
         */
        final Button button = SPUIComponentProvider.getButton("", name, description, "", false, null,
                SPUITagButtonStyle.class);
        button.setId(createButtonId(name));
        button.setCaptionAsHtml(true);
        if (id != null) {
            // Use button.getCaption() since the caption name is modified
            // according to the length
            // available in UI.
            button.setCaption(prepareFilterButtonCaption(button.getCaption(), color));
        }

        if (HawkbitCommonUtil.trimAndNullIfEmpty(description) != null) {
            button.setDescription(description);
        } else {
            button.setDescription(name);
        }
        button.setData(id == null ? SPUIDefinitions.NO_TAG_BUTTON_ID : itemId);

        return button;
    }

    private String prepareFilterButtonCaption(final String name, final String color) {
        final StringBuilder caption = new StringBuilder();
        caption.append("<span style=\"color: ").append(color).append(" !important;\">");
        caption.append(FontAwesome.CIRCLE.getHtml());
        caption.append("</span> ");
        caption.append(name);
        return caption.toString();
    }

    protected void refreshTable() {
        setContainerDataSource(createButtonsLazyQueryContainer());
    }

    /**
     * Id of the buttons table to be used in test cases.
     * 
     * @return Id of the Button table.
     */
    protected abstract String getButtonsTableId();

    /**
     * create new lazyquery container to display the buttons.
     * 
     * @return reference of {@link LazyQueryContainer}
     */
    protected abstract LazyQueryContainer createButtonsLazyQueryContainer();

    /**
     * Check if button should be displayed as clicked by default.
     * 
     * @param buttonCaption
     *            button caption
     * @return true if button is clicked
     */
    protected abstract boolean isClickedByDefault(final String buttonCaption);

    /**
     * Get filter button Id.
     * 
     * @param name
     * @return
     */
    protected abstract String createButtonId(String name);

    /**
     * Get Drop Handler for Filter Buttons.
     * 
     * @return
     */
    protected abstract DropHandler getFilterButtonDropHandler();

    /**
     * Get prefix Id of Button Wrapper to be used for drag and drop, delete and
     * test cases.
     * 
     * @return prefix Id of Button Wrapper
     */
    protected abstract String getButttonWrapperIdPrefix();

    /**
     * Get info to be set for the button wrapper.
     * 
     * @return button wrapper info.
     */
    protected abstract String getButtonWrapperData();
}
