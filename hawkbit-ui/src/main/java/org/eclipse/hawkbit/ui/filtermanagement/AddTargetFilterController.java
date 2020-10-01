/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.springframework.util.StringUtils;

/**
 * Controller for add target filter
 *
 * @author rollouts
 *
 */
public class AddTargetFilterController
        extends AbstractEntityWindowController<ProxyTargetFilterQuery, ProxyTargetFilterQuery> {

    private final TargetFilterQueryManagement targetFilterManagement;
    private final TargetFilterAddUpdateLayout layout;
    private final Runnable closeFormCallback;

    /**
     * Constructor for AddTargetFilterController
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param targetFilterManagement
     *            TargetFilterQueryManagement
     * @param layout
     *            TargetFilterAddUpdateLayout
     * @param closeFormCallback
     *            Runnable
     */
    public AddTargetFilterController(final UIConfiguration uiConfig,
            final TargetFilterQueryManagement targetFilterManagement, final TargetFilterAddUpdateLayout layout,
            final Runnable closeFormCallback) {
        super(uiConfig);

        this.targetFilterManagement = targetFilterManagement;
        this.layout = layout;
        this.closeFormCallback = closeFormCallback;
    }

    @Override
    public EntityWindowLayout<ProxyTargetFilterQuery> getLayout() {
        return layout;
    }

    @Override
    protected ProxyTargetFilterQuery buildEntityFromProxy(final ProxyTargetFilterQuery proxyEntity) {
        return proxyEntity;
    }

    @Override
    protected void persistEntity(final ProxyTargetFilterQuery entity) {
        final TargetFilterQuery newTargetFilter = targetFilterManagement.create(
                getEntityFactory().targetFilterQuery().create().name(entity.getName()).query(entity.getQuery()));

        displaySuccess("message.save.success", newTargetFilter.getName());
        getEventBus().publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_ADDED, ProxyTargetFilterQuery.class, newTargetFilter.getId()));

        closeFormCallback.run();
    }

    @Override
    protected boolean isEntityValid(final ProxyTargetFilterQuery entity) {
        if (!StringUtils.hasText(entity.getName())) {
            displayValidationError("message.error.missing.filtername");
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        if (targetFilterManagement.getByName(trimmedName).isPresent()) {
            displayValidationError("message.target.filter.duplicate", trimmedName);
            return false;
        }

        return true;
    }
}
