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
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * TODO MR
 * 
 * @author rem1wa3
 *
 */
public abstract class AbstractTargetTagLayout extends AbstractTagLayout<TargetTag> {

    private static final long serialVersionUID = 1L;

    private final transient TargetTagManagement targetTagManagement;

    public AbstractTargetTagLayout(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final SpPermissionChecker permChecker, final UINotification uiNotification,
            final TargetTagManagement targetTagManagement) {
        super(i18n, entityFactory, eventBus, permChecker, uiNotification, TargetTag.NAME_MAX_SIZE,
                TargetTag.DESCRIPTION_MAX_SIZE);
        this.targetTagManagement = targetTagManagement;
    }

    @Override
    protected Optional<TargetTag> findEntityByName() {
        return targetTagManagement.getByName(getTagName().getValue());
    }

    public TargetTagManagement getTargetTagManagement() {
        return targetTagManagement;
    }

}
