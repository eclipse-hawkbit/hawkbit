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
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.ui.common.AbstractMetadataPopupLayout;
import org.eclipse.hawkbit.ui.common.DistributionSetIdName;
import org.eclipse.hawkbit.ui.distributions.event.MetadataEvent;
import org.eclipse.hawkbit.ui.distributions.event.MetadataEvent.MetadataUIEvent;
import org.eclipse.hawkbit.ui.distributions.state.ManageDistUIState;
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
    private ManageDistUIState manageDistUIState;
    
    
    @Autowired
    private EntityFactory entityFactory;
    
    @Override
    protected void checkForDuplicate(DistributionSet entity, String value) {
        distributionSetManagement.findOne(entity, value);
    }

    @Override
    protected DistributionSetMetadata createMetadata(DistributionSet entity, String key, String value) {
        DistributionSetMetadata dsMetaData = distributionSetManagement
                .createDistributionSetMetadata(entityFactory.generateDistributionSetMetadata(entity, key, value));
        setSelectedEntity(dsMetaData.getDistributionSet());
        DistributionSetIdName lastselectedDS =  manageDistUIState.getLastSelectedDistribution().isPresent() ? 
                manageDistUIState.getLastSelectedDistribution().get() : null;
        if(lastselectedDS!=null){           
            if((lastselectedDS.getName().concat(lastselectedDS.getVersion())).equals((dsMetaData.getDistributionSet().getName().
                                            concat(dsMetaData.getDistributionSet().getVersion())))){
                eventBus.publish(this, new MetadataEvent(MetadataUIEvent.CREATE_DISTRIBUTIONSET_METADATA,dsMetaData.getKey())); 
            }
        }   
        return dsMetaData;
    }

    @Override
    protected DistributionSetMetadata updateMetadata(DistributionSet entity, String key, String value) {
        DistributionSetMetadata dsMetaData = distributionSetManagement
                .updateDistributionSetMetadata(entityFactory.generateDistributionSetMetadata(entity, key, value));
        setSelectedEntity(dsMetaData.getDistributionSet());
        return dsMetaData;
    }

    @Override
    protected List<DistributionSetMetadata> getMetadataList() {
        return getSelectedEntity().getMetadata();
    }

    @Override
    protected void deleteMetadata(String key) {
        distributionSetManagement.deleteDistributionSetMetadata(getSelectedEntity(), key);
        DistributionSetIdName lastselectedDS =  manageDistUIState.getLastSelectedDistribution().isPresent() ? 
                manageDistUIState.getLastSelectedDistribution().get() : null;
        if(lastselectedDS!=null){       
            DistributionSet distSet = distributionSetManagement.findDistributionSetById(lastselectedDS.getId());
            if((distSet.getName().concat(distSet.getVersion())).equals((getSelectedEntity().getName().
                                            concat(getSelectedEntity().getVersion())))){
                eventBus.publish(this, new MetadataEvent(MetadataUIEvent.DELETE_DISTRIBUTIONSET_METADATA,key)); 
            }    
        }   
    }
}
