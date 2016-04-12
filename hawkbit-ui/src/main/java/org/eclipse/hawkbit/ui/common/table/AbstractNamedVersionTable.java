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

/**
 * Abstract table to handling {@link NamedVersionedEntity}
 *
 * @param <E>
 *            e is the entity class
 * @param <I>
 *            i is the id of the table
 */
public abstract class AbstractNamedVersionTable<E extends NamedVersionedEntity, I> extends AbstractTable<E, I> {

    private static final long serialVersionUID = 780050712209750719L;

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
        item.getItemProperty(SPUILabelDefinitions.VAR_VERSION).setValue(baseEntity.getVersion());
        super.updateEntity(baseEntity, item);
    }

}
