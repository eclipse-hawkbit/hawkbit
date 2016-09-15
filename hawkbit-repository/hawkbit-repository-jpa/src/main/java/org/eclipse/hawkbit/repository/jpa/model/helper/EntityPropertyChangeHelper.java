/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model.helper;

import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.eventbus.event.remote.json.GenericEventEntity.PropertyChange;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.internal.sessions.ObjectChangeSet;
import org.eclipse.persistence.queries.UpdateObjectQuery;
import org.eclipse.persistence.sessions.changesets.DirectToFieldChangeRecord;

/**
 * Helper class to get the change set for the property changes in the Entity.
 *
 */
public final class EntityPropertyChangeHelper {

    private EntityPropertyChangeHelper() {
        // noop
    }

    /**
     * To get the map of entity property change set
     * 
     * @param clazz
     * @param event
     * @return the map of the changeSet
     */
    public static Map<String, PropertyChange> getChangeSet(final DescriptorEvent event) {
        final ObjectChangeSet changeSet = ((UpdateObjectQuery) event.getQuery()).getObjectChangeSet();
        return changeSet.getChanges().stream().filter(record -> record instanceof DirectToFieldChangeRecord)
                .map(record -> (DirectToFieldChangeRecord) record)
                .collect(Collectors.toMap(record -> record.getAttribute(),
                        record -> new PropertyChange(record.getOldValue(), record.getNewValue())));
    }
}
