/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettag.targettype;

import java.util.stream.Collectors;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.ui.common.AbstractAddNamedEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.targettype.ProxyTargetTypeValidator;
import org.springframework.util.StringUtils;

/**
 * Add target type window controller
 */
public class AddTargetTypeWindowController
        extends AbstractAddNamedEntityWindowController<ProxyTargetType, ProxyTargetType, TargetType> {

    private final TargetTypeWindowLayout layout;
    private final ProxyTargetTypeValidator validator;
    private TargetTypeManagement targetTypeManagement;

    /**
     * Constructor for AddTargetTypeWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetTypeManagement
     *            targetTypeManagement
     * @param layout
     *            TargetTypeWindowLayout
     */
    public AddTargetTypeWindowController(final CommonUiDependencies uiDependencies,
                                         final TargetTypeManagement targetTypeManagement, final TargetTypeWindowLayout layout) {
        super(uiDependencies);

        this.targetTypeManagement = targetTypeManagement;
        this.layout = layout;
        this.validator = new ProxyTargetTypeValidator(uiDependencies);
    }

    @Override
    public EntityWindowLayout<ProxyTargetType> getLayout() {
        return layout;
    }

    @Override
    protected ProxyTargetType buildEntityFromProxy(final ProxyTargetType proxyEntity) {
        return new ProxyTargetType();
    }

    @Override
    protected TargetType persistEntityInRepository(final ProxyTargetType entity) {
        TargetType targetType = targetTypeManagement.create(getEntityFactory().targetType().create()
                .name(entity.getName()).description(entity.getDescription()).colour(entity.getColour()));

        if (!entity.getSelectedSmTypes().isEmpty()) {
            targetTypeManagement.assignCompatibleDistributionSetTypes(targetType.getId(),
                    entity.getSelectedSmTypes().stream().map(ProxyType::getId).collect(Collectors.toSet()));
        }
        return targetType;
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
        return validator.isEntityValid(entity, () -> targetTypeManagement.getByName(trimmedName).isPresent());
    }
}
