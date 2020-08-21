/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.dstable;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
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
 * Controller for update distribution set window
 */
public class UpdateDsWindowController
        extends AbstractEntityWindowController<ProxyDistributionSet, ProxyDistributionSet> {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateDsWindowController.class);

    private final VaadinMessageSource i18n;
    private final EntityFactory entityFactory;
    private final UIEventBus eventBus;
    private final UINotification uiNotification;

    private final DistributionSetManagement dsManagement;

    private final DsWindowLayout layout;

    /**
     * Constructor for UpdateDsWindowController
     *
     * @param i18n
     *            VaadinMessageSource
     * @param entityFactory
     *            EntityFactory
     * @param eventBus
     *            UIEventBus
     * @param uiNotification
     *            UINotification
     * @param dsManagement
     *            DistributionSetManagement
     * @param layout
     *            DsWindowLayout
     */
    public UpdateDsWindowController(final VaadinMessageSource i18n, final EntityFactory entityFactory,
            final UIEventBus eventBus, final UINotification uiNotification,
            final DistributionSetManagement dsManagement, final DsWindowLayout layout) {
        this.i18n = i18n;
        this.entityFactory = entityFactory;
        this.eventBus = eventBus;
        this.uiNotification = uiNotification;

        this.dsManagement = dsManagement;

        this.layout = layout;
    }

    @Override
    public AbstractEntityWindowLayout<ProxyDistributionSet> getLayout() {
        return layout;
    }

    @Override
    protected ProxyDistributionSet buildEntityFromProxy(final ProxyDistributionSet proxyEntity) {
        final ProxyDistributionSet ds = new ProxyDistributionSet();

        ds.setId(proxyEntity.getId());
        ds.setTypeInfo(proxyEntity.getTypeInfo());
        ds.setName(proxyEntity.getName());
        ds.setVersion(proxyEntity.getVersion());
        ds.setDescription(proxyEntity.getDescription());
        ds.setRequiredMigrationStep(proxyEntity.isRequiredMigrationStep());

        return ds;
    }

    @Override
    protected void adaptLayout(final ProxyDistributionSet proxyEntity) {
        layout.disableDsTypeSelect();
    }

    @Override
    protected void persistEntity(final ProxyDistributionSet entity) {
        final DistributionSetUpdate dsUpdate = entityFactory.distributionSet().update(entity.getId())
                .name(entity.getName()).version(entity.getVersion()).description(entity.getDescription())
                .requiredMigrationStep(entity.isRequiredMigrationStep());

        try {
            final DistributionSet updatedDs = dsManagement.update(dsUpdate);

            uiNotification.displaySuccess(
                    i18n.getMessage("message.update.success", updatedDs.getName() + ":" + updatedDs.getVersion()));
            eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                    EntityModifiedEventType.ENTITY_UPDATED, ProxyDistributionSet.class, updatedDs.getId()));
        } catch (final EntityNotFoundException | EntityReadOnlyException e) {
            LOG.trace("Update of distribution set failed in UI: {}", e.getMessage());
            final String entityType = i18n.getMessage("caption.distribution");
            uiNotification
                    .displayWarning(i18n.getMessage("message.deleted.or.notAllowed", entityType, entity.getName()));
        }
    }

    @Override
    protected boolean isEntityValid(final ProxyDistributionSet entity) {
        if (!StringUtils.hasText(entity.getName()) || !StringUtils.hasText(entity.getVersion())) {
            uiNotification.displayValidationError(i18n.getMessage("message.error.missing.nameorversion"));
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        final String trimmedVersion = StringUtils.trimWhitespace(entity.getVersion());
        if (dsManagement.getByNameAndVersion(trimmedName, trimmedVersion).isPresent()) {
            uiNotification
                    .displayValidationError(i18n.getMessage("message.duplicate.dist", trimmedName, trimmedVersion));
            return false;
        }

        return true;
    }
}
