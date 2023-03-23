/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.event;

/**
 * Event topics constants
 */
public final class EventTopics {
    public static final String REMOTE_EVENT_RECEIVED = "remoteEventReceived";
    public static final String REMOTE_EVENT_DISPATCHED = "remoteEventDispatched";
    public static final String ENTITY_MODIFIED = "entityModified";
    public static final String SELECTION_CHANGED = "selectionChanged";
    public static final String TARGET_FILTER_TAB_CHANGED = "targetFilterTabChanged";
    public static final String FILTER_CHANGED = "filterChanged";
    public static final String PINNING_CHANGED = "pinningChanged";
    public static final String FILE_UPLOAD_CHANGED = "fileUploadChanged";
    public static final String BULK_UPLOAD_CHANGED = "bulkUploadChanged";
    public static final String ENTITY_DRAGGING_CHANGED = "entityDraggingChanged";
    public static final String TENANT_CONFIG_CHANGED = "tenantConfigChanged";

    private EventTopics() {
    }
}
