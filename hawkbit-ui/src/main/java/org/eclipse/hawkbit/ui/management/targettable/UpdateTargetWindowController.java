/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.targettable;

import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.builder.TargetUpdate;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.common.AbstractUpdateNamedEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTarget;
import org.springframework.util.StringUtils;

/**
 * Controller for update target window
 */
public class UpdateTargetWindowController
        extends AbstractUpdateNamedEntityWindowController<ProxyTarget, ProxyTarget, Target> {

    private final TargetManagement targetManagement;
    private final TargetWindowLayout layout;

    private String controllerIdBeforeEdit;
    private final ProxyTargetValidator proxyTargetValidator;

    /**
     * Constructor for UpdateTargetWindowController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetManagement
     *            TargetManagement
     * @param layout
     *            TargetWindowLayout
     */
    public UpdateTargetWindowController(final CommonUiDependencies uiDependencies,
            final TargetManagement targetManagement, final TargetWindowLayout layout) {
        super(uiDependencies);

        this.targetManagement = targetManagement;
        this.layout = layout;
        this.proxyTargetValidator = new ProxyTargetValidator(uiDependencies);
    }

    @Override
    protected ProxyTarget buildEntityFromProxy(final ProxyTarget proxyEntity) {
        final ProxyTarget target = new ProxyTarget();

        target.setId(proxyEntity.getId());
        target.setControllerId(proxyEntity.getControllerId());
        target.setName(proxyEntity.getName());
        target.setDescription(proxyEntity.getDescription());
        target.setTypeInfo(proxyEntity.getTypeInfo());

        controllerIdBeforeEdit = proxyEntity.getControllerId();

        return target;
    }

    @Override
    public EntityWindowLayout<ProxyTarget> getLayout() {
        return layout;
    }

    @Override
    protected void adaptLayout(final ProxyTarget proxyEntity) {
        layout.setControllerIdEnabled(false);
        layout.setNameRequired(true);
    }

    @Override
    protected Target persistEntityInRepository(final ProxyTarget entity) {
        final TargetUpdate targetUpdate = getEntityFactory().target().update(entity.getControllerId())
                .name(entity.getName()).description(entity.getDescription())
                .targetType(entity.getTypeInfo() != null ? entity.getTypeInfo().getId() : null);

        Target updatedTarget = targetManagement.update(targetUpdate);

        // Un-assigning target type needs another DB request to update the target type value to Null
        if (entity.getTypeInfo() == null){
            return targetManagement.unAssignType(entity.getControllerId());
        }

        return updatedTarget;
    }

    @Override
    protected Class<ProxyTarget> getEntityClass() {
        return ProxyTarget.class;
    }

    @Override
    protected boolean isEntityValid(final ProxyTarget entity) {
        final String trimmedControllerId = StringUtils.trimWhitespace(entity.getControllerId());
        return proxyTargetValidator.isEntityValid(entity, () -> hasControllerIdChanged(trimmedControllerId)
                && targetManagement.getByControllerID(trimmedControllerId).isPresent());
    }

    private boolean hasControllerIdChanged(final String trimmedControllerId) {
        return !controllerIdBeforeEdit.equals(trimmedControllerId);
    }
}
