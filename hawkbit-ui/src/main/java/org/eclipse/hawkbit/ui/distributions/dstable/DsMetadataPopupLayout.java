/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SpPermissionChecker;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.ui.common.AbstractMetadataPopupLayout;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.UIScope;

/**
 * Pop up layout to display distribution metadata.
 */
@SpringComponent
@UIScope
public class DsMetadataPopupLayout extends AbstractMetadataPopupLayout<DistributionSet, MetaData> {

    private static final long serialVersionUID = -7778944849012048106L;

    @Autowired
    private transient DistributionSetManagement distributionSetManagement;

    @Autowired
    private transient EntityFactory entityFactory;

    @Autowired
    protected SpPermissionChecker permChecker;

    @Override
    protected void checkForDuplicate(final DistributionSet entity, final String value) {
        distributionSetManagement.findDistributionSetMetadata(entity.getId(), value);
    }

    /**
     * Create metadata for DistributionSet.
     */
    @Override
    protected DistributionSetMetadata createMetadata(final DistributionSet entity, final String key,
            final String value) {
        final DistributionSetMetadata dsMetaData = distributionSetManagement.createDistributionSetMetadata(
                entity.getId(), Lists.newArrayList(entityFactory.generateMetadata(key, value))).get(0);
        setSelectedEntity(dsMetaData.getDistributionSet());
        return dsMetaData;
    }

    /**
     * Update metadata for DistributionSet.
     */
    @Override
    protected DistributionSetMetadata updateMetadata(final DistributionSet entity, final String key,
            final String value) {
        final DistributionSetMetadata dsMetaData = distributionSetManagement
                .updateDistributionSetMetadata(entity.getId(), entityFactory.generateMetadata(key, value));
        setSelectedEntity(dsMetaData.getDistributionSet());
        return dsMetaData;
    }

    @Override
    protected List<MetaData> getMetadataList() {
        return Collections.unmodifiableList(
                distributionSetManagement.findDistributionSetMetadataByDistributionSetId(getSelectedEntity().getId()));
    }

    /**
     * Update metadata for DistributionSet.
     */

    @Override
    protected void deleteMetadata(final DistributionSet entity, final String key, final String value) {
        distributionSetManagement.deleteDistributionSetMetadata(entity.getId(), key);
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
