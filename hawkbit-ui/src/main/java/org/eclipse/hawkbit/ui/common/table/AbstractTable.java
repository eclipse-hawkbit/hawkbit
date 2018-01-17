/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.common.ManagementEntityState;
import org.eclipse.hawkbit.ui.common.UserDetailsFormatter;
import org.eclipse.hawkbit.ui.components.RefreshableContainer;
import org.eclipse.hawkbit.ui.utils.SPDateTimeUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.vaadin.data.Container;
import com.vaadin.data.Item;
import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.ui.Component;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Abstract table to handling entity
 *
 * @param <E>
 *            e is the entity class
 */
public abstract class AbstractTable<E extends NamedEntity> extends Table implements RefreshableContainer {

    private static final long serialVersionUID = 1L;

    private static final float DEFAULT_COLUMN_NAME_MIN_SIZE = 0.8F;

    protected static final String ACTION_NOT_ALLOWED_MSG = "message.action.not.allowed";

    protected transient EventBus.UIEventBus eventBus;

    protected VaadinMessageSource i18n;

    protected UINotification notification;

    protected AbstractTable(final UIEventBus eventBus, final VaadinMessageSource i18n,
            final UINotification notification) {
        this.eventBus = eventBus;
        this.i18n = i18n;
        this.notification = notification;
        setStyleName("sp-table");
        setSizeFull();
        setImmediate(true);
        setHeight(100.0F, Unit.PERCENTAGE);
        addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        addStyleName(ValoTheme.TABLE_SMALL);
        setSortEnabled(false);
        setId(getTableId());
        addCustomGeneratedColumns();
        setDefault();
        addValueChangeListener(event -> onValueChange());
        setPageLength(SPUIDefinitions.PAGE_SIZE);
        eventBus.subscribe(this);
    }

