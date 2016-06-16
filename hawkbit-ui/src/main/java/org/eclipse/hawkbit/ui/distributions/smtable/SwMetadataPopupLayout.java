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

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.AbstractMetadataPopupLayout;
import org.eclipse.hawkbit.ui.distributions.event.MetadataEvent;
import org.eclipse.hawkbit.ui.distributions.event.MetadataEvent.MetadataUIEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
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
    private transient SoftwareManagement softwareManagement;
    
    @Autowired
    private ArtifactUploadState artifactUploadState;
    
    @Autowired
    private EntityFactory entityFactory;
    
    @Autowired
    private ManageDistUIState manageDistUIState;
      
    @Override
    protected void checkForDuplicate(SoftwareModule entity, String value) {
        softwareManagement.findSoftwareModuleMetadata(entity, value);
    }

    @Override
    protected SoftwareModuleMetadata createMetadata(SoftwareModule entity, String key, String value) {
        SoftwareModuleMetadata swMetadata = softwareManagement.createSoftwareModuleMetadata(entityFactory.generateSoftwareModuleMetadata(entity, key, value));
        setSelectedEntity(swMetadata.getSoftwareModule());
      final Long selectedDistSWModuleId =  manageDistUIState.getSelectedBaseSwModuleId().isPresent() ? 
               manageDistUIState.getSelectedBaseSwModuleId().get() : null;
      final SoftwareModule selectedUploadSWModule= artifactUploadState.getSelectedBaseSoftwareModule().isPresent() ?
              artifactUploadState.getSelectedBaseSoftwareModule().get() : null;
      if(selectedDistSWModuleId!=null && selectedDistSWModuleId.equals(swMetadata.getSoftwareModule().getId())){
           eventBus.publish(this, new MetadataEvent(MetadataUIEvent.CREATE_DIST_SOFTWAREMODULE_METADATA,swMetadata.getKey()));
      }else if(selectedUploadSWModule!=null && (selectedUploadSWModule.getName().concat(selectedUploadSWModule.getVersion()).
                                 equals(entity.getName().concat(entity.getVersion())))){
          eventBus.publish(this, new MetadataEvent(MetadataUIEvent.CREATE_UPLOAD_SOFTWAREMODULE_METADATA,swMetadata.getKey()));
      }
        return swMetadata;
    }

    @Override
    protected SoftwareModuleMetadata updateMetadata(SoftwareModule entity, String key, String value) {
        SoftwareModuleMetadata swMetadata = softwareManagement.updateSoftwareModuleMetadata(entityFactory.generateSoftwareModuleMetadata(entity, key, value));
        setSelectedEntity(swMetadata.getSoftwareModule());
        return swMetadata;
    }

    @Override
    protected List<SoftwareModuleMetadata> getMetadataList() {
        return getSelectedEntity().getMetadata();
    }


    @Override
    protected void deleteMetadata(String key) {
        softwareManagement.deleteSoftwareModuleMetadata(getSelectedEntity(), key);
        final Long selectedDistSWModuleId =  manageDistUIState.getSelectedBaseSwModuleId().isPresent() ?
                manageDistUIState.getSelectedBaseSwModuleId().get() : null;
        final SoftwareModule selectedUploadSWModule= artifactUploadState.getSelectedBaseSoftwareModule().isPresent() ?
                        artifactUploadState.getSelectedBaseSoftwareModule().get() : null;          
        if(selectedDistSWModuleId!=null && selectedDistSWModuleId.equals(getSelectedEntity().getId())){        
             eventBus.publish(this, new MetadataEvent(MetadataUIEvent.DELETE_DIST_SOFTWAREMODULE_METADATA,key));
        }else if(selectedUploadSWModule!=null && (selectedUploadSWModule.getName().concat(selectedUploadSWModule.getVersion()).
                equals(getSelectedEntity().getName().concat(getSelectedEntity().getVersion())))){
            eventBus.publish(this, new MetadataEvent(MetadataUIEvent.DELETE_UPLOAD_SOFTWAREMODULE_METADATA,key));
        }
    }

}
