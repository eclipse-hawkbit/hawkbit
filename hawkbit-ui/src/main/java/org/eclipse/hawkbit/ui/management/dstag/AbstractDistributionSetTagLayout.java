/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.dstag;

import java.util.Optional;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.layouts.AbstractTagLayout;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Abstract class for tag Layout of Distribution Sets.
 *
 */
public abstract class AbstractDistributionSetTagLayout extends AbstractTagLayout<DistributionSetTag> {

    private static final long serialVersionUID = 1L;

    private final transient DistributionSetTagManagement distributionSetTagManagement;

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
     * @param distributionSetTagManagement
     *            DistributionSetTagManagement
     */
    public AbstractDistributionSetTagLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final DistributionSetTagManagement distributionSetTagManagement) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
        this.distributionSetTagManagement = distributionSetTagManagement;
    }

    @Override
    protected Optional<DistributionSetTag> findEntityByName() {
        return distributionSetTagManagement.getByName(getTagName().getValue());
    }

    public DistributionSetTagManagement getDistributionSetTagManagement() {
        return distributionSetTagManagement;
    }

    @Override
    protected int getTagNameSize() {
        return DistributionSetTag.NAME_MAX_SIZE;
    }

    @Override
    protected int getTagDescSize() {
        return DistributionSetTag.DESCRIPTION_MAX_SIZE;
    }

    @Override
    protected String getTagNameId() {
        return UIComponentIdProvider.NEW_DISTRIBUTION_TAG_NAME;
    }

    @Override
    protected String getTagDescId() {
        return UIComponentIdProvider.NEW_DISTRIBUTION_TAG_DESC;
    }
}
