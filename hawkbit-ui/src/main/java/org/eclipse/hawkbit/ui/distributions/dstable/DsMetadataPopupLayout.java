/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import java.util.List;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.ui.common.AbstractMetadataPopupLayout;
import org.eclipse.hawkbit.ui.distributions.event.MetadataEvent;
import org.eclipse.hawkbit.ui.distributions.event.MetadataEvent.MetadataUIEvent;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 * Pop up layout to display distribution metadata.
 */
@SpringComponent
@ViewScope
public class DsMetadataPopupLayout extends AbstractMetadataPopupLayout<DistributionSet, DistributionSetMetadata> {

    private static final long serialVersionUID = -7778944849012048106L;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    @Autowired
    private transient EntityFactory entityFactory;

    @Autowired
    protected SpPermissionChecker permChecker;

    @Override
    protected void checkForDuplicate(final DistributionSet entity, final String value) {
        distributionSetManagement.findOne(entity, value);
    }

    /**
     * Create metadata for DistributionSet.
     */
    @Override
    protected DistributionSetMetadata createMetadata(final DistributionSet entity, final String key,
            final String value) {
        final DistributionSetMetadata dsMetaData = distributionSetManagement
                .createDistributionSetMetadata(entityFactory.generateDistributionSetMetadata(entity, key, value));
        setSelectedEntity(dsMetaData.getDistributionSet());
        eventBus.publish(this, new MetadataEvent(MetadataUIEvent.CREATE_DISTRIBUTION_SET_METADATA, dsMetaData));
        return dsMetaData;
    }

    /**
     * Update metadata for DistributionSet.
     */
    @Override
    protected DistributionSetMetadata updateMetadata(final DistributionSet entity, final String key,
            final String value) {
        final DistributionSetMetadata dsMetaData = distributionSetManagement
                .updateDistributionSetMetadata(entityFactory.generateDistributionSetMetadata(entity, key, value));
        setSelectedEntity(dsMetaData.getDistributionSet());
        return dsMetaData;
    }

    @Override
    protected List<DistributionSetMetadata> getMetadataList() {
        return getSelectedEntity().getMetadata();
    }

    /**
     * Update metadata for DistributionSet.
     */

    @Override
    protected void deleteMetadata(final DistributionSet entity, final String key, final String value) {
        final DistributionSetMetadata dsMetaData = entityFactory.generateDistributionSetMetadata(entity, key, value);
        distributionSetManagement.deleteDistributionSetMetadata(entity, key);
        eventBus.publish(this, new MetadataEvent(MetadataUIEvent.DELETE_DISTRIBUTION_SET_METADATA, dsMetaData));
    }

    @Override
    protected boolean hasCreatePermission() {
        return permChecker.hasCreateDistributionPermission();
    }

    @Override
    protected boolean hasUpdatePermission() {
        return permChecker.hasUpdateDistributionPermission();
    }
}