    /**
     * Gets the selected item id or in multiselect mode a set of selected ids.
     * 
     * @param table
     *            the table to retrieve the selected ID(s)
     * @return the ID(s) which are selected in the table
     */
    public static <T> Set<T> getTableValue(final Table table) {
        @SuppressWarnings("unchecked")
        Set<T> values = (Set<T>) table.getValue();
        if (values == null) {
            values = Collections.emptySet();
        }
        return values.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private void onValueChange() {
        eventBus.publish(this, UploadArtifactUIEvent.HIDE_DROP_HINTS);

        final Set<Long> values = getTableValue(this);

        Long lastId = null;
        if (!values.isEmpty()) {
            lastId = Iterables.getLast(values);
        }
        setManagementEntityStateValues(values, lastId);
        selectEntity(lastId);
        afterEntityIsSelected();
    }

    protected void setManagementEntityStateValues(final Set<Long> values, final Long lastId) {
        final ManagementEntityState managementEntityState = getManagementEntityState();
        if (managementEntityState == null) {
            return;
        }
        managementEntityState.setLastSelectedEntityId(lastId);
        managementEntityState.setSelectedEnitities(values);
    }

    private void setDefault() {
        setSelectable(true);
        setMultiSelect(true);
        setDragMode(TableDragMode.MULTIROW);
        setColumnCollapsingAllowed(false);
        setDropHandler(getTableDropHandler());
    }

    protected void addNewContainerDS() {
        final Container container = createContainer();
        addContainerProperties(container);
        setContainerDataSource(container);
        final int size = container.size();
        if (size == 0) {
            setData(SPUIDefinitions.NO_DATA);
        }
    }

    protected void selectRow() {
        if (!isMaximized()) {
            if (isFirstRowSelectedOnLoad()) {
                selectFirstRow();
            } else {
                setValue(getItemIdToSelect());
            }
        }
    }

    /**
     * Select all rows in the table.
     */
    protected void selectAll() {
        if (isMultiSelect()) {
            // only contains the ItemIds of the visible items in the table
            setValue(getItemIds());
        }
    }

    protected void setColumnProperties() {
        final List<TableColumn> columnList = getTableVisibleColumns();
        final List<Object> swColumnIds = new ArrayList<>();
        for (final TableColumn column : columnList) {
            setColumnHeader(column.getColumnPropertyId(), column.getColumnHeader());
            setColumnExpandRatio(column.getColumnPropertyId(), column.getExpandRatio());
            swColumnIds.add(column.getColumnPropertyId());
        }
        setVisibleColumns(swColumnIds.toArray());
    }

    private void selectFirstRow() {
        final Container container = getContainerDataSource();
        final int size = container.size();
        if (size > 0) {
            select(firstItemId());
        }
    }

    private void applyMaxTableSettings() {
        setColumnProperties();
        setValue(null);
        setSelectable(false);
        setMultiSelect(false);
        setDragMode(TableDragMode.NONE);
        setColumnCollapsingAllowed(true);
    }

    private void applyMinTableSettings() {
        setDefault();
        setColumnProperties();
        selectRow();
    }

    protected void refreshFilter() {
        addNewContainerDS();
        setColumnProperties();
        selectRow();
    }

    @SuppressWarnings("unchecked")
    protected void updateEntity(final E baseEntity, final Item item) {
        item.getItemProperty(SPUILabelDefinitions.VAR_NAME).setValue(baseEntity.getName());
        item.getItemProperty(SPUILabelDefinitions.VAR_ID).setValue(baseEntity.getId());
        item.getItemProperty(SPUILabelDefinitions.VAR_DESC).setValue(baseEntity.getDescription());
        item.getItemProperty(SPUILabelDefinitions.VAR_CREATED_BY)
                .setValue(UserDetailsFormatter.loadAndFormatCreatedBy(baseEntity));
        item.getItemProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY)
                .setValue(UserDetailsFormatter.loadAndFormatLastModifiedBy(baseEntity));
        item.getItemProperty(SPUILabelDefinitions.VAR_CREATED_DATE)
                .setValue(SPDateTimeUtil.getFormattedDate(baseEntity.getCreatedAt()));
        item.getItemProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE)
                .setValue(SPDateTimeUtil.getFormattedDate(baseEntity.getLastModifiedAt()));

    }

    protected void onBaseEntityEvent(final BaseUIEntityEvent<E> event) {
        if (BaseEntityEventType.MINIMIZED == event.getEventType()) {
            UI.getCurrent().access(this::applyMinTableSettings);
        } else if (BaseEntityEventType.MAXIMIZED == event.getEventType()) {
            UI.getCurrent().access(this::applyMaxTableSettings);
        } else if (BaseEntityEventType.ADD_ENTITY == event.getEventType()
                || BaseEntityEventType.REMOVE_ENTITY == event.getEventType()) {
            UI.getCurrent().access(this::refreshContainer);
        }
    }

    /**
     * Return the entity which should be deleted by a transferable
     * 
     * @param transferable
     *            the table transferable
     * @return set of entities id which will deleted
     */
    public Set<Long> getDeletedEntityByTransferable(final TableTransferable transferable) {
        final Set<Long> selectedEntities = getTableValue(this);
        final Set<Long> ids = new HashSet<>();
        final Long tranferableData = (Long) transferable.getData(SPUIDefinitions.ITEMID);
        if (tranferableData == null) {
            return ids;
        }

        if (!selectedEntities.contains(tranferableData)) {
            ids.add(tranferableData);
        } else {
            ids.addAll(selectedEntities);
        }
        return ids;
    }

    /**
     * Finds the entity object of the given entity ID by performing a database
     * search
     * 
     * @param lastSelectedId
     *            ID of the entity
     * @return entity object as Optional
     */
    protected abstract Optional<E> findEntityByTableValue(Long lastSelectedId);

    /**
     * This method is performed after selecting the current entity in the table.
     */
    protected void afterEntityIsSelected() {
        // can be overridden by subclass
    }

    /**
     * Publish the BaseEntityEventType.SELECTED_ENTITY Event with the given
     * entity.
     * 
     * @param selectedLastEntity
     *            entity that was selected in the table
     */
    protected abstract void publishSelectedEntityEvent(final E selectedLastEntity);

    protected void setLastSelectedEntityId(final Long selectedLastEntityId) {
        getManagementEntityState().setLastSelectedEntityId(selectedLastEntityId);
    }

    protected abstract ManagementEntityState getManagementEntityState();

    /**
     * Get Id of the table.
     * 
     * @return Id.
     */
    protected abstract String getTableId();

    /**
     * Create container of the data to be displayed by the table.
     */
    protected abstract Container createContainer();

    /**
     * Add container properties to the container passed in the reference.
     * 
     * @param container
     *            reference of {@link Container}
     */
    protected abstract void addContainerProperties(Container container);

    /**
     * Add any generated columns if required.
     */
    protected void addCustomGeneratedColumns() {
        // can be overriden
    }

    /**
     * Check if the first row should be selected by default on load. (if there
     * is no other item selected)
     * 
     * @return true if it should be selected otherwise return false, if there is
     *         a different item already selected.
     */
    protected abstract boolean isFirstRowSelectedOnLoad();

    /**
     * Get Item Id which should be displayed as selected.
     * 
     * @return reference of Item Id of the Row.
     */
    protected abstract Object getItemIdToSelect();

    /**
     * Check if the table is maximized or minimized.
     * 
     * @return true if maximized, otherwise false.
     */
    protected abstract boolean isMaximized();

    /**
     * Based on table state (max/min) columns to be shown are returned.
     * 
     * @return List<TableColumn> list of visible columns
     */
    protected List<TableColumn> getTableVisibleColumns() {
        final List<TableColumn> columnList = new ArrayList<>();
        if (!isMaximized()) {
            columnList.add(new TableColumn(SPUILabelDefinitions.VAR_NAME, i18n.getMessage("header.name"),
                    getColumnNameMinimizedSize()));
            return columnList;
        }
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_NAME, i18n.getMessage("header.name"), 0.2F));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_CREATED_BY, i18n.getMessage("header.createdBy"), 0.1F));
        columnList.add(
                new TableColumn(SPUILabelDefinitions.VAR_CREATED_DATE, i18n.getMessage("header.createdDate"), 0.1F));
        columnList.add(
                new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY, i18n.getMessage("header.modifiedBy"), 0.1F));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_LAST_MODIFIED_DATE,
                i18n.getMessage("header.modifiedDate"), 0.1F));
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_DESC, i18n.getMessage("header.description"), 0.2F));
        setItemDescriptionGenerator((source, itemId, propertyId) -> {

            if (SPUILabelDefinitions.VAR_CREATED_BY.equals(propertyId)) {
                return getItem(itemId).getItemProperty(SPUILabelDefinitions.VAR_CREATED_BY).getValue().toString();
            }
            if (SPUILabelDefinitions.VAR_LAST_MODIFIED_BY.equals(propertyId)) {
                return getItem(itemId).getItemProperty(SPUILabelDefinitions.VAR_LAST_MODIFIED_BY).getValue().toString();
            }
            return null;
        });

        return columnList;
    }

    protected float getColumnNameMinimizedSize() {
        return DEFAULT_COLUMN_NAME_MIN_SIZE;
    }

    private DropHandler getTableDropHandler() {
        return new DropHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return getDropAcceptCriterion();
            }

            @Override
            public void drop(final DragAndDropEvent event) {
                if (!isDropValid(event)) {
                    return;
                }
                if (event.getTransferable().getSourceComponent() instanceof Table) {
                    onDropEventFromTable(event);
                } else if (event.getTransferable().getSourceComponent() instanceof DragAndDropWrapper) {
                    onDropEventFromWrapper(event);
                }
            }
        };
    }

    protected Set<Long> getDraggedTargetList(final DragAndDropEvent event) {
        final com.vaadin.event.dd.TargetDetails targetDet = event.getTargetDetails();
        final Table targetTable = (Table) targetDet.getTarget();
        final Set<Long> targetSelected = getTableValue(targetTable);

        final AbstractSelectTargetDetails dropData = (AbstractSelectTargetDetails) event.getTargetDetails();
        final Long targetItemId = (Long) dropData.getItemIdOver();

        if (!targetSelected.contains(targetItemId)) {
            return Sets.newHashSet(targetItemId);
        }

        return targetSelected;
    }

    private Set<Long> getDraggedTargetList(final TableTransferable transferable, final Table source) {
        @SuppressWarnings("unchecked")
        final AbstractTable<NamedEntity> table = (AbstractTable<NamedEntity>) source;
        return table.getDeletedEntityByTransferable(transferable);
    }

    private boolean validateDropList(final Set<?> droplist) {
        if (droplist.isEmpty()) {
            final String actionDidNotWork = i18n.getMessage("message.action.did.not.work", new Object[] {});
            notification.displayValidationError(actionDidNotWork);
            return false;
        }
        return true;
    }

    protected boolean isDropValid(final DragAndDropEvent dragEvent) {
        final Transferable transferable = dragEvent.getTransferable();
        final Component compsource = transferable.getSourceComponent();

        final List<String> missingPermissions = hasMissingPermissionsForDrop();
        if (!missingPermissions.isEmpty()) {
            notification.displayValidationError(i18n.getMessage("message.permission.insufficient", missingPermissions));
            return false;
        }

        if (compsource instanceof Table) {
            return validateTable((Table) compsource)
                    && validateDropList(getDraggedTargetList((TableTransferable) transferable, (Table) compsource));
        }

        if (compsource instanceof DragAndDropWrapper) {
            return validateDragAndDropWrapper((DragAndDropWrapper) compsource)
                    && validateDropList(getDraggedTargetList(dragEvent));
        }
        notification.displayValidationError(i18n.getMessage(ACTION_NOT_ALLOWED_MSG));
        return false;
    }

    private boolean validateTable(final Table compsource) {
        if (!compsource.getId().equals(getDropTableId())) {
            notification.displayValidationError(ACTION_NOT_ALLOWED_MSG);
            return false;
        }
        return true;
    }

    @Override
    public void refreshContainer() {
        final Container container = getContainerDataSource();
        if (!(container instanceof LazyQueryContainer)) {
            return;
        }
        ((LazyQueryContainer) getContainerDataSource()).refresh();
    }

    protected UINotification getNotification() {
        return notification;
    }

    /**
     * Finds the entity object of the given entity ID and performs the
     * publishing of the BaseEntityEventType.SELECTED_ENTITY event
     * 
     * @param entityId
     *            ID of the current entity
     */
    public void selectEntity(final Long entityId) {
        E entity = null;
        if (entityId != null) {
            entity = findEntityByTableValue(entityId).orElse(null);
        }

        setLastSelectedEntityId(entityId);
        publishSelectedEntityEvent(entity);
    }

    protected abstract List<String> hasMissingPermissionsForDrop();

    protected abstract boolean validateDragAndDropWrapper(final DragAndDropWrapper wrapperSource);

    protected abstract void onDropEventFromWrapper(DragAndDropEvent event);

    protected abstract void onDropEventFromTable(DragAndDropEvent event);

    protected abstract String getDropTableId();

    protected abstract AcceptCriterion getDropAcceptCriterion();

    protected abstract void setDataAvailable(boolean available);

}
