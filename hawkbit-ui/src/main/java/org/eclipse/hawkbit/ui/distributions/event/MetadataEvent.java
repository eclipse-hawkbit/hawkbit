/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.event;

import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * 
 * Metadata Events.
 *
 */
public class MetadataEvent {

    public enum MetadataUIEvent {
        DELETE_SOFTWARE_MODULE_METADATA, CREATE_SOFTWARE_MODULE_METADATA;
    }

    private final MetadataUIEvent metadataUIEvent;

    private final MetaData metadata;

    private final SoftwareModule module;

    public MetadataEvent(final MetadataUIEvent metadataUIEvent, final MetaData metadata, final SoftwareModule module) {
        this.metadataUIEvent = metadataUIEvent;
        this.metadata = metadata;
        this.module = module;
    }

    public MetadataUIEvent getMetadataUIEvent() {
        return metadataUIEvent;
    }

    public MetaData getMetaData() {
        return metadata;
    }

    public SoftwareModule getModule() {
        return module;
    }

}
