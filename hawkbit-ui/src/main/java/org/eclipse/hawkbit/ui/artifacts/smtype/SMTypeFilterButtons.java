/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.event.UploadViewAcceptCriteria;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtons;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIComponentIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 * Software module type filter buttons.
 * 
 */
@SpringComponent
@ViewScope
public class SMTypeFilterButtons extends AbstractFilterButtons {

    private static final long serialVersionUID = 169198312654380358L;

    @Autowired
    private ArtifactUploadState artifactUploadState;

    @Autowired
    private UploadViewAcceptCriteria uploadViewAcceptCriteria;

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final SoftwareModuleTypeEvent event) {
        if (event.getSoftwareModuleTypeEnum() == SoftwareModuleTypeEvent.SoftwareModuleTypeEnum.ADD_SOFTWARE_MODULE_TYPE
                || event.getSoftwareModuleTypeEnum() == SoftwareModuleTypeEvent.SoftwareModuleTypeEnum.UPDATE_SOFTWARE_MODULE_TYPE
                || event.getSoftwareModuleTypeEnum() == SoftwareModuleTypeEvent.SoftwareModuleTypeEnum.DELETE_SOFTWARE_MODULE_TYPE
                        && event.getSoftwareModuleType() != null) {
            refreshTable();
        }
    }

    @EventBusListenerMethod(scope = EventScope.SESSION)
    void onEvent(final UploadArtifactUIEvent event) {
        if (event == UploadArtifactUIEvent.DELETED_ALL_SOFWARE_TYPE) {
            refreshTable();
        }
    }

    @Override
    protected String getButtonsTableId() {
        return SPUIComponentIdProvider.SW_MODULE_TYPE_TABLE_ID;
    }

    @Override
    protected LazyQueryContainer createButtonsLazyQueryContainer() {
        return HawkbitCommonUtil.createLazyQueryContainer(
                new BeanQueryFactory<SoftwareModuleTypeBeanQuery>(SoftwareModuleTypeBeanQuery.class));
    }

    @Override
    protected boolean isClickedByDefault(final String typeName) {
        return artifactUploadState.getSoftwareModuleFilters().getSoftwareModuleType().isPresent() && artifactUploadState
                .getSoftwareModuleFilters().getSoftwareModuleType().get().getName().equals(typeName);
    }

    @Override
    protected String createButtonId(final String name) {
        return SPUIComponentIdProvider.SM_TYPE_FILTER_BTN_ID + name;
    }

    @Override
    protected DropHandler getFilterButtonDropHandler() {
        return new DropHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return uploadViewAcceptCriteria;
            }

            @Override
            public void drop(final DragAndDropEvent event) {
                /* Not required */
            }
        };
    }

    @Override
    protected String getButttonWrapperIdPrefix() {
        return SPUIComponentIdProvider.UPLOAD_TYPE_BUTTON_PREFIX;
    }

    @Override
    protected String getButtonWrapperData() {
        return null;
    }

}
