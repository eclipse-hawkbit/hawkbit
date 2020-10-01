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
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.mappers.DistributionSetToProxyDistributionMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;
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
 * Controller for add distribution set window
 */
public class AddDsWindowController extends AbstractEntityWindowController<ProxyDistributionSet, ProxyDistributionSet> {

    private final SystemManagement systemManagement;
    private final DistributionSetManagement dsManagement;
    private final DsWindowLayout layout;
    private final EventView view;

    /**
     * Constructor for AddDsWindowController
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param systemManagement
     *            SystemManagement
     * @param dsManagement
     *            DistributionSetManagement
     * @param layout
     *            DsWindowLayout
     * @param view
     *            EventView
     */
    public AddDsWindowController(final UIConfiguration uiConfig, final SystemManagement systemManagement,
            final DistributionSetManagement dsManagement, final DsWindowLayout layout, final EventView view) {
        super(uiConfig);

        this.systemManagement = systemManagement;
        this.dsManagement = dsManagement;
        this.layout = layout;
        this.view = view;
    }

    @Override
    public EntityWindowLayout<ProxyDistributionSet> getLayout() {
        return layout;
    }

    @Override
    protected ProxyDistributionSet buildEntityFromProxy(final ProxyDistributionSet proxyEntity) {
        // We ignore the method parameter, because we are interested in the
        // empty object, that we can populate with defaults
        final ProxyDistributionSet newDs = new ProxyDistributionSet();

        final ProxyTypeInfo newType = new ProxyTypeInfo();
        newType.setId(systemManagement.getTenantMetadata().getDefaultDsType().getId());
        newDs.setTypeInfo(newType);

        return newDs;
    }

    @Override
    protected void persistEntity(final ProxyDistributionSet entity) {
        final DistributionSet newDs = dsManagement.create(entityFactory.distributionSet().create()
                .type(entity.getTypeInfo().getKey()).name(entity.getName()).version(entity.getVersion())
                .description(entity.getDescription()).requiredMigrationStep(entity.isRequiredMigrationStep()));

        uiNotification
                .displaySuccess(i18n.getMessage("message.save.success", newDs.getName() + ":" + newDs.getVersion()));
        eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_ADDED, ProxyDistributionSet.class, newDs.getId()));

        final ProxyDistributionSet addedItem = new DistributionSetToProxyDistributionMapper().map(newDs);
        eventBus.publish(CommandTopics.SELECT_GRID_ENTITY, this, new SelectionChangedEventPayload<>(
                SelectionChangedEventType.ENTITY_SELECTED, addedItem, EventLayout.DS_LIST, view));
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
