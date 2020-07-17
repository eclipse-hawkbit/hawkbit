/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Controller for add target filter
 *
 * @author rollouts
 *
 */
public class AddTargetFilterController
        extends AbstractEntityWindowController<ProxyTargetFilterQuery, ProxyTargetFilterQuery> {
    private final VaadinMessageSource i18n;
    private final EntityFactory entityFactory;
    private final UIEventBus eventBus;
    private final UINotification uiNotification;

    private final TargetFilterQueryManagement targetFilterManagement;

    private final TargetFilterAddUpdateLayout layout;

    private final Runnable closeFormCallback;

    /**
     * Constructor for AddTargetFilterController
     *
     * @param i18n
     *          VaadinMessageSource
     * @param entityFactory
     *          EntityFactory
     * @param eventBus
     *          UIEventBus
     * @param uiNotification
     *          UINotification
     * @param targetFilterManagement
     *          TargetFilterQueryManagement
     * @param layout
     *          TargetFilterAddUpdateLayout
     * @param closeFormCallback
     *          Runnable
     */
    public AddTargetFilterController(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final UINotification uiNotification,
            final TargetFilterQueryManagement targetFilterManagement, final TargetFilterAddUpdateLayout layout,
            final Runnable closeFormCallback) {
        this.i18n = i18n;
        this.entityFactory = entityFactory;
        this.eventBus = eventBus;
        this.uiNotification = uiNotification;

        this.targetFilterManagement = targetFilterManagement;

        this.layout = layout;

        this.closeFormCallback = closeFormCallback;
    }

    @Override
    public AbstractEntityWindowLayout<ProxyTargetFilterQuery> getLayout() {
        return layout;
    }

    @Override
    protected ProxyTargetFilterQuery buildEntityFromProxy(final ProxyTargetFilterQuery proxyEntity) {
        return proxyEntity;
    }

    @Override
    protected void persistEntity(final ProxyTargetFilterQuery entity) {
        final TargetFilterQuery newTargetFilter = targetFilterManagement
                .create(entityFactory.targetFilterQuery().create().name(entity.getName()).query(entity.getQuery()));

        uiNotification.displaySuccess(i18n.getMessage("message.save.success", newTargetFilter.getName()));
        eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_ADDED, ProxyTargetFilterQuery.class, newTargetFilter.getId()));

        closeFormCallback.run();
    }

    @Override
    protected boolean isEntityValid(final ProxyTargetFilterQuery entity) {
        if (!StringUtils.hasText(entity.getName())) {
            uiNotification.displayValidationError(i18n.getMessage("message.error.missing.filtername"));
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        if (targetFilterManagement.getByName(trimmedName).isPresent()) {
            uiNotification.displayValidationError(i18n.getMessage("message.target.filter.duplicate", trimmedName));
            return false;
        }

        return true;
    }
}
