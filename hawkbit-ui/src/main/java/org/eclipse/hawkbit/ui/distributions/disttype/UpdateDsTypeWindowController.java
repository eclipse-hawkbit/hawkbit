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
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.AbstractUpdateNamedEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.mappers.TypeToProxyTypeMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.type.ProxyTypeValidator;
import org.springframework.util.StringUtils;

/**
 * Controller for update distribution set type window
 */
public class UpdateDsTypeWindowController
        extends AbstractUpdateNamedEntityWindowController<ProxyType, ProxyType, DistributionSetType> {

    private final DistributionSetTypeManagement dsTypeManagement;
    private final DistributionSetManagement dsManagement;
    private final TypeToProxyTypeMapper<SoftwareModuleType> smTypeToProxyTypeMapper;
    private final DsTypeWindowLayout layout;
    private final ProxyTypeValidator validator;

    private String nameBeforeEdit;
    private String keyBeforeEdit;
    private boolean isDsTypeAssigned;

    /**
     * Constructor for UpdateDsTypeWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param dsTypeManagement
     *            DistributionSetTypeManagement
     * @param dsManagement
     *            DistributionSetManagement
     * @param layout
     *            DsTypeWindowLayout
     */
    public UpdateDsTypeWindowController(final CommonUiDependencies uiDependencies,
            final DistributionSetTypeManagement dsTypeManagement, final DistributionSetManagement dsManagement,
            final DsTypeWindowLayout layout) {
        super(uiDependencies);

        this.dsTypeManagement = dsTypeManagement;
        this.dsManagement = dsManagement;
        this.smTypeToProxyTypeMapper = new TypeToProxyTypeMapper<>();
        this.layout = layout;
        this.validator = new ProxyTypeValidator(uiDependencies);
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
            getUiNotification().displayValidationError(
                    nameBeforeEdit + "  " + getI18n().getMessage("message.error.dist.set.type.update"));
            layout.disableDsTypeSmSelectLayout();
        }
    }

    @Override
    protected DistributionSetType persistEntityInRepository(final ProxyType entity) {
        final DistributionSetTypeUpdate dsTypeUpdate = getEntityFactory().distributionSetType().update(entity.getId())
                .description(entity.getDescription()).colour(entity.getColour());

        final List<Long> mandatorySmTypeIds = entity.getSelectedSmTypes().stream().filter(ProxyType::isMandatory)
                .map(ProxyType::getId).collect(Collectors.toList());

        final List<Long> optionalSmTypeIds = entity.getSelectedSmTypes().stream()
                .filter(selectedSmType -> !selectedSmType.isMandatory()).map(ProxyType::getId)
                .collect(Collectors.toList());

        dsTypeUpdate.mandatory(mandatorySmTypeIds).optional(optionalSmTypeIds);

        return dsTypeManagement.update(dsTypeUpdate);
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getEntityClass() {
        return ProxyType.class;
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getParentEntityClass() {
        return ProxyDistributionSet.class;
    }

    @Override
    protected boolean isEntityValid(final ProxyType entity) {
        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        final String trimmedKey = StringUtils.trimWhitespace(entity.getKey());
        return validator.isDsTypeValid(entity,
                () -> hasKeyChanged(trimmedKey) && dsTypeManagement.getByKey(trimmedKey).isPresent(),
                () -> hasNameChanged(trimmedName) && dsTypeManagement.getByName(trimmedName).isPresent());
    }

    private boolean hasNameChanged(final String trimmedName) {
        return !nameBeforeEdit.equals(trimmedName);
    }

    private boolean hasKeyChanged(final String trimmedKey) {
        return !keyBeforeEdit.equals(trimmedKey);
    }
}
