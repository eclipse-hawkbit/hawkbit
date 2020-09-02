/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype.filter;

import java.util.Arrays;
import java.util.List;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.smtype.SmTypeWindowBuilder;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventLayoutViewAware;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.filterlayout.AbstractFilterLayout;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener;
import org.eclipse.hawkbit.ui.common.layout.listener.EntityModifiedListener.EntityModifiedAwareSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.GridActionsVisibilityListener;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedGenericSupport;
import org.eclipse.hawkbit.ui.common.layout.listener.support.EntityModifiedGridRefreshAwareSupport;
import org.eclipse.hawkbit.ui.common.state.TypeFilterLayoutUiState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

import com.vaadin.ui.ComponentContainer;

/**
 * Software module type filter buttons layout.
 */
public class SMTypeFilterLayout extends AbstractFilterLayout {
    private static final long serialVersionUID = 1L;

    private final SMTypeFilterHeader smTypeFilterHeader;
    private final SMTypeFilterButtons smTypeFilterButtons;

    private final transient GridActionsVisibilityListener gridActionsVisibilityListener;
    private final transient EntityModifiedListener<ProxyType> entityModifiedListener;

    /**
     * Constructor
     * 
     * @param i18n
     *            VaadinMessageSource
     * @param permChecker
     *            SpPermissionChecker
     * @param eventBus
     *            UIEventBus
     * @param entityFactory
     *            EntityFactory
     * @param uiNotification
     *            UINotification
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     * @param smTypeFilterLayoutUiState
     *            SMTypeFilterLayoutUiState
     */
    public SMTypeFilterLayout(final VaadinMessageSource i18n, final SpPermissionChecker permChecker,
            final UIEventBus eventBus, final EntityFactory entityFactory, final UINotification uiNotification,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final TypeFilterLayoutUiState smTypeFilterLayoutUiState) {
        final SmTypeWindowBuilder smTypeWindowBuilder = new SmTypeWindowBuilder(i18n, entityFactory, eventBus,
                uiNotification, softwareModuleTypeManagement);

        this.smTypeFilterHeader = new SMTypeFilterHeader(eventBus, i18n, permChecker, smTypeWindowBuilder,
                smTypeFilterLayoutUiState, EventView.UPLOAD);
        this.smTypeFilterButtons = new SMTypeFilterButtons(eventBus, i18n, uiNotification, permChecker,
                softwareModuleTypeManagement, smTypeWindowBuilder, smTypeFilterLayoutUiState, EventView.UPLOAD);

        this.gridActionsVisibilityListener = new GridActionsVisibilityListener(eventBus,
                new EventLayoutViewAware(EventLayout.SM_TYPE_FILTER, EventView.UPLOAD),
                smTypeFilterButtons::hideActionColumns, smTypeFilterButtons::showEditColumn,
                smTypeFilterButtons::showDeleteColumn);
        this.entityModifiedListener = new EntityModifiedListener.Builder<>(eventBus, ProxyType.class)
                .entityModifiedAwareSupports(getEntityModifiedAwareSupports())
                .parentEntityType(ProxySoftwareModule.class).build();

        buildLayout();
    }

    private List<EntityModifiedAwareSupport> getEntityModifiedAwareSupports() {
        return Arrays.asList(EntityModifiedGridRefreshAwareSupport.of(smTypeFilterButtons::refreshAll),
                EntityModifiedGenericSupport.of(null, null, smTypeFilterButtons::resetFilterOnTypesDeleted));
    }

    @Override
    protected SMTypeFilterHeader getFilterHeader() {
        return smTypeFilterHeader;
    }

    @Override
    protected ComponentContainer getFilterContent() {
        return wrapFilterContent(smTypeFilterButtons);
    }

    /**
     * Is called when view is shown to the user
     */
    public void restoreState() {
        smTypeFilterButtons.restoreState();
    }

    /**
     * Unsubscribe the events listeners
     */
    public void unsubscribeListener() {
        gridActionsVisibilityListener.unsubscribe();
        entityModifiedListener.unsubscribe();
    }
}
