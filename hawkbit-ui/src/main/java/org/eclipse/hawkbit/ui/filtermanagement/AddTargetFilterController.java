/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.ui.common.AbstractAddEntityWindowController;
import org.eclipse.hawkbit.ui.common.CommonUiDependencies;
import org.eclipse.hawkbit.ui.common.EntityWindowLayout;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyIdentifiableEntity;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTargetFilterQuery;
import org.springframework.util.StringUtils;

/**
 * Controller for add target filter
 *
 * @author rollouts
 *
 */
public class AddTargetFilterController
        extends AbstractAddEntityWindowController<ProxyTargetFilterQuery, ProxyTargetFilterQuery, TargetFilterQuery> {

    private final TargetFilterQueryManagement targetFilterManagement;
    private final TargetFilterAddUpdateLayout layout;
    private final Runnable closeFormCallback;

    /**
     * Constructor for AddTargetFilterController
     *
     * @param uiDependencies
     *            {@link CommonUiDependencies}
     * @param targetFilterManagement
     *            TargetFilterQueryManagement
     * @param layout
     *            TargetFilterAddUpdateLayout
     * @param closeFormCallback
     *            Runnable
     */
    public AddTargetFilterController(final CommonUiDependencies uiDependencies,
            final TargetFilterQueryManagement targetFilterManagement, final TargetFilterAddUpdateLayout layout,
            final Runnable closeFormCallback) {
        super(uiDependencies);

        this.targetFilterManagement = targetFilterManagement;
        this.layout = layout;
        this.closeFormCallback = closeFormCallback;
    }

    @Override
    public EntityWindowLayout<ProxyTargetFilterQuery> getLayout() {
        return layout;
    }

    @Override
    protected ProxyTargetFilterQuery buildEntityFromProxy(final ProxyTargetFilterQuery proxyEntity) {
        return proxyEntity;
    }

    @Override
    protected TargetFilterQuery persistEntityInRepository(final ProxyTargetFilterQuery entity) {
        return targetFilterManagement.create(
                getEntityFactory().targetFilterQuery().create().name(entity.getName()).query(entity.getQuery()));
    }

    @Override
    protected void postPersist() {
        closeFormCallback.run();
    }

    @Override
    protected String getDisplayableName(final ProxyTargetFilterQuery entity) {
        return entity.getName();
    }

    @Override
    protected String getDisplayableEntityTypeMessageKey() {
        return "caption.target.filter";
    }

    @Override
    protected Long getId(final TargetFilterQuery entity) {
        return entity.getId();
    }

    @Override
    protected Class<? extends ProxyIdentifiableEntity> getEntityClass() {
        return ProxyTargetFilterQuery.class;
    }

    @Override
    protected boolean isEntityValid(final ProxyTargetFilterQuery entity) {
        if (!StringUtils.hasText(entity.getName())) {
            displayValidationError("message.error.missing.filtername");
            return false;
        }

        final String trimmedName = StringUtils.trimWhitespace(entity.getName());
        if (targetFilterManagement.getByName(trimmedName).isPresent()) {
            displayValidationError("message.target.filter.duplicate", trimmedName);
            return false;
        }

        return true;
    }
}
