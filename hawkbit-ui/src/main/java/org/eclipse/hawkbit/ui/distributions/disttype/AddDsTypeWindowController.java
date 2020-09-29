/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.disttype;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Add distribution set type window controller
 */
public class AddDsTypeWindowController extends AbstractEntityWindowController<ProxyType, ProxyType> {

    private final DistributionSetTypeManagement dsTypeManagement;
    private final DsTypeWindowLayout layout;

    /**
     * Constructor for AddDsTypeWindowController
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param dsTypeManagement
     *            DistributionSetTypeManagement
     * @param layout
     *            DsTypeWindowLayout
     */
    public AddDsTypeWindowController(final UIConfiguration uiConfig,
            final DistributionSetTypeManagement dsTypeManagement, final DsTypeWindowLayout layout) {
        super(uiConfig);

        this.dsTypeManagement = dsTypeManagement;
        this.layout = layout;
    }

    @Override
    public AbstractEntityWindowLayout<ProxyType> getLayout() {
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
        final List<Long> mandatorySmTypeIds = entity.getSelectedSmTypes().stream().filter(ProxyType::isMandatory)
                .map(ProxyType::getId).collect(Collectors.toList());

        final List<Long> optionalSmTypeIds = entity.getSelectedSmTypes().stream()
                .filter(selectedSmType -> !selectedSmType.isMandatory()).map(ProxyType::getId)
                .collect(Collectors.toList());

        final DistributionSetType newDsType = dsTypeManagement.create(entityFactory.distributionSetType().create()
                .key(entity.getKey()).name(entity.getName()).description(entity.getDescription())
                .colour(entity.getColour()).mandatory(mandatorySmTypeIds).optional(optionalSmTypeIds));

        uiNotification.displaySuccess(i18n.getMessage("message.save.success", newDsType.getName()));
        eventBus.publish(EventTopics.ENTITY_MODIFIED, this, new EntityModifiedEventPayload(
                EntityModifiedEventType.ENTITY_ADDED, ProxyDistributionSet.class, ProxyType.class, newDsType.getId()));
    }

    @Override
    protected boolean isEntityValid(final ProxyType entity) {
        if (!StringUtils.hasText(entity.getName()) || !StringUtils.hasText(entity.getKey())
                || CollectionUtils.isEmpty(entity.getSelectedSmTypes())) {
            uiNotification.displayValidationError(i18n.getMessage("message.error.missing.typenameorkeyorsmtype"));
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        final String trimmedKey = StringUtils.trimWhitespace(entity.getKey());
        if (dsTypeManagement.getByName(trimmedName).isPresent()) {
            uiNotification.displayValidationError(i18n.getMessage("message.type.duplicate.check", trimmedName));
            return false;
        }
        if (dsTypeManagement.getByKey(trimmedKey).isPresent()) {
            uiNotification.displayValidationError(i18n.getMessage("message.type.key.ds.duplicate.check", trimmedKey));
            return false;
        }

        return true;
    }
}
