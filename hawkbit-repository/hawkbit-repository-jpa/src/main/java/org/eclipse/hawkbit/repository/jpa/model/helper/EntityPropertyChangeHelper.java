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

import org.eclipse.hawkbit.repository.eventbus.event.AbstractPropertyChangeEvent;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.internal.sessions.ObjectChangeSet;
import org.eclipse.persistence.queries.UpdateObjectQuery;
import org.eclipse.persistence.sessions.changesets.DirectToFieldChangeRecord;

public class EntityPropertyChangeHelper<T extends TenantAwareBaseEntity>  {
    
    
    public static <T extends TenantAwareBaseEntity> Map<String, AbstractPropertyChangeEvent<T>.Values> getChangeSet(
            final Class<T> clazz, final DescriptorEvent event) {
        final T rolloutGroup = clazz.cast(event.getObject());
        final ObjectChangeSet changeSet = ((UpdateObjectQuery) event.getQuery())
                .getObjectChangeSet();
        return changeSet
                .getChanges()
                .stream()
                .filter(record -> record instanceof DirectToFieldChangeRecord)
                .map(record -> (DirectToFieldChangeRecord) record)
                .collect(
                        Collectors.toMap(
                                record -> record.getAttribute(),
                                record -> new AbstractPropertyChangeEvent<T>(
                                        rolloutGroup, null).new Values(record
                                        .getOldValue(), record.getNewValue())));
    }

}
