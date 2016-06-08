/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import java.util.List;

import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SwMetadataCompositeKey;
import org.eclipse.hawkbit.ui.common.AbstractMetadataPopupLayout;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 * Pop up layout to display software module metadata.
 *
 */
@SpringComponent
@ViewScope
public class SwMetadataPopupLayout extends AbstractMetadataPopupLayout<SoftwareModule, SoftwareModuleMetadata> {

    private static final long serialVersionUID = -1252090014161012563L;

    @Autowired
    private SoftwareManagement softwareManagement;

    @Override
    protected void checkForDuplicate(SoftwareModule entity, String value) {
        softwareManagement.findSoftwareModuleMetadata(new SwMetadataCompositeKey(entity, value));
    }

    @Override
    protected SoftwareModuleMetadata createMetadata(SoftwareModule entity, String key, String value) {
        SoftwareModuleMetadata swMetadata = softwareManagement.createSoftwareModuleMetadata(new SoftwareModuleMetadata(
                key, entity, value));
        setSelectedEntity(swMetadata.getSoftwareModule());
        return swMetadata;
    }

    @Override
    protected SoftwareModuleMetadata updateMetadata(SoftwareModule entity, String key, String value) {
        SoftwareModuleMetadata swMetadata = softwareManagement.updateSoftwareModuleMetadata(new SoftwareModuleMetadata(
                key, entity, value));
        setSelectedEntity(swMetadata.getSoftwareModule());
        return swMetadata;
    }

    @Override
    protected List<SoftwareModuleMetadata> getMetadataList() {
        return getSelectedEntity().getMetadata();
    }

    @Override
    protected Object getMetaDataCompositeKey(SoftwareModuleMetadata metaData) {
        return metaData.getId();
    }

    @Override
    protected void deleteMetadata(String key) {
        softwareManagement.deleteSoftwareModuleMetadata(new SwMetadataCompositeKey(getSelectedEntity(), key));
    }

}
