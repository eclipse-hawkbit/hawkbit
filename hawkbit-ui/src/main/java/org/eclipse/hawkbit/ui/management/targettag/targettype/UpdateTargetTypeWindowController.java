/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.targettype;

import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.AbstractUpdateNamedEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.mappers.TypeToProxyTypeMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.targettype.ProxyTargetTypeValidator;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for update target type window
 */
public class UpdateTargetTypeWindowController
        extends AbstractUpdateNamedEntityWindowController<ProxyTargetType, ProxyTargetType, TargetType> {

    private final TargetTypeManagement targetTypeManagement;
    private final TypeToProxyTypeMapper<DistributionSetType> dsTypeToProxyTypeMapper;
    private final TargetTypeWindowLayout layout;
    private final ProxyTargetTypeValidator validator;

    private String nameBeforeEdit;

    /**
     * Constructor for UpdateTargetTypeWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetTypeManagement
     *            TargetTypeManagement
     * @param layout
     *            TargetTypeWindowLayout
     */
    public UpdateTargetTypeWindowController(final CommonUiDependencies uiDependencies,
                                            final TargetTypeManagement targetTypeManagement,
                                            final TargetTypeWindowLayout layout) {
        super(uiDependencies);

        this.targetTypeManagement = targetTypeManagement;
        this.dsTypeToProxyTypeMapper = new TypeToProxyTypeMapper<>();
        this.layout = layout;
        this.validator = new ProxyTargetTypeValidator(uiDependencies);
    }

    @Override
    public AbstractEntityWindowLayout<ProxyTargetType> getLayout() {
        return layout;
    }

    @Override
    protected ProxyTargetType buildEntityFromProxy(final ProxyTargetType proxyEntity) {
        final ProxyTargetType dsType = new ProxyTargetType();
        dsType.setId(proxyEntity.getId());
        dsType.setName(proxyEntity.getName());
        dsType.setDescription(proxyEntity.getDescription());
        dsType.setColour(proxyEntity.getColour());
        dsType.setSelectedDsTypes(getDsTypesByDsTypeId(proxyEntity.getId()));
        nameBeforeEdit = proxyEntity.getName();

        return dsType;
    }

    private Set<ProxyType> getDsTypesByDsTypeId(final Long id) {
        Optional<TargetType> targetType = targetTypeManagement.get(id);
        return targetType.map(type -> type.getCompatibleDistributionSetTypes().stream()
                .map(dsTypeToProxyTypeMapper::map).collect(Collectors.toSet())).orElse(Collections.emptySet());

    }

    @Override
    protected TargetType persistEntityInRepository(final ProxyTargetType entity) {

        Set<Long> dsTypesIds = getDsTypesByDsTypeId(entity.getId()).stream().map(ProxyType::getId).collect(Collectors.toSet());

        Set<Long> selectedDsIds = entity.getSelectedDsTypes().stream().map(ProxyType::getId).collect(Collectors.toSet());

        Set<Long> dsTypesForRemoval = getDsTypesByDsTypeId(entity.getId()).stream().map(ProxyType::getId)
                .filter(dsType -> !selectedDsIds.contains(dsType)).collect(Collectors.toSet());
        Set<Long> dsTypesForAdd = selectedDsIds.stream()
                .filter(dsType -> !dsTypesIds.contains(dsType)).collect(Collectors.toSet());

        dsTypesForRemoval.forEach(dsType -> targetTypeManagement.unassignDistributionSetType(entity.getId(), dsType));

        if (!dsTypesForAdd.isEmpty()) {
            targetTypeManagement.assignCompatibleDistributionSetTypes(entity.getId(), dsTypesForAdd);
        }

        return targetTypeManagement.update(getEntityFactory().targetType().update(entity.getId())
                .name(entity.getName()).description(entity.getDescription()).colour(entity.getColour()));

    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getEntityClass() {
        return ProxyTargetType.class;
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getParentEntityClass() {
        return ProxyTarget.class;
    }

    @Override
    protected boolean isEntityValid(final ProxyTargetType entity) {
        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        return validator.isEntityValid(entity,
                () -> hasNamedChanged(trimmedName) && targetTypeManagement.getByName(trimmedName).isPresent());
    }

    private boolean hasNamedChanged(final String trimmedName) {
        return !nameBeforeEdit.equals(trimmedName);
    }
}
