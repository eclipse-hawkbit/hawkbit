/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.filterlayout;

import static org.eclipse.hawkbit.ui.utils.SPUIDefinitions.NO_TAG_BUTTON_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.ui.common.ConfirmationDialog;
import org.eclipse.hawkbit.ui.common.event.FilterHeaderEvent;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleNoBorder;
import org.eclipse.hawkbit.ui.decorators.SPUITagButtonStyle;
import org.eclipse.hawkbit.ui.management.tag.TagIdName;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Item;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.DragAndDropWrapper.DragStartMode;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;
import com.vaadin.ui.Window.CloseEvent;
import com.vaadin.ui.Window.CloseListener;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Parent class for filter button layout.
 */
public abstract class AbstractFilterButtons extends Table {

    private static final long serialVersionUID = 1L;

    protected static final String DEFAULT_GREEN = "rgb(44,151,32)";

    protected static final String FILTER_BUTTON_COLUMN = "filterButton";

    private final transient EventBus.UIEventBus eventBus;

    private final AbstractFilterButtonClickBehaviour filterButtonClickBehaviour;

    private final VaadinMessageSource i18n;

    private final String noTagLabel;

    protected AbstractFilterButtons(final UIEventBus eventBus,
            final AbstractFilterButtonClickBehaviour filterButtonClickBehaviour, final VaadinMessageSource i18n) {
        this.eventBus = eventBus;
        this.i18n = i18n;
        this.filterButtonClickBehaviour = filterButtonClickBehaviour;
        createTable();
        noTagLabel = i18n.getMessage(UIMessageIdProvider.LABEL_NO_TAG);
        if (doSubscribeToEventBus()) {
            eventBus.subscribe(this);
        }
    }

