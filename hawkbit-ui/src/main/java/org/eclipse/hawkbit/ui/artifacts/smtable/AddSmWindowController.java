/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtable;

import javax.validation.ConstraintViolationException;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.mappers.SoftwareModuleToProxyMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.event.CommandTopics;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventLayout;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.eclipse.hawkbit.ui.common.event.EventView;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload;
import org.eclipse.hawkbit.ui.common.event.SelectionChangedEventPayload.SelectionChangedEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * Controller for populating and saving data in Add Software Module Window.
 */
public class AddSmWindowController extends AbstractEntityWindowController<ProxySoftwareModule, ProxySoftwareModule> {
    private static final Logger LOG = LoggerFactory.getLogger(AddSmWindowController.class);

    private final SoftwareModuleManagement smManagement;
    private final SmWindowLayout layout;
    private final EventView view;

    /**
     * Constructor
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param smManagement
     *            SoftwareModuleManagement
     * @param layout
     *            SoftwareModuleWindowLayout
     * @param view
     *            EventView
     */
    public AddSmWindowController(final CommonUiDependencies uiDependencies, final SoftwareModuleManagement smManagement,
            final SmWindowLayout layout, final EventView view) {
        super(uiDependencies);

        this.smManagement = smManagement;
        this.layout = layout;
        this.view = view;
    }

    @Override
    public EntityWindowLayout<ProxySoftwareModule> getLayout() {
        return layout;
    }

    @Override
    protected ProxySoftwareModule buildEntityFromProxy(final ProxySoftwareModule proxyEntity) {
        // We ignore the method parameter, because we are interested in the
        // empty object, that we can populate with defaults
        return new ProxySoftwareModule();
    }

    @Override
    protected void persistEntity(final ProxySoftwareModule entity) {
        final SoftwareModuleCreate smCreate = getEntityFactory().softwareModule().create()
                .type(entity.getTypeInfo().getKey()).name(entity.getName()).version(entity.getVersion())
                .vendor(entity.getVendor()).description(entity.getDescription());

        final SoftwareModule newSoftwareModule;
        try {
            newSoftwareModule = smManagement.create(smCreate);
        } catch (final ConstraintViolationException ex) {
            LOG.trace("Create of software module failed in UI: {}", ex.getMessage());
            displayWarning("message.save.fail", entity.getName() + ":" + entity.getVersion());
            return;
        }

        displaySuccess("message.save.success", newSoftwareModule.getName() + ":" + newSoftwareModule.getVersion());
        getEventBus().publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_ADDED, ProxySoftwareModule.class, newSoftwareModule.getId()));

        final ProxySoftwareModule addedItem = new SoftwareModuleToProxyMapper().map(newSoftwareModule);
        getEventBus().publish(CommandTopics.SELECT_GRID_ENTITY, this, new SelectionChangedEventPayload<>(
                SelectionChangedEventType.ENTITY_SELECTED, addedItem, EventLayout.SM_LIST, view));
    }

    @Override
    protected boolean isEntityValid(final ProxySoftwareModule entity) {
        if (!StringUtils.hasText(entity.getName()) || !StringUtils.hasText(entity.getVersion())
                || entity.getTypeInfo() == null) {
            displayValidationError("message.error.missing.nameorversionortype");
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        final String trimmedVersion = StringUtils.trimWhitespace(entity.getVersion());
        final Long typeId = entity.getTypeInfo().getId();
        if (smManagement.getByNameAndVersionAndType(trimmedName, trimmedVersion, typeId).isPresent()) {
            displayValidationError("message.duplicate.softwaremodule", trimmedName, trimmedVersion);
            return false;
        }

        return true;
    }
}
