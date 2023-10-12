/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.management.targettag.targettype;

import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.ContextAware;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.ui.common.AbstractAddNamedEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyType;
import org.eclipse.hawkbit.ui.common.targettype.ProxyTargetTypeValidator;

/**
 * Add target type window controller
 */
public class AddTargetTypeWindowController
        extends AbstractAddNamedEntityWindowController<ProxyTargetType, ProxyTargetType, TargetType> {

    private final TargetTypeWindowLayout layout;
    private final ProxyTargetTypeValidator validator;
    private final TargetTypeManagement targetTypeManagement;
    private final ContextAware contextRunner;

    /**
     * Constructor for AddTargetTypeWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetTypeManagement
     *            targetTypeManagement
     * @param layout
     *            TargetTypeWindowLayout
     * @param contextRunner
     *            ContextRunner
     */
    public AddTargetTypeWindowController(final CommonUiDependencies uiDependencies,
            final TargetTypeManagement targetTypeManagement, final TargetTypeWindowLayout layout,
            final ContextAware contextRunner) {
        super(uiDependencies);

        this.targetTypeManagement = targetTypeManagement;
        this.layout = layout;
        this.validator = new ProxyTargetTypeValidator(uiDependencies);
        this.contextRunner = contextRunner;
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
        return targetTypeManagement.create(getEntityFactory().targetType().create().name(entity.getName())
                .description(entity.getDescription()).colour(entity.getColour())
                .compatible(entity.getSelectedDsTypes().stream().map(ProxyType::getId).collect(Collectors.toSet())));
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
        return validator.isEntityValid(entity, () -> {
//            return contextRunner.runInAdminContext(() -> {
                return targetTypeManagement.getByName(entity.getName()).isPresent();
//            });
        });
    }
}
