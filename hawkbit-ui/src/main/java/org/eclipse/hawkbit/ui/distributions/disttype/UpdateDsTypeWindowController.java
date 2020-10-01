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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowController;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.UIConfiguration;
import org.eclipse.hawkbit.ui.common.data.mappers.TypeToProxyTypeMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload;
import org.eclipse.hawkbit.ui.common.event.EntityModifiedEventPayload.EntityModifiedEventType;
import org.eclipse.hawkbit.ui.common.event.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Controller for update distribution set type window
 */
public class UpdateDsTypeWindowController extends AbstractEntityWindowController<ProxyType, ProxyType> {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateDsTypeWindowController.class);

    private final DistributionSetTypeManagement dsTypeManagement;
    private final DistributionSetManagement dsManagement;
    private final TypeToProxyTypeMapper<SoftwareModuleType> smTypeToProxyTypeMapper;
    private final DsTypeWindowLayout layout;

    private String nameBeforeEdit;
    private String keyBeforeEdit;
    private boolean isDsTypeAssigned;

    /**
     * Constructor for UpdateDsTypeWindowController
     *
     * @param uiConfig
     *            {@link UIConfiguration}
     * @param dsTypeManagement
     *            DistributionSetTypeManagement
     * @param dsManagement
     *            DistributionSetManagement
     * @param layout
     *            DsTypeWindowLayout
     */
    public UpdateDsTypeWindowController(final UIConfiguration uiConfig,
            final DistributionSetTypeManagement dsTypeManagement, final DistributionSetManagement dsManagement,
            final DsTypeWindowLayout layout) {
        super(uiConfig);

        this.dsTypeManagement = dsTypeManagement;
        this.dsManagement = dsManagement;
        this.smTypeToProxyTypeMapper = new TypeToProxyTypeMapper<>();
        this.layout = layout;
    }

    @Override
    public AbstractEntityWindowLayout<ProxyType> getLayout() {
        return layout;
    }

    @Override
    protected ProxyType buildEntityFromProxy(final ProxyType proxyEntity) {
        final ProxyType dsType = new ProxyType();

        dsType.setId(proxyEntity.getId());
        dsType.setName(proxyEntity.getName());
        dsType.setDescription(proxyEntity.getDescription());
        dsType.setColour(proxyEntity.getColour());
        dsType.setKey(proxyEntity.getKey());
        dsType.setSelectedSmTypes(getSmTypesByDsTypeId(proxyEntity.getId()));

        nameBeforeEdit = proxyEntity.getName();
        keyBeforeEdit = proxyEntity.getKey();
        isDsTypeAssigned = dsManagement.countByTypeId(proxyEntity.getId()) > 0;

        return dsType;
    }

    private Set<ProxyType> getSmTypesByDsTypeId(final Long id) {
        return dsTypeManagement.get(id).map(dsType -> {
            final Stream<ProxyType> mandatorySmTypeStream = dsType.getMandatoryModuleTypes().stream()
                    .map(mandatorySmType -> {
                        final ProxyType mappedType = smTypeToProxyTypeMapper.map(mandatorySmType);
                        mappedType.setMandatory(true);

                        return mappedType;
                    });

            final Stream<ProxyType> optionalSmTypeStream = dsType.getOptionalModuleTypes().stream()
                    .map(optionalSmType -> {
                        final ProxyType mappedType = smTypeToProxyTypeMapper.map(optionalSmType);
                        mappedType.setMandatory(false);

                        return mappedType;
                    });

            return Stream.concat(mandatorySmTypeStream, optionalSmTypeStream).collect(Collectors.toSet());
        }).orElse(null);
    }

    @Override
    protected void adaptLayout(final ProxyType proxyEntity) {
        layout.disableTagName();
        layout.disableTypeKey();

        if (isDsTypeAssigned) {
            displayValidationError(nameBeforeEdit + "  " + getI18n().getMessage("message.error.dist.set.type.update"));
            layout.disableDsTypeSmSelectLayout();
        }
    }

    @Override
    protected void persistEntity(final ProxyType entity) {
        final DistributionSetTypeUpdate dsTypeUpdate = getEntityFactory().distributionSetType().update(entity.getId())
                .description(entity.getDescription()).colour(entity.getColour());

        final List<Long> mandatorySmTypeIds = entity.getSelectedSmTypes().stream().filter(ProxyType::isMandatory)
                .map(ProxyType::getId).collect(Collectors.toList());

        final List<Long> optionalSmTypeIds = entity.getSelectedSmTypes().stream()
                .filter(selectedSmType -> !selectedSmType.isMandatory()).map(ProxyType::getId)
                .collect(Collectors.toList());

        dsTypeUpdate.mandatory(mandatorySmTypeIds).optional(optionalSmTypeIds);

        try {
            final DistributionSetType updatedDsType = dsTypeManagement.update(dsTypeUpdate);

            displaySuccess("message.update.success", updatedDsType.getName());
            getEventBus().publish(EventTopics.ENTITY_MODIFIED, this,
                    new EntityModifiedEventPayload(EntityModifiedEventType.ENTITY_UPDATED, ProxyDistributionSet.class,
                            ProxyType.class, updatedDsType.getId()));
        } catch (final EntityNotFoundException | EntityReadOnlyException e) {
            LOG.trace("Update of DS type failed in UI: {}", e.getMessage());
            final String entityType = getI18n().getMessage("caption.entity.distribution.type");
            displayWarning("message.deleted.or.notAllowed", entityType, entity.getName());
        }
    }

    @Override
    protected boolean isEntityValid(final ProxyType entity) {
        if (!StringUtils.hasText(entity.getName()) || !StringUtils.hasText(entity.getKey())
                || CollectionUtils.isEmpty(entity.getSelectedSmTypes())) {
            displayValidationError("message.error.missing.typenameorkeyorsmtype");
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        final String trimmedKey = StringUtils.trimWhitespace(entity.getKey());
        if (!nameBeforeEdit.equals(trimmedName) && dsTypeManagement.getByName(trimmedName).isPresent()) {
            displayValidationError("message.type.duplicate.check", trimmedName);
            return false;
        }
        if (!keyBeforeEdit.equals(trimmedKey) && dsTypeManagement.getByKey(trimmedKey).isPresent()) {
            displayValidationError("message.type.key.ds.duplicate.check", trimmedKey);
            return false;
        }

        return true;
    }
}
