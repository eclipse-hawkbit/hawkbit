/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.event;

import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
/**
 * 
 * Metadata Events.
 *
 */
public class MetadataEvent {

    public enum MetadataUIEvent {
        CREATE_DISTRIBUTION_SET_METADATA, DELETE_DISTRIBUTION_SET_METADATA, DELETE_SOFTWARE_MODULE_METADATA, CREATE_SOFTWARE_MODULE_METADATA;
    }

    private MetadataUIEvent metadataUIEvent;

    private DistributionSetMetadata distributionSetMetadata;

    private SoftwareModuleMetadata softwareModuleMetadata;

    public MetadataEvent(MetadataUIEvent metadataUIEvent, final DistributionSetMetadata distributionSetMetadata) {
        this.metadataUIEvent = metadataUIEvent;
        this.distributionSetMetadata = distributionSetMetadata;
    }

    public MetadataEvent(MetadataUIEvent metadataUIEvent, final SoftwareModuleMetadata softwareModuleMetadata) {
        this.metadataUIEvent = metadataUIEvent;
        this.softwareModuleMetadata = softwareModuleMetadata;
    }

    public MetadataUIEvent getMetadataUIEvent() {
        return metadataUIEvent;
    }

    public DistributionSetMetadata getDistributionSetMetadata() {
        return distributionSetMetadata;
    }

    public SoftwareModuleMetadata getSoftwareModuleMetadata() {
        return softwareModuleMetadata;
    }

}
