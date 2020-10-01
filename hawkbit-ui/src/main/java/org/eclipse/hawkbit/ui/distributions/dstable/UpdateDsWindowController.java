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
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Controller for update distribution set window
 */
public class UpdateDsWindowController
        extends AbstractEntityWindowController<ProxyDistributionSet, ProxyDistributionSet> {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateDsWindowController.class);

    private final DistributionSetManagement dsManagement;
    private final DsWindowLayout layout;

    /**
     * Constructor for UpdateDsWindowController
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param dsManagement
     *            DistributionSetManagement
     * @param layout
     *            DsWindowLayout
     */
    public UpdateDsWindowController(final UIConfiguration uiConfig, final DistributionSetManagement dsManagement,
            final DsWindowLayout layout) {
        super(uiConfig);

        this.dsManagement = dsManagement;
        this.layout = layout;
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
    public EntityWindowLayout<ProxyDistributionSet> getLayout() {
        return layout;
    }

    @Override
    protected void adaptLayout(final ProxyDistributionSet proxyEntity) {
        layout.disableDsTypeSelect();
    }

    @Override
    protected void persistEntity(final ProxyDistributionSet entity) {
        final DistributionSetUpdate dsUpdate = getEntityFactory().distributionSet().update(entity.getId())
                .name(entity.getName()).version(entity.getVersion()).description(entity.getDescription())
                .requiredMigrationStep(entity.isRequiredMigrationStep());

        try {
            final DistributionSet updatedDs = dsManagement.update(dsUpdate);

            displaySuccess("message.update.success", updatedDs.getName() + ":" + updatedDs.getVersion());
            getEventBus().publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                    EntityModifiedEventType.ENTITY_UPDATED, ProxyDistributionSet.class, updatedDs.getId()));
        } catch (final EntityNotFoundException | EntityReadOnlyException e) {
            LOG.trace("Update of distribution set failed in UI: {}", e.getMessage());
            final String entityType = getI18n().getMessage("caption.distribution");
            displayWarning("message.deleted.or.notAllowed", entityType, entity.getName());
        }
    }

    @Override
    protected boolean isEntityValid(final ProxyDistributionSet entity) {
        if (!StringUtils.hasText(entity.getName()) || !StringUtils.hasText(entity.getVersion())) {
            displayValidationError("message.error.missing.nameorversion");
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        final String trimmedVersion = StringUtils.trimWhitespace(entity.getVersion());
        if (dsManagement.getByNameAndVersion(trimmedName, trimmedVersion).isPresent()) {
            displayValidationError("message.duplicate.dist", trimmedName, trimmedVersion);
            return false;
        }

        return true;
    }
}
