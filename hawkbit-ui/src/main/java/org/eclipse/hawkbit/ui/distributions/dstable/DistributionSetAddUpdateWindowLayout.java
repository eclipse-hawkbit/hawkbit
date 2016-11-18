/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.common.CommonDialogWindow.SaveDialogCloseListener;
import org.eclipse.hawkbit.ui.common.table.BaseEntityEventType;
import org.eclipse.hawkbit.ui.management.event.DistributionTableEvent;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
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
public class DistributionSetAddUpdateWindowLayout extends AbstractDistributionSetUpdateWindowLayout {

    private static final long serialVersionUID = -5602182034230568435L;

    @Autowired
    private DistributionSetTable distributionTable;

    /**
     * Save or update distribution set.
     *
     */
    private final class SaveOrUpdateSaveOnCloseDialogListener implements SaveDialogCloseListener {
        @Override
        public void saveOrUpdate() {
            if (editDistribution) {
                updateDistribution();
                return;
            }
            addNewDistribution();
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
    public DistributionSetAddUpdateWindowLayout(final I18N i18n, final UINotification notificationMessage,
            final SessionEventBus eventBus, final DistributionSetManagement distributionSetManagement,
            final SystemManagement systemManagement, final EntityFactory entityFactory) {
        super(i18n, notificationMessage, eventBus, distributionSetManagement, systemManagement, entityFactory);
    }

    @Override
    protected SaveDialogCloseListener createSaveOnCloseDialogListener() {
        return new SaveOrUpdateSaveOnCloseDialogListener();
    }

    /**
     * Add new Distribution set.
     */
    private void addNewDistribution() {
        editDistribution = Boolean.FALSE;

        final String name = HawkbitCommonUtil.trimAndNullIfEmpty(distNameTextField.getValue());
        final String version = HawkbitCommonUtil.trimAndNullIfEmpty(distVersionTextField.getValue());
        final Long distSetTypeId = (Long) distsetTypeNameComboBox.getValue();

        final String desc = HawkbitCommonUtil.trimAndNullIfEmpty(descTextArea.getValue());
        final boolean isMigStepReq = reqMigStepCheckbox.getValue();

        final DistributionSet newDist = distributionSetManagement
                .createDistributionSet(entityFactory.distributionSet().create().name(name).version(version)
                        .description(desc).type(distributionSetManagement.findDistributionSetTypeById(distSetTypeId))
                        .requiredMigrationStep(isMigStepReq));

        eventBus.publish(this, new DistributionTableEvent(BaseEntityEventType.ADD_ENTITY, newDist));

        notificationMessage.displaySuccess(
                i18n.get("message.new.dist.save.success", new Object[] { newDist.getName(), newDist.getVersion() }));
    }

}
