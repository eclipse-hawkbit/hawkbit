/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.targettype;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.ui.common.AbstractEntityWindowLayout;
import org.eclipse.hawkbit.ui.common.AbstractUpdateNamedEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.data.mappers.TypeToProxyTypeMapper;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.type.ProxyTypeValidator;
import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * Controller for update distribution set type window
 */
public class UpdateTargetTypeWindowController
        extends AbstractUpdateNamedEntityWindowController<ProxyTargetType, ProxyTargetType, TargetType> {

    private final TargetTypeManagement targetTypeManagement;
    private final TargetManagement targetManagement;
    private final TypeToProxyTypeMapper<DistributionSetType> dsTypeToProxyTypeMapper;
    private final TargetTypeWindowLayout layout;
    private final ProxyTypeValidator validator;

    private String nameBeforeEdit;
    private String keyBeforeEdit;
    private boolean isTargetTypeAssigned;

    /**
     * Constructor for UpdateDsTypeWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetTypeManagement
     *            DistributionSetTypeManagement
     * @param targetManagement
     *            DistributionSetManagement
     * @param layout
     *            DsTypeWindowLayout
     */
    public UpdateTargetTypeWindowController(final CommonUiDependencies uiDependencies,
                                            final TargetTypeManagement targetTypeManagement, final TargetManagement targetManagement,
                                            final TargetTypeWindowLayout layout) {
        super(uiDependencies);

        this.targetTypeManagement = targetTypeManagement;
        this.targetManagement = targetManagement;
        this.dsTypeToProxyTypeMapper = new TypeToProxyTypeMapper<>();
        this.layout = layout;
        this.validator = new ProxyTypeValidator(uiDependencies);
    }

    @Override
    public AbstractEntityWindowLayout<ProxyTargetType> getLayout() {
        return layout;
    }

    @Override
    protected ProxyTargetType buildEntityFromProxy(final ProxyTargetType proxyEntity) {
        final ProxyTargetType dsType = new ProxyTargetType();
        dsType.setName(proxyEntity.getName());
        dsType.setDescription(proxyEntity.getDescription());
        dsType.setColour(proxyEntity.getColour());
        dsType.setSelectedSmTypes(getSmTypesByDsTypeId(proxyEntity.getId()));

        nameBeforeEdit = proxyEntity.getName();

        isTargetTypeAssigned = targetTypeManagement.get(proxyEntity.getId()).get() != null;

        return dsType;
    }

    private Set<ProxyType> getSmTypesByDsTypeId(final Long id) {
        return targetTypeManagement.get(id).get().getCompatibleDistributionSetTypes().stream()
                .map(dsTypeToProxyTypeMapper::map).collect(Collectors.toSet());
    }

    @Override
    protected void adaptLayout(final ProxyTargetType proxyEntity) {
       /* layout.disableTagName();

        if (isTargetTypeAssigned) {
            getUiNotification().displayValidationError(
                    nameBeforeEdit + "  " + getI18n().getMessage("message.error.target.type.update"));
            layout.disableTargetTypeDsSelectLayout();
        }  */
    }

    @Override
    protected TargetType persistEntityInRepository(final ProxyTargetType entity) {
        //TODO: implement
        return null;
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
        //final String trimmedKey = StringUtils.trimWhitespace(entity.getKey());
        //TODO: add the validator, check ds type update controller
        return true;
    }

    private boolean hasNameChanged(final String trimmedName) {
        return !nameBeforeEdit.equals(trimmedName);
    }

    private boolean hasKeyChanged(final String trimmedKey) {
        return !keyBeforeEdit.equals(trimmedKey);
    }
}
