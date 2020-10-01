/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.smtype;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType.SmTypeAssign;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.springframework.util.StringUtils;

/**
 * Controller for Add software module type window
 */
public class AddSmTypeWindowController extends AbstractEntityWindowController<ProxyType, ProxyType> {

    private final SoftwareModuleTypeManagement smTypeManagement;
    private final SmTypeWindowLayout layout;

    /**
     * Constructor for AddSmTypeWindowController
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param smTypeManagement
     *            SoftwareModuleTypeManagement
     * @param layout
     *            SmTypeWindowLayout
     */
    public AddSmTypeWindowController(final UIConfiguration uiConfig,
            final SoftwareModuleTypeManagement smTypeManagement, final SmTypeWindowLayout layout) {
        super(uiConfig);

        this.smTypeManagement = smTypeManagement;
        this.layout = layout;
    }

    @Override
    public EntityWindowLayout<ProxyType> getLayout() {
        return layout;
    }

    @Override
    protected ProxyType buildEntityFromProxy(final ProxyType proxyEntity) {
        // We ignore the method parameter, because we are interested in the
        // empty object, that we can populate with defaults
        return new ProxyType();
    }

    @Override
    protected void persistEntity(final ProxyType entity) {
        final int assignNumber = entity.getSmTypeAssign() == SmTypeAssign.SINGLE ? 1 : Integer.MAX_VALUE;

        final SoftwareModuleType newSmType = smTypeManagement
                .create(getEntityFactory().softwareModuleType().create().key(entity.getKey()).name(entity.getName())
                        .description(entity.getDescription()).colour(entity.getColour()).maxAssignments(assignNumber));

        getUiNotification().displaySuccess(getI18n().getMessage("message.save.success", newSmType.getName()));
        getEventBus().publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_ADDED, ProxySoftwareModule.class, ProxyType.class, newSmType.getId()));
    }

    @Override
    protected boolean isEntityValid(final ProxyType entity) {
        if (!StringUtils.hasText(entity.getName()) || !StringUtils.hasText(entity.getKey())
                || entity.getSmTypeAssign() == null) {
            getUiNotification().displayValidationError(getI18n().getMessage("message.error.missing.typenameorkeyorsmtype"));
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        final String trimmedKey = StringUtils.trimWhitespace(entity.getKey());
        if (smTypeManagement.getByName(trimmedName).isPresent()) {
            getUiNotification().displayValidationError(getI18n().getMessage("message.type.duplicate.check", trimmedName));
            return false;
        }
        if (smTypeManagement.getByKey(trimmedKey).isPresent()) {
            getUiNotification()
                    .displayValidationError(getI18n().getMessage("message.type.key.swmodule.duplicate.check", trimmedKey));
            return false;
        }

        return true;
    }
}
