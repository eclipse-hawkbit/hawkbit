/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import java.util.Optional;

import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.layouts.AbstractTypeLayout;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * General Layout for Distribution Type pop-up window which is provided on
 * Distribution View when creating, updating, or deleting a Distribution Set
 * Type
 */
public abstract class AbstractDistributionSetTypeLayout extends AbstractTypeLayout<DistributionSetType> {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetTypeManagement distributionSetTypeManagement;

    private DistributionSetTypeSoftwareModuleSelectLayout twinTables;

    private final transient SoftwareModuleTypeManagement softwareModuleTypeManagement;

    /**
     * Constructor
     * 
     * @param i18n
     *            VaadinMessageSource
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param permChecker
     *            SpPermissionChecker
     * @param uiNotification
     *            UINotification
     * @param distributionSetTypeManagement
     *            DistributionSetTypeManagement
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     */
    public AbstractDistributionSetTypeLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final DistributionSetTypeManagement distributionSetTypeManagement,
            final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        createTwinTables();
    }

    private void createTwinTables() {
        twinTables = new DistributionSetTypeSoftwareModuleSelectLayout(getI18n(), softwareModuleTypeManagement);
        getMainLayout().addComponent(twinTables, 2, 0);
        updateRequiredFields();
    }

    private void updateRequiredFields() {
        getWindow().updateAllComponents(getMainLayout());
        getWindow().addComponentListeners();
    }

    @Override
    protected int getTagNameSize() {
        return DistributionSetType.NAME_MAX_SIZE;
    }

    @Override
    protected int getTagDescSize() {
        return DistributionSetType.DESCRIPTION_MAX_SIZE;
    }

    @Override
    protected int getTypeKeySize() {
        return DistributionSetType.KEY_MAX_SIZE;
    }

    @Override
    protected String getTagNameId() {
        return UIComponentIdProvider.NEW_DISTRIBUTION_TYPE_NAME;
    }

    @Override
    protected String getTagDescId() {
        return UIComponentIdProvider.NEW_DISTRIBUTION_TYPE_DESC;
    }

    @Override
    protected String getTypeKeyId() {
        return UIComponentIdProvider.NEW_DISTRIBUTION_TYPE_KEY;
    }

    @Override
    protected void resetFields() {
        super.resetFields();
        twinTables.reset();
    }

    @Override
    protected Optional<DistributionSetType> findEntityByKey() {
        return distributionSetTypeManagement.getByKey(getTypeKey().getValue());
    }

    @Override
    protected String getDuplicateKeyErrorMessage(final DistributionSetType existingType) {
        return getI18n().getMessage("message.type.key.duplicate.check", existingType.getKey());
    }

    @Override
    protected Optional<DistributionSetType> findEntityByName() {
        return distributionSetTypeManagement.getByName(getTagName().getValue());
    }

    public DistributionSetTypeSoftwareModuleSelectLayout getTwinTables() {
        return twinTables;
    }

    public DistributionSetTypeManagement getDistributionSetTypeManagement() {
        return distributionSetTypeManagement;
    }

    public SoftwareModuleTypeManagement getSoftwareModuleTypeManagement() {
        return softwareModuleTypeManagement;
    }

}
