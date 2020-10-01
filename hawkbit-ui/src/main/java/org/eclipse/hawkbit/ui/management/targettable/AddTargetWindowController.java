/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.mappers.TargetToProxyTargetMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.event.CommandTopics;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload.SelectionChangedEventType;
import org.springframework.util.StringUtils;

/**
 * Controller for add target window
 */
public class AddTargetWindowController extends AbstractEntityWindowController<ProxyTarget, ProxyTarget> {

    private final TargetManagement targetManagement;
    private final TargetWindowLayout layout;
    private final EventView view;

    /**
     * Constructor for AddTargetWindowController
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param targetManagement
     *            TargetManagement
     * @param layout
     *            TargetWindowLayout
     * @param view
     *            EventView
     */
    public AddTargetWindowController(final UIConfiguration uiConfig, final TargetManagement targetManagement,
            final TargetWindowLayout layout, final EventView view) {
        super(uiConfig);

        this.targetManagement = targetManagement;
        this.layout = layout;
        this.view = view;
    }

    @Override
    protected ProxyTarget buildEntityFromProxy(final ProxyTarget proxyEntity) {
        // We ignore the method parameter, because we are interested in the
        // empty object, that we can populate with defaults
        return new ProxyTarget();
    }

    @Override
    public EntityWindowLayout<ProxyTarget> getLayout() {
        return layout;
    }

    @Override
    protected void adaptLayout(final ProxyTarget proxyEntity) {
        layout.setControllerIdEnabled(true);
        layout.setNameRequired(false);
    }

    @Override
    protected void persistEntity(final ProxyTarget entity) {
        final Target newTarget = targetManagement.create(getEntityFactory().target().create()
                .controllerId(entity.getControllerId()).name(entity.getName()).description(entity.getDescription()));

        displaySuccess("message.save.success", newTarget.getName());
        getEventBus().publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_ADDED, ProxyTarget.class, newTarget.getId()));

        final ProxyTarget addedItem = new TargetToProxyTargetMapper(getI18n()).map(newTarget);
        getEventBus().publish(CommandTopics.SELECT_GRID_ENTITY, this, new SelectionChangedEventPayload<>(
                SelectionChangedEventType.ENTITY_SELECTED, addedItem, EventLayout.TARGET_LIST, view));
    }

    @Override
    protected boolean isEntityValid(final ProxyTarget entity) {
        if (!StringUtils.hasText(entity.getControllerId())) {
            displayValidationError("message.error.missing.controllerId");
            return false;
        }

        final String trimmedControllerId = StringUtils.trimWhitespace(entity.getControllerId());
        if (targetManagement.getByControllerID(trimmedControllerId).isPresent()) {
            displayValidationError("message.target.duplicate.check", trimmedControllerId);
            return false;
        }

        return true;
    }
}
