/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag;

import java.util.Optional;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.ui.SpPermissionChecker;
import org.eclipse.hawkbit.ui.layouts.AbstractTagLayout;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Abstract class for tag layout of Targets.
 *
 */
public abstract class AbstractTargetTagLayout extends AbstractTagLayout<TargetTag> {

    private static final long serialVersionUID = 1L;

    private final transient TargetTagManagement targetTagManagement;

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
     * @param targetTagManagement
     *            TargetTagManagement
     */
    public AbstractTargetTagLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final TargetTagManagement targetTagManagement) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification);
        this.targetTagManagement = targetTagManagement;
    }

    @Override
    protected Optional<TargetTag> findEntityByName() {
        return targetTagManagement.getByName(getTagName().getValue());
    }

    public TargetTagManagement getTargetTagManagement() {
        return targetTagManagement;
    }

    @Override
    protected int getTagNameSize() {
        return TargetTag.NAME_MAX_SIZE;
    }

    @Override
    protected int getTagDescSize() {
        return TargetTag.DESCRIPTION_MAX_SIZE;
    }

    @Override
    protected String getTagNameId() {
        return UIComponentIdProvider.NEW_TARGET_TAG_NAME;
    }

    @Override
    protected String getTagDescId() {
        return UIComponentIdProvider.NEW_TARGET_TAG_DESC;
    }

}
