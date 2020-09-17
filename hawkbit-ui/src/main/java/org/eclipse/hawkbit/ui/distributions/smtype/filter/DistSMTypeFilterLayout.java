/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtype.filter;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.artifacts.smtype.filter.SMTypeFilterLayout;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.state.TypeFilterLayoutUiState;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Software Module Type filter layout.
 */
public class DistSMTypeFilterLayout extends SMTypeFilterLayout {
    private static final long serialVersionUID = 1L;

    private final transient SmTypeCssStylesHandler smTypeCssStylesHandler;

    /**
     * Constructor
     *
     * @param eventBus
     *            UIEventBus
     * @param i18n
     *            VaadinMessageSource
     * @param permChecker
     *            SpPermissionChecker
     * @param entityFactory
     *            EntityFactory
     * @param uiNotification
     *            UINotification
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     * @param smTypeFilterLayoutUiState
     *            TypeFilterLayoutUiState
     */
    public DistSMTypeFilterLayout(final UIEventBus eventBus, final VaadinMessageSource i18n,
            final SpPermissionChecker permChecker, final EntityFactory entityFactory,
            final UINotification uiNotification, final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final TypeFilterLayoutUiState smTypeFilterLayoutUiState) {
        super(i18n, permChecker, eventBus, entityFactory, uiNotification, softwareModuleTypeManagement,
                smTypeFilterLayoutUiState, EventView.DISTRIBUTIONS);

        this.smTypeCssStylesHandler = new SmTypeCssStylesHandler(softwareModuleTypeManagement);
        this.smTypeCssStylesHandler.updateSmTypeStyles();

        // buildLayout(); was already called in super(...)
    }

    @Override
    protected void refreshFilterButtons() {
        super.refreshFilterButtons();

        this.smTypeCssStylesHandler.updateSmTypeStyles();
    }
}
