/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.smtable.SoftwareModuleGrid;
import org.eclipse.hawkbit.ui.artifacts.smtable.SoftwareModuleGridHeader;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.detailslayout.SoftwareModuleDetails;
import org.eclipse.hawkbit.ui.common.detailslayout.SoftwareModuleDetailsHeader;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventLayoutViewAware;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.EventViewAware;
import org.eclipse.hawkbit.ui.common.layout.MasterEntityAwareComponent;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener.EntityModifiedAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.FilterChangedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.SelectGridEntityListener;
import org.eclipse.hawkbit.ui.common.layout.listener.SelectionChangedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedGridRefreshAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedSelectionAwareSupport;
import org.eclipse.hawkbit.ui.common.softwaremodule.AbstractSoftwareModuleGridLayout;
import org.eclipse.hawkbit.ui.common.state.GridLayoutUiState;
import org.eclipse.hawkbit.ui.common.state.TypeFilterLayoutUiState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Implementation of software module Layout on the Distribution View
 */
public class SwModuleGridLayout extends AbstractSoftwareModuleGridLayout {
    private static final long serialVersionUID = 1L;

    private final SoftwareModuleGridHeader swModuleGridHeader;
    private final SoftwareModuleGrid swModuleGrid;
    private final SoftwareModuleDetailsHeader softwareModuleDetailsHeader;
    private final SoftwareModuleDetails swModuleDetails;

    /**
     * Constructor for SwModuleGridLayout
     *
     * @param i18n
     *            VaadinMessageSource
     * @param uiNotification
     *            UINotification
     * @param eventBus
     *            UIEventBus
     * @param softwareModuleManagement
     *            SoftwareModuleManagement
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     * @param entityFactory
     *            EntityFactory
     * @param permChecker
     *            SpPermissionChecker
     * @param artifactManagement
     *            ArtifactManagement
     * @param smTypeFilterLayoutUiState
     *            TypeFilterLayoutUiState
     * @param swModuleGridLayoutUiState
     *            GridLayoutUiState
     */
    public SwModuleGridLayout(final VaadinMessageSource i18n, final UINotification uiNotification,
            final UIEventBus eventBus, final SoftwareModuleManagement softwareModuleManagement,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement, final EntityFactory entityFactory,
            final SpPermissionChecker permChecker, final ArtifactManagement artifactManagement,
            final TypeFilterLayoutUiState smTypeFilterLayoutUiState,
            final GridLayoutUiState swModuleGridLayoutUiState) {
        super(i18n, entityFactory, eventBus, uiNotification, softwareModuleManagement, softwareModuleTypeManagement,
                permChecker, EventView.DISTRIBUTIONS);

        this.swModuleGridHeader = new SoftwareModuleGridHeader(i18n, permChecker, eventBus, smTypeFilterLayoutUiState,
                swModuleGridLayoutUiState, getSmWindowBuilder(), getEventView());
        this.swModuleGridHeader.buildHeader();
        this.swModuleGrid = new SoftwareModuleGrid(eventBus, i18n, permChecker, uiNotification,
                smTypeFilterLayoutUiState, swModuleGridLayoutUiState, softwareModuleManagement, getEventView());
        this.swModuleGrid.addDragAndDropSupport();
        this.swModuleGrid.addMasterSupport();
        this.swModuleGrid.init();

        this.softwareModuleDetailsHeader = new SoftwareModuleDetailsHeader(i18n, permChecker, eventBus, uiNotification,
                getSmWindowBuilder(), getSmMetaDataWindowBuilder());
        this.softwareModuleDetailsHeader.addArtifactDetailsHeaderSupport(artifactManagement);
        this.softwareModuleDetailsHeader.buildHeader();
        this.swModuleDetails = new SoftwareModuleDetails(i18n, eventBus, softwareModuleManagement,
                softwareModuleTypeManagement, getSmMetaDataWindowBuilder());
        this.swModuleDetails.buildDetails();

        addEventListener(new FilterChangedListener<>(eventBus, ProxySoftwareModule.class,
                new EventViewAware(getEventView()), swModuleGrid.getFilterSupport()));
        addEventListener(new SelectionChangedListener<>(eventBus,
                new EventLayoutViewAware(EventLayout.DS_LIST, getEventView()), getMasterDsAwareComponents()));
        addEventListener(new SelectionChangedListener<>(eventBus,
                new EventLayoutViewAware(EventLayout.SM_LIST, getEventView()), getMasterSmAwareComponents()));
        addEventListener(new SelectGridEntityListener<>(eventBus,
                new EventLayoutViewAware(EventLayout.SM_LIST, getEventView()), swModuleGrid.getSelectionSupport()));
        addEventListener(new EntityModifiedListener.Builder<>(eventBus, ProxySoftwareModule.class)
                .entityModifiedAwareSupports(getSmModifiedAwareSupports()).build());

        buildLayout(swModuleGridHeader, swModuleGrid, softwareModuleDetailsHeader, swModuleDetails);
    }

    private List<MasterEntityAwareComponent<ProxyDistributionSet>> getMasterDsAwareComponents() {
        return Collections.singletonList(swModuleGrid.getMasterEntitySupport());
    }

    private List<MasterEntityAwareComponent<ProxySoftwareModule>> getMasterSmAwareComponents() {
        return Arrays.asList(softwareModuleDetailsHeader, swModuleDetails);
    }

    private List<EntityModifiedAwareSupport> getSmModifiedAwareSupports() {
        return Arrays.asList(EntityModifiedGridRefreshAwareSupport.of(swModuleGrid::refreshAll),
                EntityModifiedSelectionAwareSupport.of(swModuleGrid.getSelectionSupport(),
                        swModuleGrid::mapIdToProxyEntity));
    }

    @Override
    protected SoftwareModuleGridHeader getSoftwareModuleGridHeader() {
        return swModuleGridHeader;
    }

    @Override
    protected SoftwareModuleGrid getSoftwareModuleGrid() {
        return swModuleGrid;
    }
}
