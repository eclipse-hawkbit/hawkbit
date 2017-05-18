/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import java.util.EnumSet;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent;
import org.eclipse.hawkbit.ui.artifacts.event.SoftwareModuleTypeEvent.SoftwareModuleTypeEnum;
import org.eclipse.hawkbit.ui.artifacts.event.UploadArtifactUIEvent;
import org.eclipse.hawkbit.ui.artifacts.state.ArtifactUploadState;
import org.eclipse.hawkbit.ui.common.SoftwareModuleTypeBeanQuery;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterButtons;
import org.eclipse.hawkbit.ui.dd.criteria.UploadViewClientCriterion;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.vaadin.addons.lazyquerycontainer.BeanQueryFactory;
import org.vaadin.addons.lazyquerycontainer.LazyQueryContainer;
import org.vaadin.spring.events.EventBus.UIEventBus;
import org.vaadin.spring.events.EventScope;
import org.vaadin.spring.events.annotation.EventBusListenerMethod;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;

/**
 * Software module type filter buttons.
 *
 */
public class SMTypeFilterButtons extends AbstractFilterButtons {

    private static final long serialVersionUID = 169198312654380358L;

    private final ArtifactUploadState artifactUploadState;

    private final UploadViewClientCriterion uploadViewClientCriterion;

    SMTypeFilterButtons(final UIEventBus eventBus, final ArtifactUploadState artifactUploadState,
            final UploadViewClientCriterion uploadViewClientCriterion,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        super(eventBus, new SMTypeFilterButtonClick(eventBus, artifactUploadState, softwareModuleTypeManagement));
        this.artifactUploadState = artifactUploadState;
        this.uploadViewClientCriterion = uploadViewClientCriterion;
    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final SoftwareModuleTypeEvent event) {
        if (event.getSoftwareModuleType() != null
                && EnumSet.allOf(SoftwareModuleTypeEnum.class).contains(event.getSoftwareModuleTypeEnum())) {
            refreshTable();
        }

    }

    @EventBusListenerMethod(scope = EventScope.UI)
    void onEvent(final UploadArtifactUIEvent event) {
        if (event == UploadArtifactUIEvent.DELETED_ALL_SOFWARE_TYPE) {
            refreshTable();
        }
    }

    @Override
    protected String getButtonsTableId() {
        return UIComponentIdProvider.SW_MODULE_TYPE_TABLE_ID;
    }

    @Override
    protected LazyQueryContainer createButtonsLazyQueryContainer() {
        return HawkbitCommonUtil.createLazyQueryContainer(new BeanQueryFactory<>(SoftwareModuleTypeBeanQuery.class));
    }

    @Override
    protected boolean isClickedByDefault(final String typeName) {
        return artifactUploadState.getSoftwareModuleFilters().getSoftwareModuleType()
                .map(type -> type.getName().equals(typeName)).orElse(false);
    }

    @Override
    protected String createButtonId(final String name) {
        return UIComponentIdProvider.SM_TYPE_FILTER_BTN_ID + name;
    }

    @Override
    protected DropHandler getFilterButtonDropHandler() {
        return new DropHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public AcceptCriterion getAcceptCriterion() {
                return uploadViewClientCriterion;
            }

            @Override
            public void drop(final DragAndDropEvent event) {
                /* Not required */
            }
        };
    }

    @Override
    protected String getButttonWrapperIdPrefix() {
        return UIComponentIdProvider.UPLOAD_TYPE_BUTTON_PREFIX;
    }

    @Override
    protected String getButtonWrapperData() {
        return null;
    }

}
