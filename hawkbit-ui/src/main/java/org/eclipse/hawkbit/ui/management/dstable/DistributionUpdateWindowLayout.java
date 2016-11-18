/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstable;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.distributions.dstable.AbstractDistributionSetUpdateWindowLayout;
import org.eclipse.hawkbit.ui.utils.I18N;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.spring.events.EventBus.SessionEventBus;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;

/**
 * WindowContent for adding/editing a Distribution
 */
@SpringComponent
@ViewScope
public class DistributionUpdateWindowLayout extends AbstractDistributionSetUpdateWindowLayout {

    private static final long serialVersionUID = 1L;

    /**
     * Save or update distribution set.
     *
     */
    private final class UpdateSaveOnCloseDialogListener implements SaveDialogCloseListener {
        @Override
        public void saveOrUpdate() {
            if (editDistribution) {
                updateDistribution();
                return;
            }
        }

        @Override
        public boolean canWindowSaveOrUpdate() {
            return !isDuplicate();
        }

    }

    /**
     * Constructor.
     * 
     * @param i18n
     *            the i18n
     * @param notificationMessage
     *            the notification message
     * @param eventBus
     *            the event bus
     * @param distributionSetManagement
     *            the distributionSetManagement
     * @param systemManagement
     *            the systemManagement
     * @param entityFactory
     *            the entityFactory
     */
    @Autowired
    public DistributionUpdateWindowLayout(final I18N i18n, final UINotification notificationMessage,
            final SessionEventBus eventBus, final DistributionSetManagement distributionSetManagement,
            final SystemManagement systemManagement, final EntityFactory entityFactory) {
        super(i18n, notificationMessage, eventBus, distributionSetManagement, systemManagement, entityFactory);
    }

    @Override
    protected SaveDialogCloseListener createSaveOnCloseDialogListener() {
        return new UpdateSaveOnCloseDialogListener();
    }

}
