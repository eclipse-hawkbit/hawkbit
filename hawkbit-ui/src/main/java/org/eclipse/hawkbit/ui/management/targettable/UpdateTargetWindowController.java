/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.builder.TargetUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.utils.UINotification;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.vaadin.spring.events.EventBus.UIEventBus;

/**
 * Controller for update target window
 */
public class UpdateTargetWindowController extends AbstractEntityWindowController<ProxyTarget, ProxyTarget> {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateTargetWindowController.class);

    private final VaadinMessageSource i18n;
    private final EntityFactory entityFactory;
    private final UIEventBus eventBus;
    private final UINotification uiNotification;

    private final TargetManagement targetManagement;

    private final TargetWindowLayout layout;

    private String controllerIdBeforeEdit;

    /**
     * Constructor for UpdateTargetWindowController
     *
     * @param i18n
     *          VaadinMessageSource
     * @param entityFactory
     *          EntityFactory
     * @param eventBus
     *          UIEventBus
     * @param uiNotification
     *          UINotification
     * @param targetManagement
     *          TargetManagement
     * @param layout
     *          TargetWindowLayout
     */
    public UpdateTargetWindowController(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final UINotification uiNotification, final TargetManagement targetManagement,
            final TargetWindowLayout layout) {
        this.i18n = i18n;
        this.entityFactory = entityFactory;
        this.eventBus = eventBus;
        this.uiNotification = uiNotification;

        this.targetManagement = targetManagement;

        this.layout = layout;
    }

    @Override
    public AbstractEntityWindowLayout<ProxyTarget> getLayout() {
        return layout;
    }

    @Override
    protected ProxyTarget buildEntityFromProxy(final ProxyTarget proxyEntity) {
        final ProxyTarget target = new ProxyTarget();

        target.setId(proxyEntity.getId());
        target.setControllerId(proxyEntity.getControllerId());
        target.setName(proxyEntity.getName());
        target.setDescription(proxyEntity.getDescription());

        controllerIdBeforeEdit = proxyEntity.getControllerId();

        return target;
    }

    @Override
    protected void adaptLayout(final ProxyTarget proxyEntity) {
        layout.setControllerIdEnabled(false);
        layout.setNameRequired(true);
    }

    @Override
    protected void persistEntity(final ProxyTarget entity) {
        final TargetUpdate targetUpdate = entityFactory.target().update(entity.getControllerId()).name(entity.getName())
                .description(entity.getDescription());

        try {
            final Target updatedTarget = targetManagement.update(targetUpdate);

            uiNotification.displaySuccess(i18n.getMessage("message.update.success", updatedTarget.getName()));
            eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                    EntityModifiedEventType.ENTITY_UPDATED, ProxyTarget.class, updatedTarget.getId()));
        } catch (final EntityNotFoundException | EntityReadOnlyException e) {
            LOG.trace("Update of target failed in UI: {}", e.getMessage());
            final String entityType = i18n.getMessage("caption.target");
            uiNotification
                    .displayWarning(i18n.getMessage("message.deleted.or.notAllowed", entityType, entity.getName()));
        }
    }

    @Override
    protected boolean isEntityValid(final ProxyTarget entity) {
        if (!StringUtils.hasText(entity.getControllerId())) {
            uiNotification.displayValidationError(i18n.getMessage("message.error.missing.controllerId"));
            return false;
        }

        final String trimmedControllerId = StringUtils.trimWhitespace(entity.getControllerId());
        if (!controllerIdBeforeEdit.equals(trimmedControllerId)
                && targetManagement.getByControllerID(trimmedControllerId).isPresent()) {
            uiNotification
                    .displayValidationError(i18n.getMessage("message.target.duplicate.check", trimmedControllerId));
            return false;
        }

        return true;
    }
}
