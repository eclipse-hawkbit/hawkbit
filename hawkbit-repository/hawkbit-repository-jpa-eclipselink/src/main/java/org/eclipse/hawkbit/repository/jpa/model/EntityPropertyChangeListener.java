/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.List;

import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.descriptors.DescriptorEventAdapter;
import org.eclipse.persistence.queries.UpdateObjectQuery;

/**
 * Listens to updates on <code></code>JpaTarget</code> entities, filtering out updates that only change the
 * "lastTargetQuery" or "address" fields.
 */
public class EntityPropertyChangeListener extends DescriptorEventAdapter {

    private static final List<String> TARGET_UPDATE_EVENT_IGNORE_FIELDS = List.of(
            "lastTargetQuery", "address", // actual to be skipped
            "optLockRevision", "lastModifiedAt", "lastModifiedBy" // system to be skipped
    );

    @Override
    public void postUpdate(final DescriptorEvent event) {
        final Object object = event.getObject();
        if (((UpdateObjectQuery) event.getQuery()).getObjectChangeSet().getChangedAttributeNames().stream()
                .anyMatch(field -> !TARGET_UPDATE_EVENT_IGNORE_FIELDS.contains(field))) {
            AbstractBaseEntity.doNotify(((EventAwareEntity) object)::fireUpdateEvent);
        }
    }
}