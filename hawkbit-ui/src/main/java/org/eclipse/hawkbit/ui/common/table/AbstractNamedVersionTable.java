/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.table;

import java.util.List;

import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.ui.utils.SPUILabelDefinitions;
import org.eclipse.hawkbit.ui.utils.TableColumn;

import com.vaadin.data.Item;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.event.ShortcutAction;

/**
 * Abstract table to handling {@link NamedVersionedEntity}
 *
 * @param <E>
 *            e is the entity class
 * @param <I>
 *            i is the id of the table
 */
public abstract class AbstractNamedVersionTable<E extends NamedVersionedEntity, I> extends AbstractTable<E, I>
        implements Handler {

    private static final long serialVersionUID = 780050712209750719L;

    protected ShortcutAction actionSelectAll;

    protected ShortcutAction actionUnSelectAll;

    /**
     * Initialize the component.
     */
    @Override
    protected void init() {
        super.init();
        actionSelectAll = new ShortcutAction(i18n.get("action.target.table.selectall"));
        actionUnSelectAll = new ShortcutAction(i18n.get("action.target.table.clear"));
        setMultiSelect(true);
        setSelectable(true);
    }

    @Override
    protected List<TableColumn> getTableVisibleColumns() {
        final List<TableColumn> columnList = super.getTableVisibleColumns();
        final float versionColumnSize = isMaximized() ? 0.1F : 0.2F;
        columnList
                .add(new TableColumn(SPUILabelDefinitions.VAR_VERSION, i18n.get("header.version"), versionColumnSize));
        return columnList;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void updateEntity(final E baseEntity, final Item item) {
        super.updateEntity(baseEntity, item);
        item.getItemProperty(SPUILabelDefinitions.VAR_VERSION).setValue(baseEntity.getVersion());
    }

    @Override
    public Action[] getActions(final Object target, final Object sender) {
        return new Action[] { actionSelectAll, actionUnSelectAll };
    }

    /**
     * Select all rows in the table.
     */
    public void selectAll() {
        // only contains the ItemIds of the visible items in the table
        setValue(getItemIds());
    }

    /**
     * Clear all selections in the table.
     */
    public void unSelectAll() {
        setValue(null);
    }

}
