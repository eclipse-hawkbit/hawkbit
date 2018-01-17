/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.table;

import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.data.Item;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.ui.DragAndDropWrapper;

/**
 * Abstract table to handling {@link NamedVersionedEntity}
 *
 * @param <E>
 *            e is the entity class
 */
public abstract class AbstractNamedVersionTable<E extends NamedVersionedEntity> extends AbstractTable<E> {

    private static final long serialVersionUID = 780050712209750719L;

    protected AbstractNamedVersionTable(final UIEventBus eventBus, final VaadinMessageSource i18n,
            final UINotification notification) {
        super(eventBus, i18n, notification);
        setMultiSelect(true);
        setSelectable(true);
    }

    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        final List<TableColumn> columnList = super.getTableVisibleColumns();
        final float versionColumnSize = isMaximized() ? 0.1F : 0.2F;
        columnList.add(new TableColumn(SPUILabelDefinitions.VAR_VERSION, i18n.getMessage("header.version"),
                versionColumnSize));
        return columnList;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void updateEntity(final E baseEntity, final Item item) {
        super.updateEntity(baseEntity, item);
        item.getItemProperty(SPUILabelDefinitions.VAR_VERSION).setValue(baseEntity.getVersion());
    }

    @Override
    protected void onDropEventFromTable(final DragAndDropEvent event) {
        // subclass can implement

    }

    @Override
    protected void onDropEventFromWrapper(final DragAndDropEvent event) {
        // subclass can implement
    }

    @Override
    protected List<String> hasMissingPermissionsForDrop() {
        return Collections.emptyList();
    }

    @Override
    protected String getDropTableId() {
        return null;
    }

    @Override
    protected boolean validateDragAndDropWrapper(final DragAndDropWrapper wrapperSource) {
        return false;
    }

}