    /**
     * Subscribes the view to the eventBus. Method has to be overriden (return
     * false) if the view does not contain any listener to avoid Vaadin blowing
     * up our logs with warnings.
     */
    protected boolean doSubscribeToEventBus() {
        return true;
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
        setSizeFull();
        setSelectable(false);
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

    protected void addColumn() {
        addGeneratedColumn(FILTER_BUTTON_COLUMN, (source, itemId, columnId) -> addGeneratedCell(itemId));
    }

    /**
     * Insert the update icons next to the filter tags in target, distribution
     * and software module tags/types tables
     */
    public void addUpdateColumn() {
        if (alreadyContainsColumn(SPUIDefinitions.UPDATE_FILTER_BUTTON_COLUMN)) {
            return;
        }
        deleteColumnIfVisible(SPUIDefinitions.DELETE_FILTER_BUTTON_COLUMN);
        addGeneratedColumn(SPUIDefinitions.UPDATE_FILTER_BUTTON_COLUMN,
                (source, itemId, columnId) -> addUpdateCell(itemId));
    }

    /**
     * Insert the delete icons next to the filter tags in target, distribution
     * and software module tags/types tables
     */
    public void addDeleteColumn() {
        if (alreadyContainsColumn(SPUIDefinitions.DELETE_FILTER_BUTTON_COLUMN)) {
            return;
        }
        deleteColumnIfVisible(SPUIDefinitions.UPDATE_FILTER_BUTTON_COLUMN);
        addGeneratedColumn(SPUIDefinitions.DELETE_FILTER_BUTTON_COLUMN,
                (source, itemId, columnId) -> addDeleteCell(itemId));
    }

    /**
     * Removes the edit and delete icon next to the filter tags in target,
     * distribution and software module tags/types tables, when the edit/delete
     * action is executed or cancelled
     */
    public void removeUpdateAndDeleteColumn() {
        removeGeneratedColumn(SPUIDefinitions.UPDATE_FILTER_BUTTON_COLUMN);
        removeGeneratedColumn(SPUIDefinitions.DELETE_FILTER_BUTTON_COLUMN);
    }

    private List<Object> getVisibleColumnsAsList() {
        return Arrays.asList(getVisibleColumns());
    }

    private void deleteColumnIfVisible(final String columnName) {
        final List<Object> columns = getVisibleColumnsAsList();
        if (columns.contains(columnName)) {
            removeGeneratedColumn(columnName);
        }
    }

    private boolean alreadyContainsColumn(final String columnName) {
        return getVisibleColumnsAsList().contains(columnName);
    }

    private Object addDeleteCell(final Object itemId) {
        if (itemId instanceof TagIdName && noTagLabel.equals(((TagIdName) itemId).getName())) {
            return null;
        }

        final Button deleteButton = SPUIComponentProvider.getButton("", "", "", "", true, FontAwesome.TRASH_O,
                SPUIButtonStyleNoBorder.class);
        if (itemId instanceof TagIdName) {
            deleteButton.setId(UIComponentIdProvider.DELETE_TAG_ID + ((TagIdName) itemId).getName());
        } else {
            deleteButton.setId(UIComponentIdProvider.DELETE_TAG_ID + itemId.toString());
        }
        deleteButton.setDescription(i18n.getMessage(UIMessageIdProvider.TOOLTIP_DELETE));
        deleteButton.addClickListener(this::addDeleteButtonClickListener);
        return deleteButton;
    }

    private Button addUpdateCell(final Object itemId) {
        if (itemId instanceof TagIdName && noTagLabel.equals(((TagIdName) itemId).getName())) {
            return null;
        }

        final Button editButton = SPUIComponentProvider.getButton("", "", "", "", true, FontAwesome.EDIT,
                SPUIButtonStyleNoBorder.class);
        if (itemId instanceof TagIdName) {
            editButton.setId(UIComponentIdProvider.UPDATE_TAG_ID + ((TagIdName) itemId).getName());
        } else {
            editButton.setId(UIComponentIdProvider.UPDATE_TAG_ID + itemId.toString());
        }
        editButton.setDescription(SPUIDefinitions.EDIT);
        editButton.addClickListener(this::addEditButtonClickListener);
        return editButton;
    }

    protected abstract void addEditButtonClickListener(final ClickEvent event);

    protected abstract void addDeleteButtonClickListener(final ClickEvent event);

    private DragAndDropWrapper addGeneratedCell(final Object itemId) {

        final Item item = getItem(itemId);
        final Long id = (Long) item.getItemProperty(SPUILabelDefinitions.VAR_ID).getValue();
        final String name = (String) item.getItemProperty(SPUILabelDefinitions.VAR_NAME).getValue();
        final String desc = (String) item.getItemProperty(SPUILabelDefinitions.VAR_DESC).getValue() != null
                ? item.getItemProperty(SPUILabelDefinitions.VAR_DESC).getValue().toString() : null;
        final String color = (String) item.getItemProperty(SPUILabelDefinitions.VAR_COLOR).getValue() != null
                ? item.getItemProperty(SPUILabelDefinitions.VAR_COLOR).getValue().toString() : DEFAULT_GREEN;
        final Button typeButton = createFilterButton(id, name, desc, color, itemId);
        typeButton.addClickListener(filterButtonClickBehaviour::processFilterButtonClick);

        if ((NO_TAG_BUTTON_ID.equals(typeButton.getData()) && isNoTagStateSelected())
                || (id != null && isClickedByDefault(name))) {
            filterButtonClickBehaviour.setDefaultClickedButton(typeButton);
        }

        return createDragAndDropWrapper(typeButton, name, id);
    }

    protected boolean isNoTagStateSelected() {
        return false;
    }

    private DragAndDropWrapper createDragAndDropWrapper(final Button tagButton, final String name, final Long id) {
        final DragAndDropWrapper bsmBtnWrapper = new DragAndDropWrapper(tagButton);
        bsmBtnWrapper.addStyleName(ValoTheme.DRAG_AND_DROP_WRAPPER_NO_VERTICAL_DRAG_HINTS);
        bsmBtnWrapper.addStyleName(ValoTheme.DRAG_AND_DROP_WRAPPER_NO_HORIZONTAL_DRAG_HINTS);
        bsmBtnWrapper.addStyleName(SPUIStyleDefinitions.FILTER_BUTTON_WRAPPER);
        if (getButtonWrapperData() != null) {
            if (id == null) {
                bsmBtnWrapper.setData(getButtonWrapperData());
            } else {
                bsmBtnWrapper.setData(getButtonWrapperData().concat("" + id));
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
        setColumnWidth(FILTER_BUTTON_COLUMN, 120);
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

        if (!StringUtils.isEmpty(description)) {
            button.setDescription(description);
        } else {
            button.setDescription(name);
        }
        button.setData(id == null ? SPUIDefinitions.NO_TAG_BUTTON_ID : itemId);

        return button;
    }

    private static String prepareFilterButtonCaption(final String name, final String color) {
        final StringBuilder caption = new StringBuilder();
        caption.append("<span style=\"color: ").append(color).append(" !important;\">");
        caption.append(FontAwesome.CIRCLE.getHtml());
        caption.append("</span> ");
        caption.append(name);
        return caption.toString();
    }

    protected String getEntityId(final ClickEvent event) {
        final String buttonId = event.getButton().getId();
        if (!StringUtils.hasText(buttonId)) {
            return "";
        }
        if (buttonId.startsWith(UIComponentIdProvider.UPDATE_TAG_ID)) {
            return buttonId.substring(UIComponentIdProvider.UPDATE_TAG_ID.length());
        }
        if (buttonId.startsWith(UIComponentIdProvider.DELETE_TAG_ID)) {
            return buttonId.substring(UIComponentIdProvider.DELETE_TAG_ID.length());
        }
        return "";
    }

    /**
     * Refreshes the tags tables
     */
    public void refreshTable() {
        setContainerDataSource(createButtonsLazyQueryContainer());
        removeUpdateAndDeleteColumn();
    }

    protected void openConfirmationWindowForDeletion(final String entityToDelete, final String entityName,
            final FilterHeaderEvent event) {
        final ConfirmationDialog confirmDialog = new ConfirmationDialog(
                i18n.getMessage("caption.entity.delete.action.confirmbox"),
                i18n.getMessage("message.confirm.delete.entity", entityName.toLowerCase(),
                        entityToDelete.substring(entityToDelete.indexOf('.') + 1), ""),
                i18n.getMessage(UIMessageIdProvider.BUTTON_OK), i18n.getMessage(UIMessageIdProvider.BUTTON_CANCEL),
                ok -> {
                    if (ok) {
                        deleteEntity(entityToDelete);
                    } else {
                        removeUpdateAndDeleteColumn();
                        getEventBus().publish(this, event);
                    }
                });
        confirmDialog.getWindow().addCloseListener(getCloseListenerForEditAndDeleteTag(event));
        UI.getCurrent().addWindow(confirmDialog.getWindow());
        confirmDialog.getWindow().bringToFront();
    }

    protected CloseListener getCloseListenerForEditAndDeleteTag(final FilterHeaderEvent event) {
        return new Window.CloseListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void windowClose(final CloseEvent e) {
                removeUpdateAndDeleteColumn();
                getEventBus().publish(this, event);
            }
        };
    }

    protected abstract void deleteEntity(String entityToDelete);

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

    protected EventBus.UIEventBus getEventBus() {
        return eventBus;
    }

    protected VaadinMessageSource getI18n() {
        return i18n;
    }

    protected AbstractFilterButtonClickBehaviour getFilterButtonClickBehaviour() {
        return filterButtonClickBehaviour;
    }

    protected String getNoTagLabel() {
        return noTagLabel;
    }

}
