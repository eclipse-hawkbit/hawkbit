/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.event;

public class MetadataEvent {
    
    public enum MetadataUIEvent {
        CREATE_MANAGE_DISTRIBUTIONSET_METADATA,DELETE_MANAGE_DISTRIBUTIONSET_METADATA,
        CREATE_DIST_DISTRIBUTIONSET_METADATA,DELETE_DIST_DISTRIBUTIONSET_METADATA,
        CREATE_UPLOAD_SOFTWAREMODULE_METADATA,DELETE_UPLOAD_SOFTWAREMODULE_METADATA,
        CREATE_DIST_SOFTWAREMODULE_METADATA,DELETE_DIST_SOFTWAREMODULE_METADATA;

    }
    
    private String metadataKey;   

    private MetadataUIEvent metadataUIEvent;

    public MetadataEvent(MetadataUIEvent metadataUIEvent, String metadataKey) {
       this.metadataUIEvent = metadataUIEvent;
       this.metadataKey = metadataKey;
    }

    public MetadataUIEvent getMetadataUIEvent() {
           return metadataUIEvent;
       }
    
    public String getMetadataKey() {
        return metadataKey;
    }

}
