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
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Controller for update target filter
 */
public class UpdateTargetFilterController
        extends AbstractEntityWindowController<ProxyTargetFilterQuery, ProxyTargetFilterQuery> {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateTargetFilterController.class);

    private final TargetFilterQueryManagement targetFilterManagement;
    private final TargetFilterAddUpdateLayout layout;
    private final Runnable closeFormCallback;

    private String nameBeforeEdit;

    /**
     * Constructor for UpdateTargetFilterController
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
    public UpdateTargetFilterController(final UIConfiguration uiConfig,
            final TargetFilterQueryManagement targetFilterManagement, final TargetFilterAddUpdateLayout layout,
            final Runnable closeFormCallback) {
        super(uiConfig);

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
        final ProxyTargetFilterQuery target = new ProxyTargetFilterQuery();

        target.setId(proxyEntity.getId());
        target.setName(proxyEntity.getName());
        target.setQuery(proxyEntity.getQuery());

        nameBeforeEdit = proxyEntity.getName();

        return target;
    }

    @Override
    protected void persistEntity(final ProxyTargetFilterQuery entity) {
        final TargetFilterQueryUpdate targetFilterUpdate = entityFactory.targetFilterQuery().update(entity.getId())
                .name(entity.getName()).query(entity.getQuery());

        try {
            final TargetFilterQuery updatedTargetFilter = targetFilterManagement.update(targetFilterUpdate);

            uiNotification.displaySuccess(i18n.getMessage("message.update.success", updatedTargetFilter.getName()));
            eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                    EntityModifiedEventType.ENTITY_UPDATED, ProxyTargetFilterQuery.class, updatedTargetFilter.getId()));
        } catch (final EntityNotFoundException | EntityReadOnlyException e) {
            LOG.trace("Update of target filter failed in UI: {}", e.getMessage());
            final String entityType = i18n.getMessage("caption.target.filter");
            uiNotification
                    .displayWarning(i18n.getMessage("message.deleted.or.notAllowed", entityType, entity.getName()));
        } finally {
            closeFormCallback.run();
        }
    }

    @Override
    protected boolean isEntityValid(final ProxyTargetFilterQuery entity) {
        if (!StringUtils.hasText(entity.getName())) {
            uiNotification.displayValidationError(i18n.getMessage("message.error.missing.filtername"));
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        if (!nameBeforeEdit.equals(trimmedName) && targetFilterManagement.getByName(trimmedName).isPresent()) {
            uiNotification.displayValidationError(i18n.getMessage("message.target.filter.duplicate", trimmedName));
            return false;
        }

        return true;
    }
}
