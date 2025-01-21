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

import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Listens to updates on <code></code>JpaTarget</code> entities, filtering out updates that only change the
 * "lastTargetQuery" or "address" fields.
 */
public class EntityPropertyChangeListener implements PostUpdateEventListener {

    private static final List<String> TARGET_UPDATE_EVENT_IGNORE_FIELDS = List.of(
            "lastTargetQuery", "address", // actual to be skipped
            "optLockRevision", "lastModifiedAt", "lastModifiedBy" // system to be skipped
    );

    private static final Class<?> JPA_TARGET;
    static {
        try {
            JPA_TARGET = Class.forName("org.eclipse.hawkbit.repository.jpa.model.JpaTarget");
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void onPostUpdate(final PostUpdateEvent event) {
        if (!JPA_TARGET.isAssignableFrom(event.getEntity().getClass())) {
            // only target entity updates goes through here
            return;
        }

        boolean lastTargetQueryChanged = false;
        boolean hasNonIgnoredChanges = false;
        for (int i : event.getDirtyProperties()) {
            final String attribute = event.getPersister().getAttributeMapping(i).getAttributeName();
            if ("lastTargetQuery".equals(attribute)) {
                lastTargetQueryChanged = true;
            } else if (!TARGET_UPDATE_EVENT_IGNORE_FIELDS.contains(attribute)) {
                hasNonIgnoredChanges = true;
                break;
            }
        }

        if (hasNonIgnoredChanges || !lastTargetQueryChanged) {
            AbstractBaseEntity.doNotify(((EventAwareEntity) event.getEntity())::fireUpdateEvent);
        }
    }

    @Override
    public boolean requiresPostCommitHandling(final EntityPersister persister) {
        return false;
    }
}