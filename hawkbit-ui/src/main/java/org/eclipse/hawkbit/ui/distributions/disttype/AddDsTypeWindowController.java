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
import org.eclipse.hawkbit.ui.common.AbstractAddNamedEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyDistributionSet;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.type.ProxyTypeValidator;
import org.springframework.util.StringUtils;

/**
 * Add distribution set type window controller
 */
public class AddDsTypeWindowController
        extends AbstractAddNamedEntityWindowController<ProxyType, ProxyType, DistributionSetType> {

    private final DistributionSetTypeManagement dsTypeManagement;
    private final DsTypeWindowLayout layout;
    private final ProxyTypeValidator validator;

    /**
     * Constructor for AddDsTypeWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param dsTypeManagement
     *            DistributionSetTypeManagement
     * @param layout
     *            DsTypeWindowLayout
     */
    public AddDsTypeWindowController(final CommonUiDependencies uiDependencies,
            final DistributionSetTypeManagement dsTypeManagement, final DsTypeWindowLayout layout) {
        super(uiDependencies);

        this.dsTypeManagement = dsTypeManagement;
        this.layout = layout;
        this.validator = new ProxyTypeValidator(uiDependencies);
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
    protected DistributionSetType persistEntityInRepository(final ProxyType entity) {
        final List<Long> mandatorySmTypeIds = entity.getSelectedSmTypes().stream().filter(ProxyType::isMandatory)
                .map(ProxyType::getId).collect(Collectors.toList());

        final List<Long> optionalSmTypeIds = entity.getSelectedSmTypes().stream()
                .filter(selectedSmType -> !selectedSmType.isMandatory()).map(ProxyType::getId)
                .collect(Collectors.toList());

        return dsTypeManagement.create(getEntityFactory().distributionSetType().create().key(entity.getKey())
                .name(entity.getName()).description(entity.getDescription()).colour(entity.getColour())
                .mandatory(mandatorySmTypeIds).optional(optionalSmTypeIds));
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
        return validator.isDsTypeValid(entity, () -> dsTypeManagement.getByKey(trimmedKey).isPresent(),
                () -> dsTypeManagement.getByName(trimmedName).isPresent());
    }
}
