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
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DsMetadataCompositeKey;
import org.eclipse.hawkbit.ui.common.AbstractMetadataPopupLayout;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.renderers.ClickableRenderer.RendererClickEvent;

/**
 * Pop up layout to display distribution metadata.
 */
@SpringComponent
@ViewScope
public class DsMetadataPopupLayout extends AbstractMetadataPopupLayout<DistributionSet, DistributionSetMetadata> {

    private static final long serialVersionUID = -7778944849012048106L;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @Override
    protected void checkForDuplicate(DistributionSet entity, String value) {
        distributionSetManagement.findOne(new DsMetadataCompositeKey(entity, value));
    }

    @Override
    protected DistributionSetMetadata createMetadata(DistributionSet entity, String key, String value) {
        DistributionSetMetadata dsMetaData = distributionSetManagement
                .createDistributionSetMetadata(new DistributionSetMetadata(key, entity, value));
        setSelectedEntity(dsMetaData.getDistributionSet());
        return dsMetaData;
    }

    @Override
    protected DistributionSetMetadata updateMetadata(DistributionSet entity, String key, String value) {
        DistributionSetMetadata dsMetaData = distributionSetManagement
                .updateDistributionSetMetadata(new DistributionSetMetadata(key, entity, value));
        setSelectedEntity(dsMetaData.getDistributionSet());
        return dsMetaData;
    }

    @Override
    protected List<DistributionSetMetadata> getMetadataList() {
        return distributionSetManagement.findDistributionSetMetadataByDistributionSetId(getSelectedEntity().getId());
    }

    @Override
    protected Object getMetaDataCompositeKey(DistributionSetMetadata metaData) {
        return metaData.getId();
    }

    @Override
    protected void deleteMetadata(String key) {
        distributionSetManagement.deleteDistributionSetMetadata(new DsMetadataCompositeKey(getSelectedEntity(), key));
    }
}
